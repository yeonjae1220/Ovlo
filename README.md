# Ovlo

교환학생 커뮤니티 플랫폼. 대학 간 교류를 지원하는 게시판, 채팅, 미디어 공유 서비스입니다.

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 4.0.3 (Jakarta EE 11) |
| Database | PostgreSQL 16 + Spring Data JPA + QueryDSL 5 |
| Cache / Session | Redis 7 (JWT refresh token, SHA-256 해시 저장) |
| Frontend | Next.js 15 + React 19 (App Router) · i18n(7개 언어) · PWA |
| Migration | Flyway |
| Security | Spring Security · JWT(httpOnly 쿠키, 멀티세션) · 학생 이메일 인증 |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Monitoring | OpenTelemetry + Prometheus + Grafana · logstash JSON 구조화 로그 |
| Deploy | Docker · Kubernetes(k3s) · GitHub Actions CI/CD |

> 프론트엔드는 v0.4.0에서 Vite + React SPA → Next.js App Router로 마이그레이션되었습니다.

## Architecture

TDD + 헥사고날 아키텍처(Ports & Adapters) 기반으로 설계되었습니다.

```
me.yeonjae.ovlo/
├── domain/              ← 순수 Java 도메인 모델 (프레임워크 의존 없음)
├── application/
│   ├── port/in/         ← UseCase / Query 인터페이스
│   ├── port/out/        ← LoadXxxPort / SaveXxxPort 인터페이스
│   ├── service/         ← 유스케이스 구현체
│   └── dto/             ← Command / Result record
└── adapter/
    ├── in/web/          ← REST Controller
    ├── in/web/ws/       ← WebSocket (STOMP)
    ├── out/persistence/ ← JPA Entity / Repository
    ├── out/redis/       ← Redis Adapter
    └── out/storage/     ← 파일 스토리지 (Local → S3)
```

## Domains

| 도메인 | 책임 |
|--------|------|
| `member` | 회원 신원 · 프로필 관리 |
| `auth` | JWT + Redis 인증 / 인가 |
| `university` | 대학 마스터 · 검색 · 지도 |
| `board` | 게시판 생성 · 범위 · 구독 |
| `post` | 게시글 · 댓글 · 좋아요 (Optimistic Lock) |
| `follow` | 팔로잉 / 팔로워 관계 |
| `media` | 파일 업로드 · HEIC 변환 · 스토리지 |
| `chat` | DM · 그룹채팅 (WebSocket STOMP) |
| `verification` | 학교 이메일 기반 교환학생 인증 (SMTP) · 게시판 게이팅 |
| `report` | AI 대학 리포트 (다국어) |

## Getting Started

### Prerequisites

- Docker & Docker Compose
- (로컬 개발) Java 21, Node.js 20+

### 환경 변수 설정

```bash
cp .env.example .env
# .env를 열어 JWT_SECRET 등 필수 값을 채워주세요
```

### Docker로 실행

```bash
docker compose -f docker/docker-compose.yml up -d
```

| 서비스 | 주소 |
|--------|------|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3001 (SSH 터널 권장) |

### 로컬 개발 (백엔드)

```bash
# dev 프로파일 사용 (H2 인메모리 DB)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트

```bash
# 단위 · 슬라이스 테스트
./gradlew test

# 통합 테스트 (Docker 필요)
./gradlew integrationTest
```

## Key Design Decisions

- **좋아요 동시성**: JPA `@Version` Optimistic Lock → 고트래픽 시 Redis counter로 교체 가능
- **파일 스토리지**: `StoragePort` 인터페이스로 추상화, `LocalStorageAdapter` → `S3StorageAdapter` 무중단 교체
- **HEIC 변환**: `ImageConverterPort` + TwelveMonkeys ImageIO
- **인증**: Access Token 15분 (stateless) + Refresh Token(Redis, SHA-256 해시 저장). httpOnly 쿠키 기반 로그인 지속 + 멀티세션 지원
- **채팅**: STOMP over WebSocket, `SimpleBroker` (Phase 1) → Redis Pub/Sub (Phase 2)

## Version History

버전별 상세 변경 내역은 [CHANGELOG.md](CHANGELOG.md)를 참고하세요.

| 버전 | 날짜 | 핵심 변경 |
|------|------|-----------|
| 0.5.0 | 2026-06-30 | **학생 인증 시스템** · 통합 검색 · admin 락아웃 · 구조화 로깅 |
| 0.4.0 | 2026-06-08 | **AI 대학 리포트** · **Next.js 마이그레이션** · 7개국어 · PWA · 테마 |
| 0.3.0 | 2026-04-26 | 글로벌 대학 검색 · 관리자 패널 · 반응형 UI |
| 0.2.0 | 2026-03-26 | Google OAuth 2.0 로그인 · 온보딩 |
| 0.1.0 | 2026-03-24 | 헥사고날 아키텍처 · 핵심 도메인 8종 · 실시간 채팅 |
