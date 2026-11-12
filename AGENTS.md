# Ovlo — AGENTS.md

교환학생 커뮤니티 플랫폼. TDD + 헥사고날 아키텍처(Ports & Adapters) 기반.

## 기술 스택
- Java 21 (Virtual Threads: `spring.threads.virtual.enabled=true`)
- Spring Boot 4.0.3 (Jakarta EE 11)
- PostgreSQL 16 + JPA + Spring Data + QueryDSL 5
- Redis 7 (JWT 세션 + 캐싱)
- Lombok, SpringDoc OpenAPI (Swagger UI)
- Docker 이미지 + GitHub Actions → Kubernetes 운영 배포
- OTEL + Grafana 모니터링, K6 성능 테스트

## 베이스 패키지
`me.yeonjae.ovlo` (build.gradle group = 'me.yeonjae')

## 도메인 목록 (12개 Bounded Context)
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
| `calendar` | 학교·개인 일정, 학교 구독, 담당자 신청·승인·학교 배정 |
| `office` | 국제처 운영, 담당자 소속, 공지·문의·상담·제휴 |
| `verification` | 학교 이메일 등 인증 수단·상태와 신뢰 등급 |
| `marketplace` | 학교·지역 중고거래, 묶음·나눔, 요청 통화, 게시글 번역·상품별 문의 |

중고거래 설계·검증: [`docs/marketplace.md`](docs/marketplace.md). 일반 `Post`와 별도 저장하며
상황 태그·거래 위치·카테고리를 구분한다. 요청 통화는 거래 국가와 독립적이다.
상품 쓰기는 `version`, 번역 캐시는 상품 ID·버전·목표 언어로 관리한다. 스키마는 V43이며
게시글·사진 접근에는 현재 회원 상태·차단·운영자 숨김 여부를 적용한다.

## 회원 역할과 학교 담당자 권한

`MemberRole`은 `MEMBER / UNIV / ADMIN`이다.

| 역할 | 캘린더 권한 |
|------|-------------|
| `MEMBER` | 학교 구독·게시 일정 조회, 본인 개인 일정 관리, 학교 담당자 신청 |
| `UNIV` | 일반 회원 기능 + **배정된 학교**의 일정 관리 |
| `ADMIN` | 학교 담당자 심사·배정 해제, 학교 일정 관리·공식 출처 기반 대리 등록 |

- 공개 가입은 모두 `MEMBER`로 시작한다. 학교 담당자 가입은 계정과 `PENDING` 신청을 같은 트랜잭션에서 생성한다.
- 운영진이 공식 연락처 등으로 확인한 뒤 승인하면 `MEMBER → UNIV` 변경과 학교 배정을 함께 저장한다. 기존 회원도 신청할 수 있다.
- `UNIV` 역할만으로 학교 관리 권한이 생기지 않는다. 학교 프로필·학교 이메일·구독도 관리 권한을 부여하지 않는다.
- 학교 일정 변경은 DB의 현재 회원 상태·역할·배정을 확인한다. JWT만 믿지 않으며, 마지막 배정을 해제해도 역할은 `UNIV`로 남지만 해당 학교 관리 권한은 사라진다.
- 개인 일정은 소유 회원 조건으로 조회·변경·삭제한다. `ADMIN`도 타인의 개인 일정에 접근할 수 없다.
- `/admin/**`는 별도 관리자 세션 인증·CSRF 보호를 사용한다. 회원 JWT의 역할과 관리자 화면 인증을 혼동하지 않는다.
- 국제처의 `OfficeRole` (`OFFICE_ADMIN / OFFICE_STAFF`) 및 소속 권한은 `MemberRole`·캘린더 학교 배정과 별도다.

## 캘린더 설계와 불변식

상세: [`docs/calendar.md`](docs/calendar.md).

- `UniversityEvent`, `PersonalEvent`, `UniversityApplication`은 엔티티, `EventPeriod`, `EventDetails`, `EventSource`는 불변 값 객체다.
- 학교 일정과 개인 일정은 별도 저장하고 조회 시 합친다. 학교 일정을 학생별로 복사하지 않는다.
- 학교 일정 상태는 `DRAFT / PUBLISHED / CANCELLED`다. 구독 학생에게 게시·취소 상태를 표시하고 초안은 담당자·운영진만 조회한다. 게시된 일정은 삭제 대신 취소한다.
- 담당자 등록은 `UNIVERSITY`, 운영진 대리 등록은 `OVLO_CURATED`다. 대리 등록에는 HTTP(S) 출처 URL과 확인일이 필수이며, 등록 경로·최초 작성자는 서버에서 설정하고 수정 시 보존한다.
- 출처 URL은 링크로만 사용한다. 공식성은 운영진이 확인하며 자동 수집·자동 갱신은 구현하지 않았다.
- 종일 일정은 `LocalDate`, 시간 지정 일정은 `Instant` + IANA 시간대다. 종료는 미포함이며, UI의 종일 마지막 날(포함)을 API 전송 시 변환한다. 모호하거나 존재하지 않는 DST 현지 시각은 거부한다.
- 일정 편집·상태 변경·삭제와 신청 심사는 `version`으로 경합을 검사하고 오래된 버전은 409로 거절한다. 학교 일정 변경·담당자 심사는 같은 트랜잭션에서 감사 기록을 남긴다.
- 캘린더 스키마는 `V41__calendar_and_university_applications.sql`이다. 기존 V34는 Office 마이그레이션이므로 수정하거나 번호를 재사용하지 않는다.

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
- **좋아요/싫어요 동시성**: 회원별 반응 행의 명시적 INSERT·조건부 UPDATE/DELETE + 같은 트랜잭션의 카운트 원자 증감. 아래 규칙을 따른다.
- **StoragePort**: `LocalStorageAdapter` (Phase 1) → `S3StorageAdapter` (Phase 2) 인터페이스 교체
- **TokenStorePort**: `RedisTokenAdapter` 구현 (JWT Refresh Token 저장)
- **HEIC 변환**: TwelveMonkeys ImageIO 라이브러리
- **채팅**: STOMP over WebSocket, 추후 Redis Pub/Sub 브로커로 확장
- **DB 마이그레이션**: Flyway (V1__, V2__,... + R__seed_universities.sql)

