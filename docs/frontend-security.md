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

## 5. CSP (`src/middleware.ts`)

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
