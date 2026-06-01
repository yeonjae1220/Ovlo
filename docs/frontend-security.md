# Ovlo Frontend — 보안 아키텍처

> 마지막 업데이트: 2026-05-31

---

## 1. 토큰 저장 전략

| 토큰 | 저장 위치 | persist |
|------|-----------|---------|
| `accessToken` | Zustand 메모리 | ❌ (localStorage 저장 안 함) |
| `currentUser` | Zustand + localStorage | ✅ version 4 |
| `refreshToken` | httpOnly 쿠키 | 서버 관리 |

`authStore.ts` partialize:
```typescript
partialize: (state) => ({
  currentUser: state.currentUser,  // accessToken 제외
}),
```

---

## 2. refresh 싱글톤 (`utils/refreshAuth.ts`)

layout과 axios interceptor가 동시에 refresh를 호출해도 실제 요청은 1회만 실행됩니다.

```typescript
let pending: Promise<string | null> | null = null

export async function refreshAuth(): Promise<string | null> {
  if (pending) return pending  // 진행 중이면 같은 Promise 반환
  pending = axios.post('/api/v1/auth/refresh', ...).then(...).finally(() => pending = null)
  return pending
}
```

**중요**: refresh token rotation 사용 시 이중 소비를 방지하는 핵심 패턴입니다.

---

## 3. 리로드 시 인증 복원 흐름

```
새로고침
  → (protected)/layout.tsx 마운트
  → hydrated = false → AuthSkeleton 표시
  → refreshAuth() 호출 (currentUser는 localStorage에서 즉시 복원)
  → POST /api/v1/auth/refresh (httpOnly 쿠키 자동 전송)
  → setAccessToken(newToken)
  → hydrated = true → 페이지 표시
```

Ovlo는 `currentUser`가 localStorage에 있으므로 refresh 후 별도 `getById` 호출이 필요 없습니다.

---

## 4. OAuth state CSRF 방어

```typescript
// LoginPage, RegisterPage
const state = crypto.randomUUID()
saveOauthState(state)       // SameSite=Lax 쿠키 (5분 만료)
→ Google 인가 서버 → ?state=xxx
→ OAuthCallbackPage
→ consumeOauthState()       // 쿠키 읽기 + 삭제 (one-time)
→ state 검증
```

sessionStorage 대신 쿠키 이유: iOS Safari/인앱 브라우저 OAuth 왕복 시 sessionStorage 유실 가능.

---

## 5. WebSocket 인증 연동

STOMP 연결 시 accessToken을 CONNECT 프레임 헤더로 전송합니다.

```typescript
// 클라이언트 연결 예시
client.connectHeaders = {
  Authorization: `Bearer ${accessToken}`,
}
```

백엔드 `JwtChannelInterceptor`가 CONNECT 프레임의 `Authorization` 헤더를 검증하고, 이후 세션에 memberId를 저장합니다. accessToken 만료 시 STOMP 재연결 전 `refreshAuth()`를 호출해야 합니다.

> 상세 백엔드 보안 구조는 [`docs/backend-security.md`](./backend-security.md) 참조.

---

## 6. CSP (`src/middleware.ts`)

```
script-src 'nonce-{random}' 'strict-dynamic' 'unsafe-inline'
style-src 'self' 'unsafe-inline'
connect-src 'self' https://ovlo.mungji.com wss://ovlo.mungji.com
base-uri 'self'
```

**WebSocket**: STOMP over WebSocket(`wss://ovlo.mungji.com`)은 `connect-src`에 명시.

---

## 6. 보안 헤더

```
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

---

## k8s Health Probe (2026-05-31)

liveness/readiness probe를 분리하여 의존성 장애 시 트래픽 차단.

| 경로 | 목적 | 응답 |
|------|------|------|
| `GET /health/live` | 프로세스 생사 확인 | 항상 200 |
| `GET /health/ready` | 백엔드 연결 확인 | 성공 200, 실패 503 |

`/api/*` rewrites 충돌을 피해 `/health/*` 경로 사용. `force-dynamic`으로 캐싱 방지.
---

## SSR window 가드 패턴 (2026-05-31)

Next.js는 `'use client'` 컴포넌트도 서버에서 초기 HTML 렌더링 시 실행됨.
`useState(fn)` 형태로 초기화 함수를 전달하면 서버에서도 즉시 호출되어 `window is not defined` 에러 발생.

### 문제가 된 코드 (`useBreakpoint.ts`)

```ts
// 서버에서 window.matchMedia() 호출 → ReferenceError
function getBreakpoint() {
  return {
    isMobile: window.matchMedia(MOBILE_QUERY).matches,
    ...
  }
}
export function useBreakpoint() {
  const [bp, setBp] = useState(getBreakpoint) // ← 서버에서 즉시 실행
  ...
}
```

### 수정된 코드

```ts
function getBreakpoint() {
  if (typeof window === 'undefined') {
    return { isMobile: false, isTablet: false, isDesktop: true } // SSR 기본값
  }
  return {
    isMobile: window.matchMedia(MOBILE_QUERY).matches,
    ...
  }
}
```

### 주의사항

- `useEffect` 내부의 `window` 접근은 안전 (클라이언트 전용 실행)
- `useState(fn)` 초기화 함수, 모듈 최상위 레벨 실행은 SSR 시 서버에서 호출됨
- 브라우저 전용 API(`window`, `document`, `navigator`) 는 항상 `typeof` 가드 필요
