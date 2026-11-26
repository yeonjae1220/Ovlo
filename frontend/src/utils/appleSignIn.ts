'use client'

import { getNativePlatform, isNativeApp } from './platform'

/**
 * Sign in with Apple.
 *
 * - 웹: Apple JS SDK 팝업. Services ID(NEXT_PUBLIC_APPLE_CLIENT_ID)로 초기화한다.
 * - iOS 앱: 앱 안의 네이티브 플러그인(AppleSignInPlugin.swift)으로 시스템 시트를 띄운다.
 *   WebView 에서 Apple 웹 로그인을 열면 App-Bound Domains·팝업 제약에 걸리고(nativeOAuth.ts 참고),
 *   Apple 도 앱에서는 네이티브 시트를 기대한다.
 * - Android 앱: 쓰지 않는다. Apple 의 4.8 조항은 iOS 앱에만 해당하고, 웹 흐름을 Custom Tabs 로
 *   돌려받으려면 form_post 콜백 서버가 따로 필요하다.
 *
 * 리플레이 방지: 원본 nonce 를 서버에 보내고 Apple 에는 그 SHA-256 hex 를 보낸다. 서버는 토큰의
 * nonce 클레임이 원본의 해시인지 확인하므로, 가로챈 토큰만으로는 로그인할 수 없다.
 */

const APPLE_CLIENT_ID = process.env.NEXT_PUBLIC_APPLE_CLIENT_ID || null
const APPLE_JS_URL = 'https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js'

export interface AppleCredential {
  identityToken: string
  nonce: string
  name: string | null
}

/** 사용자가 Apple 시트·팝업을 닫았다. 오류로 보여 주지 않는다. */
export class AppleSignInCanceledError extends Error {
  constructor() {
    super('Apple sign-in canceled')
    this.name = 'AppleSignInCanceledError'
  }
}

/** 버튼을 보여도 되는지. 설정 전에는 숨겨서, 눌러야 '설정 안 됨'을 알게 되는 일이 없게 한다. */
export function isAppleSignInAvailable(): boolean {
  if (!APPLE_CLIENT_ID) return false
  return getNativePlatform() !== 'android'
}

interface PreparedNonce {
  nonce: string
  hashedNonce: string
}

// 웹 팝업은 클릭(사용자 활성화) 안에서 바로 열어야 한다. Safari 는 클릭 뒤에 await 를 하나라도 거치면
// 팝업을 막을 수 있어, SDK 로드와 nonce 해시는 버튼이 보일 때 미리 해 둔다. nonce 는 한 번만 쓴다.
let prepared: PreparedNonce | null = null
let preparing: Promise<void> | null = null

/** 버튼이 보일 때 부른다. 웹은 Apple JS 를 받아 두고, 다음 클릭에 쓸 nonce 를 만들어 둔다. */
export function prepareAppleSignIn(): Promise<void> {
  if (!APPLE_CLIENT_ID) return Promise.resolve()
  if (prepared) return Promise.resolve()
  if (!preparing) {
    preparing = (async () => {
      const nonce = randomHex(32)
      const hashedNonce = await sha256Hex(nonce)
      if (!isNativeApp()) await loadAppleJs()
      prepared = { nonce, hashedNonce }
    })().finally(() => {
      preparing = null
    })
  }
  return preparing
}

/**
 * Apple 인증을 시작한다. 클릭 핸들러에서 곧바로(await 없이) 불러야 웹 팝업이 막히지 않는다 —
 * 준비가 끝나 있으면 이 함수는 동기적으로 팝업을 연다.
 */
export function requestAppleCredential(): Promise<AppleCredential> {
  if (!APPLE_CLIENT_ID) return Promise.reject(new Error('Apple sign-in is not configured'))
  const ready = prepared
  prepared = null
  // 다음 시도(취소 뒤 재시도 등)를 위해 새 nonce 를 미리 만든다. SDK 를 못 받은 상태면 여기서 다시
  // 받지 않는다 — 다음 클릭이 다시 받고 그때 오류를 보인다.
  const replenish = () => {
    if (isNativeApp() || window.AppleID) void prepareAppleSignIn().catch(() => {})
  }

  let attempt: Promise<AppleCredential>
  if (ready && !isNativeApp() && window.AppleID) {
    attempt = authorizeWeb(window.AppleID, APPLE_CLIENT_ID, ready)
  } else {
    attempt = (async () => {
      const nonce = ready ?? (await freshNonce())
      if (isNativeApp()) return authorizeNative(nonce)
      return authorizeWeb(await loadAppleJs(), APPLE_CLIENT_ID, nonce)
    })()
  }
  return attempt.finally(replenish)
}

