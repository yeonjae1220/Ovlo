# ADR-0001: 헥사고날 아키텍처 (Ports & Adapters) 채택

- 상태: Accepted
- 날짜: 2026-03-06
- 결정자: Backend

## 맥락

교환학생 커뮤니티 플랫폼은 8개의 Bounded Context(member, auth, university, board,
post, follow, media, chat)를 가지며, 인프라(PostgreSQL, Redis, 로컬/S3 스토리지,
WebSocket)가 단계적으로 교체될 예정이다. 도메인 규칙이 프레임워크/인프라 세부사항에
오염되면 테스트와 교체 비용이 급격히 커진다.

## 결정

헥사고날 아키텍처(Ports & Adapters)를 채택한다.

```
adapter/in  → application/port/in  → domain
adapter/out ← application/port/out ← domain
```

- `domain/{도메인}/model/`은 **순수 Java**만 허용한다. JPA/Spring 어노테이션·import 금지.
- 애플리케이션 서비스는 `JpaRepository`를 직접 주입하지 않고 `port/out` 인터페이스에만 의존한다.
- 도메인 간 참조는 ID(record VO)로만 한다 (예: `MemberId`).

## 근거

- 도메인 단위 테스트가 프레임워크 부팅 없이 가능 (단위/슬라이스 테스트 227+ 통과).
- 인프라 교체를 어댑터 교체로 국한 (`LocalStorageAdapter → S3StorageAdapter`,
  `RedisTokenAdapter`). 도메인은 무변경.
- 의존성 방향이 단방향이라 순환 의존을 구조적으로 차단.

## 결과

- 보일러플레이트(매퍼, 포트 인터페이스) 증가는 감수한다.
- 규칙 위반(도메인에 `jakarta.persistence`/Spring 유입)은 `.claude/hooks/domain-purity-guard.js`
  PreToolUse 훅과 CI의 `grep` 검증으로 강제한다.

## 검증

```bash
grep -r "jakarta.persistence" src/main/java/me/yeonjae/ovlo/domain/   # 결과 없어야 함
grep -r "JpaRepository" src/main/java/me/yeonjae/ovlo/application/service/  # 결과 없어야 함
```
