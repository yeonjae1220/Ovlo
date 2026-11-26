// Sign in with Apple 클라이언트 흐름 — nonce 해시, state 확인, 취소 처리, 플랫폼별 노출 여부.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const { createHash, webcrypto } = require('node:crypto')

const source = ts.transpileModule(fs.readFileSync('src/utils/appleSignIn.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText

const sha256 = (value) => createHash('sha256').update(value).digest('hex')

function load({ clientId = 'com.mungji.ovlo.web', platform = 'web', appleJs, nativeAuthorize } = {}) {
  const head = { appended: [] }
  const window = { location: { origin: 'https://ovlo.test' } }
  const document = {
    createElement: () => ({ remove() {} }),
    head: {
      appendChild(script) {
        head.appended.push(script)
        if (appleJs) window.AppleID = appleJs
        queueMicrotask(() => (appleJs ? script.onload() : script.onerror()))
      },
    },
  }
  const modules = {
    './platform': {
      isNativeApp: () => platform !== 'web',
      getNativePlatform: () => platform,
    },
    '@capacitor/core': {
      registerPlugin: (name) => {
        assert.equal(name, 'AppleSignIn')
        return { authorize: nativeAuthorize }
      },
    },
  }
  const ctx = {
    exports: {},
    require: (id) => {
      if (!(id in modules)) throw new Error(`unexpected require ${id}`)
      return modules[id]
    },
    process: { env: { NEXT_PUBLIC_APPLE_CLIENT_ID: clientId ?? '' } },
    window,
    document,
    crypto: webcrypto,
    TextEncoder,
    Uint8Array,
    Promise,
    queueMicrotask,
  }
  vm.runInNewContext(source, ctx)
  return { api: ctx.exports, head }
}

function appleJsStub(respond) {
  const calls = { config: null }
  return {
    calls,
    auth: {
      init(config) { calls.config = config },
      signIn: () => respond(calls.config),
    },
  }
}

async function main() {
  // 노출 여부: 설정 전에는 어디서도 숨기고, Android 앱에서는 설정돼도 숨긴다.
  assert.equal(load({ clientId: null }).api.isAppleSignInAvailable(), false)
  assert.equal(load({ platform: 'web' }).api.isAppleSignInAvailable(), true)
  assert.equal(load({ platform: 'ios' }).api.isAppleSignInAvailable(), true)
  assert.equal(load({ platform: 'android' }).api.isAppleSignInAvailable(), false)

  // 웹: Apple 에는 원본 nonce 의 SHA-256 hex 를, 서버에는 원본을 준다.
  {
    const appleJs = appleJsStub(async (config) => ({
      authorization: { id_token: 'web-token', code: 'web-code', state: config.state },
      user: { name: { firstName: ' Gildong ', lastName: 'Hong' } },
    }))
    const { api, head } = load({ appleJs })
    const credential = await api.requestAppleCredential()
    const config = appleJs.calls.config
    assert.equal(credential.identityToken, 'web-token')
    assert.match(credential.nonce, /^[0-9a-f]{64}$/)
    assert.equal(config.nonce, sha256(credential.nonce))
    assert.notEqual(config.nonce, credential.nonce)
    assert.equal(config.clientId, 'com.mungji.ovlo.web')
    assert.equal(config.usePopup, true)
    assert.equal(config.scope, 'name email')
    assert.equal(credential.name, 'Gildong Hong')
    // 탈퇴 때 서버가 Apple 토큰으로 바꿔 폐기할 code 와, 그 교환에 필요한 redirect_uri.
    assert.equal(credential.authorizationCode, 'web-code')
    assert.equal(credential.redirectUri, 'https://ovlo.test/login')
    assert.equal(config.redirectURI, credential.redirectUri)
    assert.equal(head.appended.length, 1)
    assert.equal(head.appended[0].src, 'https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js')
  }

  // 웹: 미리 준비했으면 클릭 핸들러 안에서 await 없이 바로 팝업(signIn)을 연다 — Safari 팝업 차단 방지.
  // 준비한 nonce 는 한 번만 쓰고, 다음 시도용으로 새로 만든다.
  {
    let signInCalls = 0
    const appleJs = appleJsStub(async (config) => ({ authorization: { id_token: 'tok', state: config.state } }))
    const originalSignIn = appleJs.auth.signIn
    appleJs.auth.signIn = () => { signInCalls += 1; return originalSignIn() }
    const { api, head } = load({ appleJs })
    await api.prepareAppleSignIn()
    assert.equal(head.appended.length, 1)

    const pending = api.requestAppleCredential()
    assert.equal(signInCalls, 1, 'signIn must run synchronously inside the click')
    const first = await pending
    assert.equal(appleJs.calls.config.nonce, sha256(first.nonce))

    await api.prepareAppleSignIn()
    const pendingSecond = api.requestAppleCredential()
    assert.equal(signInCalls, 2)
    const second = await pendingSecond
    assert.notEqual(second.nonce, first.nonce)
    assert.equal(appleJs.calls.config.nonce, sha256(second.nonce))
    assert.equal(head.appended.length, 1, 'Apple JS is loaded once')
  }

  // 웹: 응답 state 가 보낸 값과 다르면 받지 않는다.
  {
    const appleJs = appleJsStub(async () => ({ authorization: { id_token: 'forged', state: 'other' } }))
    await assert.rejects(load({ appleJs }).api.requestAppleCredential(), /state mismatch/)
  }

  // 웹: 팝업을 닫으면 취소 오류(화면에 실패로 보이지 않는다), 그 밖의 오류는 일반 오류.
  {
    const closed = load({ appleJs: appleJsStub(async () => { throw { error: 'popup_closed_by_user' } }) })
    await assert.rejects(closed.api.requestAppleCredential(), (error) => error.name === 'AppleSignInCanceledError')
    const failed = load({ appleJs: appleJsStub(async () => { throw { error: 'invalid_client' } }) })
    await assert.rejects(failed.api.requestAppleCredential(), (error) =>
      error.name !== 'AppleSignInCanceledError' && /invalid_client/.test(error.message))
  }

  // 웹: Apple JS 를 못 받으면 실패로 끝나고, 다음 시도에서 다시 받는다.
  {
    const { api, head } = load({ appleJs: null })
    await assert.rejects(api.requestAppleCredential(), /failed to load/)
    await assert.rejects(api.requestAppleCredential(), /failed to load/)
    assert.equal(head.appended.length, 2)
  }

  // iOS 앱: 네이티브 플러그인에 해시된 nonce 를 넘기고, 이름은 첫 인증 때만 온다.
  {
    let received
    const { api } = load({
      platform: 'ios',
      nativeAuthorize: async (options) => {
        received = options
        return { identityToken: 'native-token', authorizationCode: 'native-code', givenName: '길동', familyName: null }
      },
    })
    const credential = await api.requestAppleCredential()
    assert.equal(received.nonce, sha256(credential.nonce))
    assert.equal(credential.identityToken, 'native-token')
    assert.equal(credential.name, '길동')
    assert.equal(credential.authorizationCode, 'native-code')
    assert.equal(credential.redirectUri, null)

    const again = load({
      platform: 'ios',
      nativeAuthorize: async () => ({ identityToken: 'native-token', givenName: null, familyName: null }),
    })
    assert.equal((await again.api.requestAppleCredential()).name, null)
  }

  // iOS 앱: 시트를 닫으면(CANCELED) 취소 오류.
  {
    const { api } = load({
      platform: 'ios',
      nativeAuthorize: async () => { throw Object.assign(new Error('canceled'), { code: 'CANCELED' }) },
    })
    await assert.rejects(api.requestAppleCredential(), (error) => error.name === 'AppleSignInCanceledError')
  }

  // 설정이 없으면 시도하지 않는다.
  await assert.rejects(load({ clientId: null }).api.requestAppleCredential(), /not configured/)

  console.log('Apple sign-in tests passed')
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
