# Ovlo Frontend — 아키텍처

> Vite+React SPA → Next.js 15 App Router 마이그레이션 완료 (2026-05)

## 기술 스택

| 항목 | 내용 |
|------|------|
| 프레임워크 | Next.js 15 (App Router) |
| 언어 | TypeScript |
| 상태관리 | Zustand 5 (persist) |
| 서버 상태 | TanStack Query 5 |
| HTTP | axios (401 → refreshAuth 싱글톤) |
| 실시간 | STOMP over WebSocket (sockjs-client) |
| i18n | 커스텀 I18nProvider (ko/en/ja/zh/es/fr/de) |

## 라우팅 그룹

| 그룹 | 경로 예시 | 특징 |
|------|-----------|------|
| 공개 | `/`, `/login`, `/register`, `/universities` | 인증 불필요 |
| `(main)` | `/exchange-universities`, `/university-reports` | AppLayout, 공개 |
| `(protected)` | `/boards`, `/posts`, `/chat`, `/profile` | 인증 필요, layout에서 redirect |

## 디렉토리 구조

```
frontend/src/
├── app/
│   ├── middleware.ts            # CSP nonce 생성
│   ├── (main)/layout.tsx        # AppLayout wrapper
│   └── (protected)/layout.tsx  # hydration + refreshAuth + redirect
├── views/                       # 페이지 컴포넌트 (app/ 에서 import)
├── store/authStore.ts           # accessToken(메모리) + currentUser(persist)
└── utils/
    ├── refreshAuth.ts          # refresh 싱글톤
    └── oauthState.ts           # OAuth state 쿠키 관리
```

## 마이그레이션 노트 (Vite → Next.js)

| Vite | Next.js |
|------|---------|
| `react-router-dom` | next/navigation |
| `<ProtectedRoute>` | `(protected)/layout.tsx` |
| `<Outlet>` | `{children}` prop |
| `import.meta.env.VITE_*` | `process.env.NEXT_PUBLIC_*` |
| `src/pages/` | `src/views/` (Pages Router 충돌 방지) |

## useSearchParams 사용 페이지 — Suspense 필요

`/posts/new`, `/oauth/callback`, `/university-reports/[id]`
