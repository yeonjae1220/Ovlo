# Ovlo — CLAUDE.md

교환학생 커뮤니티 플랫폼. TDD + 헥사고날 아키텍처(Ports & Adapters) 기반.

## 기술 스택
- Java 21 (Virtual Threads: `spring.threads.virtual.enabled=true`)
- Spring Boot 4.0.3 (Jakarta EE 11)
- PostgreSQL 16 + JPA + Spring Data + QueryDSL 5
- Redis 7 (JWT 세션 + 캐싱)
- Lombok, SpringDoc OpenAPI (Swagger UI)
- Docker Compose 배포
- OTEL + Grafana 모니터링, K6 성능 테스트

## 베이스 패키지
`me.yeonjae.ovlo` (build.gradle group = 'me.yeonjae')

## 도메인 목록 (8개 Bounded Context)
| 도메인 | 책임 |
|--------|------|
| `member` | 회원 신원·프로필 관리 |
| `auth` | JWT + Redis 인증/인가 |
| `university` | 대학 마스터·검색·지도 |
| `board` | 게시판 생성·범위·구독 |
| `post` | 게시글·댓글·좋아요(동시성) |
| `follow` | 팔로잉/팔로워 관계 |
| `media` | 파일 업로드·HEIC 변환·스토리지 |
| `chat` | DM·그룹채팅 (WebSocket STOMP) |

## 패키지 구조 규칙
```
me.yeonjae.ovlo/
├── domain/{도메인}/model/     ← 순수 Java, 프레임워크 import 절대 금지
├── domain/{도메인}/exception/
├── application/port/in/{도메인}/   ← UseCase, Query 인터페이스
├── application/port/out/{도메인}/  ← LoadXxxPort, SaveXxxPort 인터페이스
├── application/service/command/    ← UseCase 구현체 (@Service)
├── application/service/query/      ← Query 구현체 (@Service)
├── application/dto/command/        ← 순수 record (검증 없음)
├── application/dto/result/         ← 응답용 record
├── adapter/in/web/                 ← @RestController, @Valid 검증
├── adapter/in/web/ws/              ← WebSocket 핸들러
├── adapter/out/persistence/        ← JPA @Entity, Repository, Adapter
├── adapter/out/redis/              ← Redis 어댑터
├── adapter/out/storage/            ← StoragePort 구현 (Local/S3)
└── shared/                         ← Config, Exception, Security
```

## 의존성 방향 (절대 위반 금지)
```
adapter/in  → application/port/in  → domain
adapter/out ← application/port/out ← domain
❌ domain → application (역방향 금지)
❌ application/service → JpaRepository 직접 주입 금지
❌ domain 에 @Entity, @Service, @Component 등 금지
```

## 핵심 설계 결정
- **좋아요/싫어요 동시성**: Optimistic Lock (`@Version`) → 트래픽 증가 시 Redis counter 교체
- **StoragePort**: `LocalStorageAdapter` (Phase 1) → `S3StorageAdapter` (Phase 2) 인터페이스 교체
- **TokenStorePort**: `RedisTokenAdapter` 구현 (JWT Refresh Token 저장)
- **HEIC 변환**: TwelveMonkeys ImageIO 라이브러리
- **채팅**: STOMP over WebSocket, 추후 Redis Pub/Sub 브로커로 확장
- **DB 마이그레이션**: Flyway (V1__, V2__,... + R__seed_universities.sql)

## TDD 구현 순서
1. member → 2. auth → 3. university → 4. board → 5. post(동시성) → 6. media → 7. follow → 8. chat

## 도메인 설계 시 /domain-modeling 스킬 활용
각 도메인 구현 시작 전 반드시 `/domain-modeling {도메인명}` 스킬로 VO/Entity 판별 및 불변식 설계

## 검증 명령
```bash
# 도메인 순수성 검증
grep -r "jakarta.persistence" src/main/java/me/yeonjae/ovlo/domain/
grep -r "JpaRepository" src/main/java/me/yeonjae/ovlo/application/service/

# 전체 테스트
./gradlew test

# Swagger UI
http://localhost:8080/swagger-ui.html
```
