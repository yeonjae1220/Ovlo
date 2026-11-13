# 학교 챗봇 모델 및 악용 방어

현재 구현: 인증·학교 접근 검사 → 원자적 이용량 예약 → 질문 범위 검사 → 승인 공지 검색 → 답변 생성 → 인용/URL 검사 → 독립 근거 검사 → 저장.

## 결과와 화면

| 결과 | 학생 화면 | 상담 |
|---|---|---|
| ANSWERED | 검증된 답변과 출처 | 가능 |
| NO_GROUNDS | 승인 자료에 근거가 없다는 안내 | 가능 |
| OUT_OF_SCOPE | UI 언어별 고정 범위 안내 | 서버에서도 차단 |
| 검사/모델 장애 | 안전한 답변을 확인하지 못했다는 503 안내 | 별도 학교 상담 메뉴 이용 가능 |

범위 밖 요청 뒤에는 정상 질문으로 대화를 계속할 수 있다. 거절된 턴은 이후 모델 문맥에서 제외한다.
비자·개인 자격·괴롭힘 등 실제 학생 지원 요청은 단순 민감어로 차단하지 않는다. 자료로 답하지 못하면 담당자에게 연결한다.

## 인용과 검증 응답 계약

모델에는 DB의 `noticeId`를 보내지 않고 `citation: "[N]"`과 해당 자료의 제목·본문·출처만 전달한다.
검색 순서와 자료 ID가 달라도 `[N]`은 그 요청의 인용 번호 하나만 뜻한다. 답변 검증에는 실제 인용된 자료만
전달하므로, 인용하지 않은 다른 자료의 내용으로 잘못된 인용을 승인할 수 없도록 한다. 학생 응답과 저장된 증거에는
서버가 인용 번호에 해당하는 자료 ID·버전·출처를 연결한다.

