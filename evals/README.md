# Ovlo Evals

핵심 사용자 플로우와 아키텍처 불변식에 대한 평가(eval) 픽스처. 각 `*.eval.json`은
하나의 시나리오를 **사전조건 → 절차 → 합격 기준(pass criteria)**으로 기술한다.
프롬프트/에이전트/구현 변경 시 회귀를 잡기 위한 기준선 역할을 한다.

## 구조

| 파일 | 대상 플로우 | 연관 |
|------|-------------|------|
| `auth-flow.eval.json` | 회원가입 → 로그인 → JWT 보호 자원 → refresh 회전 | ADR-0003 |
| `post-concurrency.eval.json` | 좋아요/싫어요 동시성, Optimistic Lock | ADR-0002 |
| `domain-purity.eval.json` | 도메인 레이어 순수성(JPA/Spring 미유입) | ADR-0001 |

## 실행 방법

이 픽스처는 사람이 읽는 명세이자 자동화 대상이다.

- 도메인 순수성은 CI에서 결정론적으로 검증한다:
  ```bash
  grep -r "jakarta.persistence" src/main/java/me/yeonjae/ovlo/domain/ && exit 1 || true
  grep -r "JpaRepository" src/main/java/me/yeonjae/ovlo/application/service/ && exit 1 || true
  ```
- auth/concurrency 플로우는 통합 테스트(Testcontainers, `@Tag("integration")`)로 커버한다:
  ```bash
  ./gradlew integrationTest
  ```

## 합격 기준 철학

- 기대 동작을 구현 **전에** 명시한다 (Eval-Driven Development).
- 각 시나리오는 명확한 PASS/FAIL 경계를 가진다 (모호한 "잘 동작함" 금지).
