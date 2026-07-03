# Changelog

All notable changes to Ovlo are documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

0.x 구간이므로 파괴적 변경도 MINOR 버전으로 흡수합니다.

## [Unreleased]

## [0.5.0] - 2026-06-30

교환학생 **학생 인증 시스템**과 통합 검색, 보안·관측성 강화에 집중한 릴리스.

### Added
- **학생 인증 시스템** — 학교 이메일 검증기(Gmail SMTP) + 프론트 UI + `EXCHANGE_VERIFIED` 상태
- 인증 만료 스케줄러 + 게시판 게이팅 (인증 회원만 특정 게시판 접근) + SMTP egress NetworkPolicy
- 관리자 페이지에서 수동 대학 인증 발급/취소/조회, 학생 인증 페이지 UX 개선(회원검색·대학 자동완성·연결)
- 대학을 `global_universities`로 일원화
- 교환대학 탭 통합 검색 — 리포트 ∪ 후기 카탈로그
- `lab.mungji` 콘솔 집계용 내부 엔드포인트
- JWT `userId` MDC + logstash JSON 구조화 로그, HTTP 요청당 1줄 access log
- 개발자 피드백 수집 엔드포인트 (토큰 검증 + NetworkPolicy)

### Security
- Refresh token을 Redis 저장 전 SHA-256 단방향 해시로 변환 (GLOBAL-PIT-001) — Redis 유출 시 세션 탈취 방지
- 관리자 formLogin 무차별 대입 lockout 추가 (GLOBAL-PIT-038)
- 대학 공개 읽기 API에 IP 기반 search rate limit 및 enumeration 방지 적용

### Changed
- H2 datasource를 `local` 프로파일로 격리 — 공유 문서 datasource 제거 (GLOBAL-PIT-039)

### Fixed
- `EmailSenderPort` 빈 모호성 해결(`@Primary`) — 운영 SMTP 크래시 핫픽스
- frontend-ingress NetworkPolicy 포트 80→3000 (Next.js containerPort 일치, 502 해결)
- auth 라이트 모드 가독성 개선

## [0.4.0] - 2026-06-08

AI 대학 리포트와 **프론트엔드 Next.js 마이그레이션**, 다국어를 도입한 릴리스.

### Added
- **AI 대학 리포트** — 다국어 리포트 API + 페이지, 교환대학 상세에 임베드
- 리포트에 ReactMarkdown 렌더링 + 7개 언어 리포트 스위처
- **프론트엔드 마이그레이션** — Vite + React SPA → Next.js App Router
- 7개 언어 i18n — 자동 언어 감지 + 전 UI 로컬라이즈
- SSR 기반 관리자 패널 구축 및 보안 강화
- 홈 피드/교환대학/채팅 탭 리디자인, 통합 홈 피드 + 프로필 게시글
- NetworkPolicy(default-deny + 컴포넌트별 allowlist), PWA 설치 배너 + manifest
- 다크/라이트 테마 시스템 + CSS 토큰 전환
- k8s liveness/readiness probe 분리 + actuator health 그룹

### Changed
- httpOnly 쿠키 기반 로그인 지속 + 멀티세션 지원 (atomic Redis MULTI/EXEC)
- 관리자 인증을 DB 기반 → InMemory(k8s Secret)로 이전

### Security
- nonce 기반 CSP 완성 — strict-dynamic, base-uri, layout nonce
- Actuator 비-health 엔드포인트를 ADMIN 전용으로, 기본 프로파일에서 metrics/prometheus 노출 제거
- Google OAuth 발급 JWT에서 ADMIN 권한 제외, 관리자 credential 검증 하드닝
- IP 기반 rate limit 헤더 위조 우회 방지 및 회원가입 제한 추가

### Fixed
- Next.js 마이그레이션 정합성 및 App Router hydration CSP 이슈 해결
- SSR 호환을 위한 `useBreakpoint` window 접근 가드
- 채팅 타임존 불일치, sendMessage 최적화, 인가 공백 수정
- PVC `storageClassName` 고정으로 immutable-field 배포 실패 방지

## [0.3.0] - 2026-04-26

글로벌 대학 검색과 관리자 기능, 반응형 UI를 도입한 릴리스.

### Added
- 글로벌 대학 검색 API + 웹 교환 프로그램 테이블
- 관리자 페이지 + 역할 기반 JWT + 보안 하드닝
- 게시판·게시글·대학 관리자 엔드포인트
- 데스크톱/모바일 반응형 레이아웃
- 교환대학 국가(country_code) 필터 + 국가 드롭다운
- 교환대학 후기 수 기준 정렬 + 페이지네이션
- 회원가입 시 비밀번호 조건 실시간 인디케이터
- 로그인 시 사용자 프로필 표시, 인증 사용자에게 CTA 숨김

### Changed
- 탈퇴 회원의 콘텐츠를 모든 조회에서 숨김

### Fixed
- Google OAuth 로그인 리다이렉트 버그 및 회원가입 폼 리마운트 버그
- 대학 검색을 `global_universities` 기준으로 전환, 드롭다운 값 바인딩 수정
- 즉시 SW 업데이트를 위한 `skipWaiting`/`clientsClaim` 적용

## [0.2.0] - 2026-03-26

### Added

#### Auth
- Google OAuth 2.0 로그인 (Authorization Code Flow) — `POST /api/v1/auth/google`
- `OAuthProvider` enum (LOCAL / GOOGLE), `PENDING_ONBOARDING` 회원 상태
- `GoogleLoginUseCase`, `GoogleAuthCommandService`, `GoogleOAuthAdapter`
- `CompleteOnboardingUseCase` — 온보딩 완료 시 ACTIVE 전환 — `PATCH /api/v1/members/me/onboarding`
- Flyway V12: `member` 테이블 `provider`, `provider_id` 컬럼 추가

