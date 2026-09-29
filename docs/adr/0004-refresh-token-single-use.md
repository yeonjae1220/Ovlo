# ADR-0004: Refresh Token 재발급은 제출 토큰 기준 원자적 compare-and-rotate

- 상태: Accepted
- 날짜: 2026-09-28
- 결정자: Backend / Security
- 대체: LLM-Wiki OV-ADR-010(WATCH/MULTI/EXEC CAS) 의 rotation 부분
- 관련 Pitfall: GLOBAL-PIT-001, GLOBAL-PIT-012, GLOBAL-PIT-051

## 맥락

`RedisTokenAdapter.save()` 는 WATCH/MULTI/EXEC 로 "세션에 **지금 저장된** 토큰" 기준의 역인덱스 교체를
보호했다. 그래서 세션당 유효 토큰이 하나만 남는 건 보장됐지만, **클라이언트가 제출한 토큰**과는 비교하지 않았다.

- 같은 옛 토큰으로 온 요청 A·B 가 둘 다 `findByRefreshToken` 을 통과하고, 둘 다 200 과 서로 다른 새 토큰을 받는다.
  Redis 에는 나중에 저장된 쪽만 남는다. 브라우저 쿠키에 남은 것이 다른 쪽이면 다음 재발급에서 조용히 로그아웃된다.
- refresh 가 find 한 뒤 logout 이 세션을 지우면, refresh 의 `save()` 가 `putAll` 로 세션을 되살렸다.
- 교체된 옛 토큰이 다시 와도 401 만 줄 뿐 기록이 없었다.
- 프론트 single-flight 는 탭 안에서만 동작했고, 선제 갱신 타이머(`useAuth.ts`)는 싱글톤을 우회했다.

## 결정

1. `TokenStorePort.rotate(AuthSession rotated, String presentedToken)` → `RefreshRotationOutcome`.
   Lua 한 번으로 판정과 쓰기를 원자적으로 수행한다(`ROTATE_SCRIPT`).

   | 제출 토큰 해시 | 조건 | 결과 | HTTP |
   |---|---|---|---|
   | = 현재 토큰 | 세션 있음·미만료·미무효 | `ROTATED` | 200 |
   | = 직전 토큰 | 교체 후 10초(`CONCURRENT_GRACE`) 이내 | `CONCURRENT` | 409 `AUTH_CONFLICT` |
   | = 직전 토큰 | 10초 이후 | `REUSED` → 경고 로그 | 401 |
   | 그 외·세션 없음 | | `INVALID` | 401 |

   세션 Hash 에 `prevRefreshToken`(해시)·`rotatedAt` 을 둔다. 직전 토큰의 역인덱스는 `REUSE_DETECTION_WINDOW`(1일)
   동안만 남긴다. 세션이 없으면 아무것도 쓰지 않으므로 logout 경합에서 세션이 되살아나지 않는다.
2. `save()` 는 새 세션 생성 전용(MULTI/EXEC 쓰기만)으로 줄인다.
3. 재사용 감지는 **1단계(기록만)**: 세션은 폐기하지 않는다. iOS WebView 가 Set-Cookie 를 저장하지 못하고 직전
   토큰을 다시 보내는 정상 사용자(GLOBAL-PIT-051)와 탈취를 아직 구분할 근거가 없기 때문이다. 로그 빈도를 본 뒤
   세션 폐기(2단계)로 올린다. 2단계는 코드 변경 없이 `ovlo.auth.refresh-reuse.revoke-session=true`
   (env `AUTH_REFRESH_REUSE_REVOKE_SESSION`)로 켠다 — `REUSED` 때 직전 토큰 역인덱스로 그 세션을 지운다.
   `CONCURRENT`(409)는 켜도 세션을 지우지 않는다.
4. 프론트: 모든 재발급을 `refreshAuth()` 로 모으고 Web Locks 로 탭 사이를 직렬화한다. 409 는 1회 재시도,
   일시적 실패는 로그아웃하지 않는다(`docs/frontend-security.md` §2).

## 검토했으나 채택 안 한 대안

| 대안 | 기각 이유 |
|---|---|
| WATCH 안에서 제출 토큰 비교 | 가능하지만 4갈래 판정 + 재시도 루프가 Lua 한 번보다 길다. `RateLimiterService` 가 이미 `RedisScript` 를 쓴다 |
| 진 요청에도 같은 새 토큰을 돌려주기 | 원문 토큰을 Redis 에 잠시라도 보관해야 해 GLOBAL-PIT-001(해시만 저장)과 충돌 |
| 재사용 즉시 세션 폐기 | GLOBAL-PIT-051 오탐이 곧 강제 로그아웃이 된다. 측정 후 결정 |
| 조건부 교체(24h 동안 같은 토큰 재사용) | 경합이 사라지지만 single-use 계약을 포기한다 |

## 결과

- `RedisTokenAdapterTest`: 같은 토큰으로 8스레드 × 15라운드 동시 재발급 → 매 라운드 ROTATED 정확히 1, 나머지 CONCURRENT,
  패자 토큰 미저장. 비교를 빼는 변이(`if current then`)에서 3개 테스트가 실패함을 확인했다.
- `AuthRefreshApiTest`: 409 응답은 `AUTH_CONFLICT` 이고 Set-Cookie 를 보내지 않는다.
- 한계: 직전 한 세대만 추적한다. 두 세대 이전 토큰은 `INVALID`(401)로만 보이고 재사용 로그가 남지 않는다.