질문 범위 검사와 근거 검사는 `output_config.format`의 JSON Schema로 `{"decision":"ALLOW"}` 또는
`{"decision":"DENY"}`만 생성하게 한다. 서버에서도 필수 필드·문자열 enum·추가 필드·중복 키·뒤에 붙은 JSON·종료 사유를
엄격히 검사한다. 형식 오류, 거절, 잘린 응답은 계속 503으로 차단하며 자연어에서 `ALLOW`를 추출하거나 자동 재시도하지 않는다.
답변 생성과 번역의 출력 형식은 유지한다. 사용 중인 게이트웨이의 JSON Schema 지원 여부는 배포 전 실제 모델 평가로 확인한다.
API 계약: [Anthropic structured outputs](https://platform.claude.com/docs/en/build-with-claude/structured-outputs).

건강보험처럼 원문이 특정 기관을 문의처로 지정한 경우, 답변에 국제처 등 다른 기관을 자동으로 덧붙이지 않는다.
개인별 자격을 판단할 수 없다는 원칙은 유지하면서, 일반 규정만 묻는 질문에는 불필요한 자격 판단·문의처 문구를 추가하지 않는다.
실제 경북대 승인 원문을 이용한 건강보험 3회 반복 평가와 개인별 감면 보장 거부를 포함한 25개 결과가 기대값과 일치했다.
기록: [문의처 수정 후 실제 모델 평가](validation/office-ai-health-referral-20261114.json).

## 설정

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| OVLO_LLM_ANSWER_MODEL | claude-sonnet-5-5 | 범위 검사·답변·근거 검사 (Haiku는 지원하지만 별도 품질 평가 필요) |
| OVLO_LLM_MODEL | claude-opus-5-5 | 기존 번역 모델 |
| RATE_LIMIT_INQUIRY_PER_MINUTE | 5 | 회원별 분당 시도 제한 |
| RATE_LIMIT_INQUIRY_PER_DAY | 50 | 회원별 24시간 시도 제한 |
| OVLO_INQUIRY_OFFICE_DAILY_LIMIT | 500 | 학교별 UTC 일일 질문 예약 한도 |
| OVLO_AI_GLOBAL_DAILY_LIMIT | 5000 | 모든 학교 챗봇의 UTC 일일 질문 예약 한도 |
| OVLO_AI_OFFICE_CONCURRENT_LIMIT | 5 | 학교별 동시 질문 |
| OVLO_AI_GLOBAL_CONCURRENT_LIMIT | 20 | 모든 학교 챗봇의 동시 질문 |
| OVLO_AI_OFFICE_WEIGHTED_TOKEN_BUDGET | 25000000 | 학교별 UTC 일일 가중 토큰 |
| OVLO_AI_GLOBAL_WEIGHTED_TOKEN_BUDGET | 100000000 | 모든 학교 챗봇의 UTC 일일 가중 토큰 |

회원별 동시 질문은 학교와 관계없이 1개다. 토큰 계산은 `input + 5 * output`이며,
각 질문 전에 세 번의 최대 요청(각 입력 65,536바이트, 출력 2,048토큰)에 해당하는 227,328을 예약한다.
각 호출은 입력 크기를 검사하고, 입력 상한에는 직렬화 여유를 남긴다. 알려진 사용량만 정산하고 남는 예약량을 반환한다.
오류·타임아웃·프로세스 종료 시 미확인 사용량은 환불하지 않는다. 모델 자동 재시도/폴백에 따른 무제한 비용은 허용하지 않는다.
이 예산은 금액 청구 원장이 아니며 번역 비용은 포함하지 않는다. 학교별 실제 청구에는 공급자 사용 내역 대조가 추가로 필요하다.

## 회귀 검증

```bash
./gradlew test
./gradlew integrationTest
cd frontend && npm ci && npm run lint && npm run build
```

실제 모델 평가는 명시적으로 켤 때만 유료 API를 호출한다. API 키는 환경변수로 안전하게 주입하며 커밋·로그에 넣지 않는다.

```bash
OVLO_LIVE_LLM=1 ./gradlew test --tests '*OfficeAiLiveEvaluationTest' --rerun-tasks
```

`ANTHROPIC_API_KEY`, 선택적으로 `OVLO_LLM_BASE_URL`이 필요하다. 합성 공지와 메모리 저장소를 사용하며
운영 학생 데이터나 운영 DB를 변경하지 않는다. 실제 모델을 사용하는 서비스 흐름 평가이며 운영 HTTP 화면 E2E는 아니다.
Redis·PostgreSQL 동시성과 HTTP 인증 경계는 별도 통합/보안 회귀 테스트로 검증한다.
결과는 `build/reports/live-ai/results.json`에 생성된다.

2026-11-13 검증: 정상 답변 5건, 정상 학교 질문의 근거 부족 3건, 범위 밖/공격 거절 9건,
가짜 인용·오염된 공지 검증 1건(2가지 공격)을 모두 통과했다. 실제 모델 ID는 `anthropic/claude-sonnet-5.5`.
33회 호출, 입력 18,368·출력 708토큰. 기록: [검증 결과](validation/office-ai-guardrails-20261113.json).
정상 다국어 응답에서 날짜·시간·위치·준비물이 유지됐고 범위 밖 결과는 답변과 상담 버튼을 제공하지 않았다.
샘플 평가 통과는 모든 프롬프트 공격에 대한 보장이 아니다.

2026-11-14 인용·판정 수정 검증: 기존 평가에 검색 순서와 자료 ID가 다른 기숙사·건강보험 답변 및 잘못된 출처 거부를 추가했다.
총 21개 결과가 기대값과 일치했으며, 실제 게이트웨이에서 JSON Schema 판정이 동작했다.
기록: [수정 후 실제 모델 평가](validation/office-ai-citation-fix-20261114.json).

## 운영 반영

`k8s/app.yaml`과 배포 workflow 모두 답변용 Sonnet 환경변수를 명시한다. 코드를 배포하기 전 운영 모델은 바뀌지 않는다.
기존 스키마 변경은 없지만 새 결과값 OUT_OF_SCOPE를 읽는 백엔드·프론트엔드를 함께 배포해야 한다.
자세한 결정과 롤백 주의점은 [ADR-0007](adr/0007-office-ai-abuse-controls.md)을 참고한다.

최종 로컬 검증: 일반 테스트 692개 통과(유료 LLM opt-in 진입점 2개는 기본 실행에서 제외), PostgreSQL/Redis 통합 테스트 142개 통과. 실제 LLM 평가는 별도 실행한 18개가 모두 통과했다. `bootJar`, 프론트엔드 lint·production build, `git diff --check`도 통과했다.