#### Frontend
- 랜딩 페이지 (다크 테마, 기능 소개)
- Google 로그인 버튼 (LoginPage) — 구분선 + Google 로고
- `OAuthCallbackPage` — 콜백 코드 처리, 로딩 스피너, 에러 표시
- `OnboardingPage` — 신규 OAuth 유저 프로필 완성 (고향·대학·전공)
- 멀티스텝 회원가입 (3단계: 계정 정보 → 개인 정보 → 학업 정보)
- 교환대학 상세 페이지 UX 개선 및 방향 분류(OUTBOUND/INBOUND) 추가
- 교환대학 탭 내비게이션 연결

#### CI/CD
- 프론트엔드 Docker 빌드/푸시 및 k3s 자동 배포
- GHCR Docker Actions Node.js 24 런타임 업그레이드 (v4/v6/v7)

### Changed
- `ProtectedRoute`: `PENDING_ONBOARDING` 상태 유저 → `/onboarding` 강제 리다이렉트

### Fixed
- 로그인 후 랜딩 페이지가 아닌 `/boards`로 리다이렉트
- k3s 이미지 풀 인증 오류 및 프론트엔드 롤아웃 타임아웃
- 프론트엔드 빌드 오류 (미사용 파라미터 TS6133)
- CI 파이프라인 안정성 개선 및 롤백 기능 추가

## [0.1.0] - 2026-03-24

### Added

#### Exchange University
- Exchange university search with keyword and country filter (paginated)
- Per-university video review listing with pagination
- Video review rating aggregation (count, average) via JPA Projection interfaces
- Domain models: `ExchangeUniversity`, `VideoReview` with value objects
- REST endpoint: `GET /api/v1/exchange-universities`

#### Core Domains (TDD + Hexagonal Architecture)
- **member**: 회원 신원·프로필, 닉네임 유일성 검사, 사용자 검색 API
- **auth**: JWT access/refresh token, Redis token store, rate limiting (IP + 계정 dual 체크)
- **university**: 대학 마스터 데이터, 키워드/지역 검색
- **board**: 게시판 생성·범위·구독
- **post**: 게시글·댓글·좋아요/싫어요 (Optimistic Lock 동시성), likedByMe 플래그
- **follow**: 팔로잉/팔로워 관계
- **media**: 파일 업로드, HEIC 변환, 로컬 스토리지 어댑터
- **chat**: DM·그룹채팅 (WebSocket STOMP), 읽음 마커, 메시지 이력 페이지네이션

#### Frontend
- React TypeScript PWA (Vite)
- 인증 플로우, 프로필 아바타, 사용자 검색 페이지
- 채팅방 목록, 실시간 메시지 (STOMP), 읽음/안읽음 표시

#### Infra & CI/CD
- Docker Compose (app, PostgreSQL 16, Redis 7, Grafana/OTEL 모니터링)
- Kubernetes manifests for k3s (namespace, app, postgres, redis, frontend, ingress)
- GitHub Actions CI/CD: ghcr.io 이미지 빌드 + k3s 자동 배포
- Flyway DB 마이그레이션 (V1–V10)

### Changed

#### API Contract
- `GET /boards/{id}/posts` 서브리소스를 `BoardApiController`로 이동
- `GET /posts/{id}/comments`에 페이지네이션 추가 (`page`, `size` 파라미터)
- `POST /posts/{id}/reactions` 응답 201 → 204 (리소스 미생성)
- `ErrorResponse`에 머신 리더블 에러 코드 필드 추가 (`DOMAIN_ERRORTYPE` 포맷)
- `PageResult<T>` 제네릭으로 도메인별 `*PageResult` 래퍼 클래스 통합

#### Security
- JWT에 `iss=ovlo`, `aud=ovlo-api` 클레임 추가 및 검증 강화
- 입력값 `@Size` 제약 추가 (keyword, countryCode 등 쿼리 파라미터)
- Redis `requirepass` 적용 (docker-compose, k8s 모두 Secret 참조)
- Grafana 어드민 비밀번호 하드코딩 제거 → 환경변수
- `k8s/secret.yaml` gitignore 추가

#### Code Quality
- JPA `Object[]` 캐스팅 → `ReviewCountProjection`, `ReviewAvgRatingProjection` 인터페이스 교체
- 단일 문자 변수명 개선 (`eu`→`university`, `v`→`review`)
- 중복 `blankToNull` 헬퍼 추출
- 미사용 `*PageResult` dead code 삭제

### Fixed
- `@Version` 필드 null로 인한 댓글/반응 저장 오류
- STOMP 재접속 시 만료 토큰으로 실패하는 문제
- 팔로우 API 및 채팅방 표시 오류
- 프론트엔드-백엔드 API 불일치 9건
- 채팅 실시간 메시지 전달 실패
- `isMine` 타입 불일치 (`String()` 변환으로 수정)
- 인증되지 않은 요청 401 응답 처리
- V8 Flyway 마이그레이션 버전 충돌 → V9으로 재명명

[Unreleased]: https://github.com/yeonjae1220/Ovlo/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/yeonjae1220/Ovlo/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/yeonjae1220/Ovlo/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/yeonjae1220/Ovlo/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/yeonjae1220/Ovlo/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/yeonjae1220/Ovlo/commits/v0.1.0