async function freshNonce(): Promise<PreparedNonce> {
  const nonce = randomHex(32)
  return { nonce, hashedNonce: await sha256Hex(nonce) }
}

// ─── iOS 네이티브 ────────────────────────────────────────────────────────────

interface NativeAppleResult {
  identityToken: string
  givenName?: string | null
  familyName?: string | null
}

interface AppleSignInPlugin {
  authorize(options: { nonce: string }): Promise<NativeAppleResult>
}

// registerPlugin 이 돌려주는 Proxy 는 async 함수에서 return 하면 안 된다(thenable 로 읽혀 'then' 을
// 네이티브로 호출한다 — GLOBAL-PIT-136). 모듈 변수에 두고 메서드만 부른다.
let nativePlugin: AppleSignInPlugin | null = null

async function authorizeNative({ nonce, hashedNonce }: PreparedNonce): Promise<AppleCredential> {
  if (!nativePlugin) {
    const { registerPlugin } = await import('@capacitor/core')
    nativePlugin = registerPlugin<AppleSignInPlugin>('AppleSignIn')
  }
  try {
    const result = await nativePlugin.authorize({ nonce: hashedNonce })
    return { identityToken: result.identityToken, nonce, name: joinName(result.givenName, result.familyName) }
  } catch (error) {
    if ((error as { code?: string })?.code === 'CANCELED') throw new AppleSignInCanceledError()
    throw error
  }
}

// ─── 웹 (Apple JS SDK 팝업) ──────────────────────────────────────────────────

interface AppleJsResponse {
  authorization: { id_token: string; state?: string }
  user?: { name?: { firstName?: string; lastName?: string } }
}

declare global {
  interface Window {
    AppleID?: {
      auth: {
        init(config: Record<string, unknown>): void
        signIn(): Promise<AppleJsResponse>
      }
    }
  }
}

const CANCEL_ERRORS = new Set(['popup_closed_by_user', 'user_cancelled_authorize', 'user_trigger_new_signin_flow'])

type AppleJs = NonNullable<Window['AppleID']>

/** init·signIn 은 await 없이 곧바로 부른다(팝업이 클릭 안에서 열리도록). */
function authorizeWeb(appleId: AppleJs, clientId: string, { nonce, hashedNonce }: PreparedNonce): Promise<AppleCredential> {
  const state = randomHex(16)
  appleId.auth.init({
    clientId,
    scope: 'name email',
    // 팝업 모드라 이 주소로 이동하지는 않지만, Services ID 의 Return URLs 에 등록된 값과 같아야 한다.
    redirectURI: `${window.location.origin}/login`,
    state,
    nonce: hashedNonce,
    usePopup: true,
  })
  return appleId.auth.signIn().then(
    (response) => {
      if (response.authorization.state !== state) throw new Error('Apple sign-in state mismatch')
      const name = response.user?.name
      return { identityToken: response.authorization.id_token, nonce, name: joinName(name?.firstName, name?.lastName) }
    },
    (error: unknown) => {
      const code = (error as { error?: string })?.error ?? ''
      if (CANCEL_ERRORS.has(code)) throw new AppleSignInCanceledError()
      throw new Error(`Apple sign-in failed: ${code || 'unknown'}`, { cause: error })
    },
  )
}

let appleJsLoading: Promise<AppleJs> | null = null

function loadAppleJs(): Promise<AppleJs> {
  if (window.AppleID) return Promise.resolve(window.AppleID)
  if (!appleJsLoading) {
    appleJsLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = APPLE_JS_URL
      script.async = true
      script.onload = () => (window.AppleID ? resolve(window.AppleID) : reject(new Error('Apple JS did not load')))
      script.onerror = () => {
        appleJsLoading = null
        script.remove()
        reject(new Error('Apple JS failed to load'))
      }
      document.head.appendChild(script)
    })
  }
  return appleJsLoading
}

// ─── 공통 ────────────────────────────────────────────────────────────────────

function joinName(given?: string | null, family?: string | null): string | null {
  const name = [given, family].map((part) => part?.trim()).filter(Boolean).join(' ')
  return name || null
}

function randomHex(bytes: number): string {
  const buffer = new Uint8Array(bytes)
  crypto.getRandomValues(buffer)
  return Array.from(buffer, (b) => b.toString(16).padStart(2, '0')).join('')
}

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('')
}