### 좋아요/싫어요 저장 규칙

- `post_reaction`의 `(post_id, member_id)` PK로 회원별 반응을 관리한다. 신규 반응은 명시적 INSERT로 저장한다. 복합 ID의 JPA `save()`/`merge()`로 대체하지 않는다.
- 전환·취소는 이전 반응 타입을 조건으로 UPDATE/DELETE하며, **변경 행 수가 1인 요청만** 카운트를 조정한다. 카운트는 `count = count + delta`로 갱신한다.
- 도메인 사전 조회만으로 경합을 막을 수 없다. 저장 단계에서도 동일 반응은 409, 조건부 변경 경합은 409, 존재하지 않는 반응 취소는 404로 처리한다. 중복 키 예외를 잡아 성공으로 넘기지 않는다.
- `PostJpaEntity`의 `@Version`은 유지되지만 반응 동시성은 위 조건부 DML로 처리한다. 전체 게시글 저장으로 반응 행을 덮어쓰거나 Redis counter·낙관적 락 재시도를 현재 구현으로 가정하지 않는다.
- `PostReactionConcurrencyIntegrationTest`는 실제 PostgreSQL에서 중복·전환·취소 경합과 행/카운트 일치를 검증하며 PR·배포 CI에서 실행한다. 정확히 한 요청만 성공한다는 중복 요청 검증을 완화하지 않는다.

## 초기 TDD 구현 순서
1. member → 2. auth → 3. university → 4. board → 5. post(동시성) → 6. media → 7. follow → 8. chat

## 도메인 설계 시 /domain-modeling 스킬 활용
각 도메인 구현 시작 전 반드시 `/domain-modeling {도메인명}` 스킬로 VO/Entity 판별 및 불변식 설계

## 검증 명령
```bash
# 도메인 순수성 검증
grep -r "jakarta.persistence" src/main/java/me/yeonjae/ovlo/domain/
grep -r "JpaRepository" src/main/java/me/yeonjae/ovlo/application/service/

# 일반 테스트 (integration 태그 제외)
./gradlew test

# 전체 통합 테스트 (Docker 필요, PostgreSQL/Testcontainers 포함)
./gradlew integrationTest

# 좋아요 동시성 회귀 테스트 (PR·배포 CI에서도 실행)
./gradlew integrationTest --tests '*PostReactionConcurrencyIntegrationTest'

# 캘린더 날짜·시간대·DST 유틸리티 검증
cd frontend && node scripts/test-calendar.cjs

# Swagger UI
http://localhost:8080/swagger-ui.html
```

---

## 프론트엔드 (Next.js 15 App Router)

> 상세 문서: [`docs/frontend-security.md`](docs/frontend-security.md), [`docs/frontend-architecture.md`](docs/frontend-architecture.md)
> 백엔드 보안 아키텍처: [`docs/backend-security.md`](docs/backend-security.md)
> 캘린더·학교 담당자 가입: [`docs/calendar.md`](docs/calendar.md)

**스택**: Next.js 15, React 19, TypeScript, Zustand, TanStack Query, axios

### 라우팅 그룹
| 그룹 | 경로 | 설명 |
|------|------|------|
| `(main)` | `/exchange-universities`, `/university-reports` | AppLayout, 공개 |
| `(protected)` | `/boards`, `/posts`, `/marketplace`, `/chat`, `/profile`, `/follow`, `/my-university`, `/calendar`, `/calendar/manage`, `/university-account` | 인증 필요, hydrated 후 redirect |
| 공개 가입 | `/register`, `/register/member`, `/register/university` | 가입 유형 선택, 일반 회원 가입, 학교 담당자 가입·신청 |

`/admin/calendar`는 Next.js 라우트가 아닌 백엔드 관리자 화면이며 담당자 심사와 공식 출처 기반 대리 등록을 제공한다.

### 인증 핵심 패턴
- `accessToken`: Zustand 메모리 전용 (localStorage 저장 안 함)
- `currentUser`: Zustand persist (localStorage, version 4)
- 리로드 시: `(protected)/layout.tsx` → `refreshAuth()` 싱글톤 → 1회 POST
- `refreshAuth.ts`: layout + axios interceptor 공유 Promise (rotation 환경에서 이중 소비 방지)

### CSP
`src/middleware.ts`에서 요청마다 nonce 생성:
```
script-src 'nonce-{n}' 'strict-dynamic' 'unsafe-inline'
base-uri 'self'
```

### k8s 환경변수
- `API_URL`: 내부 클러스터 URL (서버 전용, 클라이언트 번들 미포함)
- `PORT=3000`, `HOSTNAME=0.0.0.0`

## 학교 챗봇 대화와 상담 이관

- 챗봇은 저장된 이전 문의를 연결해 최대 20회 이어가며, 학생 본인·같은 학교 범위를 검사한다.
- 상담 이관은 v2 동의 후 전체 대화를 담당자에게 전달한다. 기존 v1 동의 기록은 보존한다.
- `V44__inquiry_conversation.sql`이 대화 연결과 이관 스냅샷을 추가한다. 기존 V42(대학 상태)와 V43(중고거래)는 변경하지 않는다.
