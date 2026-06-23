# ADR-0003: Refresh Token은 Redis 저장 전 SHA-256 해시

- 상태: Accepted
- 날짜: 2026-06-23
- 결정자: Backend / Security
- 관련 Pitfall: GLOBAL-PIT-001

## 맥락

JWT 인증은 Access Token(15분, stateless) + Refresh Token(7일, Redis 저장) 구조다.
1인 1세션을 위해 `auth:session:member:{id}` 해시와 `auth:token:{token}` 역인덱스를
Redis에 둔다. Refresh Token을 **평문으로** 저장하면 Redis 덤프/유출 시 토큰을 그대로
재사용해 세션 탈취가 가능하다.

## 결정

Refresh Token은 Redis에 저장하거나 조회 키로 쓰기 전에 **항상 SHA-256으로 해시**한다.

- `shared/security/TokenHashUtil.java`가 해시를 전담한다 (단일 책임).
- 평문 토큰은 클라이언트에만 존재하고, 서버는 해시만 보관한다.
- 검증 시 들어온 토큰을 동일하게 해시해 저장된 해시와 비교한다.

## 근거

- Redis 유출 시에도 평문 토큰을 복원할 수 없어 재사용 공격을 차단한다.
- SHA-256은 토큰(고엔트로피 랜덤 값) 조회용으로 충분하다. 비밀번호와 달리 무차별
  대입 위험이 낮아 솔트/느린 해시(BCrypt)는 불필요하다.

## 결과

- 회귀 방지: `RedisTokenAdapterTest`, `TokenHashUtilTest`로 평문 미저장을 검증한다.
- 동일 취약점 재발은 전역 Pitfall(GLOBAL-PIT-001)로 추적한다.
