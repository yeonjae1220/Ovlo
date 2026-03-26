# Changelog

All notable changes to Ovlo are documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[Unreleased]: https://github.com/yeonjae1220/Ovlo/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/yeonjae1220/Ovlo/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/yeonjae1220/Ovlo/commits/v0.1.0
