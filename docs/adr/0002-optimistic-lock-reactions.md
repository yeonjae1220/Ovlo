# ADR-0002: 게시글 좋아요/싫어요 동시성 — Optimistic Lock

- 상태: Accepted
- 날짜: 2026-03-06
- 결정자: Backend

## 맥락

게시글의 좋아요/싫어요는 동시 요청이 몰릴 수 있는 카운터다. 한 사용자는 한 게시글에
LIKE 또는 DISLIKE 중 하나만 가질 수 있고(반대 반응 시 기존 제거 후 추가), 동시
업데이트로 인한 lost update를 막아야 한다.

## 결정

1차 구현은 **Optimistic Lock(`@Version`)**을 사용한다.

- `PostJpaEntity`에 `@Version` 컬럼을 둔다. 도메인 모델 `Post`에는 버전을 두지 않는다
  (영속성 관심사이므로 어댑터 계층에 격리).
- 충돌 시 `OptimisticLockException` → 재시도/409 응답으로 처리.
- 반응 저장은 `PostPersistenceAdapter.save()`에서 `@Transactional` + reactions
  delete-all 후 re-insert로 일관성 유지.

## 대안

- **Pessimistic Lock**: 행 잠금으로 경합이 크면 처리량 저하. 현재 트래픽엔 과함.
- **Redis counter**: 원자적 증감으로 고트래픽에 유리하나 영속성/정합성 동기화 복잡도 증가.

## 결과

- 저트래픽 단계에서는 Optimistic Lock으로 충분하며 추가 인프라가 불필요하다.
- **트리거**: 반응 쓰기 경합으로 `OptimisticLockException` 재시도율이 유의미하게
  상승하면 Redis counter로 교체한다. 이 전환은 어댑터 계층 교체로 국한된다(ADR-0001).
