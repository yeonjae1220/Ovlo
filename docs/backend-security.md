# Ovlo Backend — 보안 아키텍처

> 최초 작성: 2026-06-01

---

## 1. 인증 (Authentication)

### HTTP API
- Spring Security `SecurityFilterChain` — JWT Bearer 토큰 검증
- `JwtAuthenticationFilter`: `Authorization: Bearer <token>` 헤더 파싱 → `SecurityContextHolder` 주입
- 인증된 memberId는 `@AuthenticationPrincipal Long memberId`로 컨트롤러에서 수령

### WebSocket (STOMP)
- `JwtChannelInterceptor`: STOMP CONNECT 프레임에서 `Authorization` 헤더 검증
- 유효한 토큰이면 `sessionAttributes["memberId"]`에 저장, 이후 SEND/SUBSCRIBE 프레임에서 재사용
- CONNECT 실패 시 `IllegalArgumentException` → 연결 거부

### 토큰 구조
| 종류 | 유효시간 | 저장 위치 |
|------|----------|-----------|
| Access Token | 15분 | 클라이언트 메모리 (Zustand) |
| Refresh Token | 7일 | Redis (`auth:token:{token}`) |

---

## 2. 인가 (Authorization)

### HTTP API 인가 원칙
- **모든 채팅 관련 엔드포인트**: `@AuthenticationPrincipal`로 인증된 memberId 요구
- **채팅방 단건 조회 / 메시지 목록 조회**: 참여자 여부를 `isMemberOfRoom()`으로 사전 검증

```java
// ChatApiController — 공통 참여자 검증 헬퍼
private void requireMembership(Long chatRoomId, Long memberId) {
    if (!getChatRoomQuery.isMemberOfRoom(chatRoomId, memberId)) {
        throw new ChatException("채팅방 접근 권한이 없습니다", ChatException.ErrorType.NOT_FOUND);
    }
}
```

> **NOT_FOUND로 응답하는 이유**: 403 Forbidden 대신 404 Not Found를 반환해 채팅방 존재 자체를 노출하지 않음 (정보 은닉 패턴).

### WebSocket 구독 인가
- `ChatSubscriptionInterceptor`: STOMP SUBSCRIBE 프레임에서 `/topic/chat/{roomId}` 구독 시 참여자 검증
- 비참여자 구독 시도 → `MessageDeliveryException`

### 메시지 전송 인가
- `ChatCommandService.sendMessage()`: `loadChatPort.isMember()` (EXISTS 쿼리 1회)로 참여자 확인 후 INSERT

---

## 3. 입력 검증 (Input Validation)

### REST API
| 엔드포인트 | 검증 항목 |
|------------|-----------|
| `POST /api/v1/chat/rooms` | `type`: `@Pattern(regexp = "DM\|GROUP")` — 허용된 Enum 값만 수락 |
| `POST /api/v1/chat/rooms` | `participantIds`: `@NotNull @NotEmpty` |
| `GET /api/v1/chat/rooms/{id}/messages` | `page`: `@Min(0)`, `size`: `@Min(1) @Max(100)` |

`ChatRoomType.valueOf(type)` 호출 전에 `@Pattern`으로 사전 검증하여 `IllegalArgumentException` → 500 경로 차단.

### WebSocket 메시지 (`saveMessage` 경로)
`ChatRoom.addMessage()` → `Message.create()`를 거치지 않는 `saveMessage` 직접 경로에서도 동일한 검증 적용:

```java
// ChatPersistenceAdapter.saveMessage()
if (content == null || content.isBlank()) {
    throw new IllegalArgumentException("메시지 내용은 빈 값일 수 없습니다");
}
```

> **배경**: `sendMessage` 최적화(Phase 3) 과정에서 도메인 모델의 불변식 검증이 우회되는 경로가 생겼고, persistence 레이어에서 동일한 검증을 명시적으로 재적용.

---

## 4. 타임존 보안 (Timezone Safety)

### 문제
`MessageJpaEntity.sentAt`, `ChatRoomReadMarkerJpaEntity.lastReadAt`이 `LocalDateTime`으로 저장될 경우 JVM 로컬 타임존에 따라 DB 값 해석이 달라질 수 있음. 서울(UTC+9) 환경에서 9시간 오차 발생 가능.

### 해결 (2026-06-01)
- 도메인/엔티티 타임 필드 전체를 `Instant`로 교체
- `application.yml` (dev/prod 모두): `hibernate.jdbc.time_zone: UTC` 적용

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

이로써 DB TIMESTAMPTZ ↔ Java Instant 간 변환이 항상 UTC 기준으로 일관되게 처리됨.

---

## 5. 에러 응답 보안

### 원칙
- 도메인 예외(ChatException 등): `GlobalExceptionHandler`에서 의미 있는 HTTP 코드로 변환
- 내부 예외(스택 트레이스, SQL 오류 등): 절대 클라이언트에 노출하지 않음
- `handleGeneral(Exception)`: 서버 로그에만 기록, 클라이언트엔 `INTERNAL_SERVER_ERROR` 코드만 반환

### 404 vs 403 정책
채팅방 관련 인가 실패 시 **NOT_FOUND(404)** 반환. 채팅방 존재 여부를 외부에 노출하지 않아 열거 공격(Enumeration Attack) 방어.

---

## 6. CORS

`SecurityConfig.CorsConfigurationSource`:
- 허용 오리진: `${CORS_ALLOWED_ORIGINS:http://localhost:3000}` (환경변수로 주입)
- 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
- 허용 헤더: Authorization, Content-Type 등
- 자격증명 허용: `allowCredentials = true`

---

## 7. 보안 체크리스트

### 채팅 기능
- [x] WebSocket CONNECT 시 JWT 검증
- [x] STOMP SUBSCRIBE 시 참여자 인가 검증
- [x] REST API `getById` — 참여자만 조회 가능
- [x] REST API `getMessages` — 참여자만 조회 가능
- [x] `sendMessage` — 참여자만 전송 가능
- [x] content null/blank 검증 (WebSocket 경로 포함)
- [x] `type` 파라미터 Enum 사전 검증 (`@Pattern`)
- [x] 인가 실패 시 404 반환 (정보 은닉)

### 공통
- [x] 모든 시크릿 환경변수 관리 (`.env` 파일, k8s Secret)
- [x] Flyway 마이그레이션 — DDL 변경 추적
- [x] `@Version` Optimistic Lock — 게시글 좋아요 동시성
- [x] Spring Security — 전 엔드포인트 기본 인증 요구
- [x] `/ws/**`, `/api/v1/universities/**` 등 명시적 permitAll만 공개

---

## 8. 알려진 제한사항 및 향후 과제

| 항목 | 현황 | 향후 대응 |
|------|------|-----------|
| 메시지 content 최대 길이 | DB TEXT 무제한 | 애플리케이션 레벨 MAX_LENGTH 상수 + 검증 추가 필요 |
| DM 채팅방 unique constraint | TOCTOU race condition 방어 미완 (코드 주석 참조) | V7 migration에 unique constraint 추가 |
| 채팅 Rate Limiting | 미적용 | WebSocket 메시지 빈도 제한 (Bucket4j 등) |
| 메시지 sender_id FK | message 테이블에 member FK 없음 | V7 이후 migration에서 FK 추가 권장 |
