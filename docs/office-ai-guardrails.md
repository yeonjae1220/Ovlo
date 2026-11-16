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

### 상담 메시지 번역 재시도

상담 조회 한 번에 새 번역은 최대 5개다. 실패한 메시지는 Redis에 메시지·목표 언어별 재시도 상태를 남겨
첫 실패 후 60초, 두 번째 실패 후 300초 동안 건너뛴다. 최초 시도부터 24시간 동안 최대 3회만 호출하며,
한도에 도달해도 원문과 `translationFailed`를 반환하고 다른 메시지의 번역을 계속한다.
호출 전에 횟수를 예약하므로 프로세스 종료도 한 회를 소비하며, 저장 성공 시 재시도 상태를 지운다.
180초 작업 소유권과 Redis 소유자 검사를 함께 사용해 만료된 작업이 새 작업의 재시도 상태를 변경하지 못하게 한다.
Redis 장애 시 유료 호출을 시작하지 않는다. 메시지별 재시도 제한에 더해 아래 누적 번역 예산을 적용한다.
스키마 변경은 없다. 구 버전으로 롤백하면 이 재시도 제한은 적용되지 않는다.

`k8s/app.yaml`과 배포 workflow 모두 답변용 Sonnet 환경변수를 명시한다. 코드를 배포하기 전 운영 모델은 바뀌지 않는다.
기존 스키마 변경은 없지만 새 결과값 OUT_OF_SCOPE를 읽는 백엔드·프론트엔드를 함께 배포해야 한다.
자세한 결정과 롤백 주의점은 [ADR-0007](adr/0007-office-ai-abuse-controls.md)을 참고한다.

최종 로컬 검증: 일반 테스트 692개 통과(유료 LLM opt-in 진입점 2개는 기본 실행에서 제외), PostgreSQL/Redis 통합 테스트 142개 통과. 실제 LLM 평가는 별도 실행한 18개가 모두 통과했다. `bootJar`, 프론트엔드 lint·production build, `git diff --check`도 통과했다.

## 번역 누적 예산 (ADR-0008)

공지·상담 번역은 `ai:{translation}:` Redis 영역을 공유하고 챗봇 답변 예산과 별도로 제한한다.
캐시를 읽을 때는 비용을 예약하지 않는다. 빈 캐시는 실패로 간주하고 새 번역에 성공하면 교체한다.
아래 Spring 설정은 모두 `ovlo.office.` 접두사다. 학교·전체 한도는 먼저 도달한 횟수 또는 가중 토큰 한도로 차단된다.

| 설정 | 기본값 |
|---|---:|
| translation-member-daily-limit | 100 |
| translation-office-daily-limit | 2,000 |
| translation-global-daily-limit | 10,000 |
| translation-member-token-budget | 10,000,000 |
| translation-office-token-budget | 50,000,000 |
| translation-global-token-budget | 250,000,000 |
| translation-office-concurrent-limit | 5 |
| translation-global-concurrent-limit | 20 |

회원 동시 호출은 1개이며, 호출당 75,776을 성공·실패 모두 예약한다. 일자는 Redis 시계의 UTC를 따른다.
상담은 상담 학생, 공지는 최초 작성자의 누적 한도를 사용한다. Redis 장애도 유료 호출을 허용하지 않는다.
한도 소진 시 번역은 보류되지만 상담 원문은 계속 조회할 수 있다. 정확한 통화 청구 원장으로 사용하지 않는다.
결정과 제한 사항: [ADR-0008](adr/0008-auth-session-and-translation-admission.md).

공지 번역은 [ADR-0009](adr/0009-notice-translation-integrity-and-recovery.md)에 따라 Opus 생성 후 Sonnet 의미 검증을 거친다.
언어별 최대 2회 호출에 해당하는 151,552 가중 토큰을 예약하고 슬롯은 300초 유지한다. 일일 횟수는 번역 시도 단위다.
숫자·연락처 및 의미 검사를 통과한 번역만 저장한다. V48 이전 MACHINE 번역은 재검증 전까지 학생 표시·챗봇 근거에서 제외된다.
동시 호출 제한은 30초 뒤, 일일 한도는 다음 UTC 날짜에 DB 작업으로 재시도한다. 유료 시도는 원문 버전·언어별 최대 3회,
작업 수명은 48시간이다. 검증 거절은 자동 반복하지 않으며 담당자의 수동 재번역도 60초 대기와 동일 횟수 한도를 적용한다.
