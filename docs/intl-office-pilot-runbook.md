# 국제처 파일럿 운영 런북 (S6)

> 설계: [intl-office-pilot-design.md](intl-office-pilot-design.md) · 권한: [ADR-0005](adr/0005-office-tenant-authorization.md) · 검색: [ADR-0006](adr/0006-grounded-answer-without-vector-db.md)
> 대상: 대학 국제처 1곳과 파일럿을 시작하는 사람. 순서대로 따라가면 "자료 등록 → 학생 질문 → 상담 이관 → 대시보드" 까지 한 번에 확인된다.

---

## 1. 운영 설정

### 1.1 환경 변수

| 변수 | 운영값 | 설명 |
|---|---|---|
| `OVLO_LLM_PROVIDER` | `anthropic` | `fake` 면 prod 기동이 막힌다(`OfficePilotConfigGuard`) |
| `ANTHROPIC_API_KEY` | 시크릿 `ovlo-secret/ANTHROPIC_API_KEY` | 비어 있으면 기동 실패 |
| `OVLO_LLM_MODEL` | `claude-opus-5-5` | 답변·번역 모두 같은 모델 |
| `OVLO_LLM_EFFORT` | `medium` | thinking 은 항상 켜져 있고 effort 로만 조절 |
| `OVLO_LLM_MAX_TOKENS` | `2048` | |
| `OVLO_LLM_TIMEOUT_SECONDS` | `90` | |
| `OVLO_SEARCH_PROVIDER` | `postgres` | `keyword` 면 prod 기동이 막힌다 |
| `OVLO_INQUIRY_OFFICE_DAILY_LIMIT` | `500` | Office 당 하루 질문 상한(LLM 비용 상한). 넘으면 429 |
| `RATE_LIMIT_INQUIRY_PER_MINUTE` / `_PER_DAY` | `5` / `50` | 회원당 질문 상한 |
| `OVLO_NOTICE_EXPIRY_WARN_DAYS` | `7` | 만료 임박 알림·대시보드 기준 |
| `OVLO_NOTICE_EXPIRY_CRON` | `0 10 0 * * *` | 만료 처리(매일 00:10 KST 가 아니라 **서버 시간**) |
| `OVLO_CONSULTATION_AUTO_CLOSE_CRON` | `0 20 0 * * *` | 해결 7일 뒤 자동 종료 |
| `OVLO_OFFICE_ALLOW_DEGRADED` | `false` | `true` 면 위 검사를 경고로 낮춘다. 장애 시 임시로만 |
| `SPRING_MAIL_HOST` 등 4개 | 시크릿 | 없으면 초대·만료 알림이 로그로만 남는다(`LogOfficeNotificationAdapter`) |

`k8s/app.yaml` 에 `OVLO_*` 와 `ANTHROPIC_API_KEY` 가 들어 있다. **이 앱의 CI 는 `kubectl set image` 형이라 매니페스트를 apply 하지 않는다** — env 를 바꾸면 `kubectl diff -f k8s/app.yaml` 로 `-` 줄을 확인한 뒤 수동 apply 한다(GLOBAL-PIT-083).

### 1.2 시크릿 넣기

```bash
ssh lenovo 'sudo kubectl -n ovlo create secret generic ovlo-secret --dry-run=client -o yaml --from-literal=ANTHROPIC_API_KEY=... ' # 기존 키를 보존하려면 patch 를 쓴다:
ssh lenovo "sudo kubectl -n ovlo patch secret ovlo-secret -p '{\"stringData\":{\"ANTHROPIC_API_KEY\":\"<키>\"}}'"
ssh lenovo 'sudo kubectl -n ovlo get secret ovlo-secret -o jsonpath="{.data.ANTHROPIC_API_KEY}" | base64 -d | wc -c'   # 바이트 길이로 확인(GLOBAL-PIT-160)
```

### 1.3 마이그레이션

V34~V38 이 Flyway 로 올라간다. 배포 뒤 확인:

```bash
ssh lenovo 'sudo kubectl -n ovlo exec deploy/postgres -- psql -U ovlo -c "select version, description, success from flyway_schema_history order by installed_rank desc limit 6"'
```

`office_notice_search_vector` 같은 GIN 인덱스는 V35 에 있으므로 `OVLO_SEARCH_PROVIDER=postgres` 가 바로 동작한다.

---

## 2. 기동 확인

```bash
ssh lenovo 'sudo kubectl -n ovlo logs deploy/app --since=5m | grep -E "\[office\]|\[LLM\] Anthropic|\[seed\]|Started"'
```

기대 로그:
- `[office] 파일럿 설정 확인: llm=anthropic search=postgres`
- `[LLM] Anthropic adapter model=claude-opus-5-5 effort=medium ...`

`국제처 파일럿 운영 설정이 잘못됐습니다` 로 죽으면 1.1 을 다시 본다. 조용히 뜨면서 fake 답을 내는 상태는 없다.

---

## 3. 파일럿 Office 만들기

### 3.1 시드로 (권장)

1. 대학 id 를 `/admin/universities` 에서 확인한다.
2. 국제처 관리자가 될 사람이 **먼저 회원 가입**한다(학교 이메일 인증까지는 불필요).
3. 아래 env 를 켜고 재기동 → 로그에 `[seed] Office 생성 ...` → **바로 끈다**(멱등이지만 켜둘 이유가 없다).

```
OVLO_PILOT_SEED_ENABLED=true
OVLO_PILOT_SEED_UNIVERSITY_ID=<id>
OVLO_PILOT_SEED_OFFICE_NAME=○○대학교 국제처
OVLO_PILOT_SEED_ADMIN_EMAIL=<가입한 이메일>
```

만들어지는 것: Office(PILOT) · 관리자 초대(INVITED) · `[예시]` 승인 자료 3건(번역은 커밋 뒤 비동기) · 제휴 4건(**꺼짐**, example.com).
관리자는 로그인 뒤 `/my-university` 에서 초대를 수락하고 **다시 로그인**해야 담당자 화면이 열린다(JWT 클레임).

### 3.2 수동으로

`/admin/offices` 에서 만들기 → 이메일로 OFFICE_ADMIN 초대. 자료는 담당자가 `/office/notices` 에서 등록·승인.

---

## 4. 스모크 테스트 (배포마다)

토큰은 로그인 응답의 `accessToken`. `$H` = `-H "Authorization: Bearer $TOKEN"`.

| # | 확인 | 명령 / 화면 | 기대 |
|---|---|---|---|
| 1 | 담당자 클레임 | `GET /api/v1/office/me` | `role: OFFICE_ADMIN`, `officeId` |
| 2 | 자료 등록·승인 | `/office/notices` 새 자료 → 승인 | 상태 APPROVED, 1~2분 뒤 번역 6개 언어 채워짐 |
| 3 | 번역 실패 알림 | 번역 중 하나를 "재번역" | 실패하면 담당자 메일/로그에 `sendNoticeTranslationFailed` |
| 4 | 학생 질문(근거 있음) | `/my-university/ask` 에 승인 자료 내용 질문 | ANSWERED, `[1]` 인용, 출처 링크 |
| 5 | 학생 질문(근거 없음) | 자료에 없는 질문 | NO_GROUNDS + "담당자에게 전달" 버튼, 추측 답 없음 |
| 6 | 상담 이관 | 동의 → 전달 | `/office/consultations` 받은함에 OPEN, 담당자 메일 |
| 7 | 메시지 번역 | 학생이 베트남어로 채팅 | 담당자 화면에 한국어 번역 병기 |
| 8 | 대시보드 | `/office` | 질문 수·답변률·근거 없음 목록 반영 |
| 9 | CSV | 대시보드 내보내기 3종 | BOM 포함, 엑셀에서 한글 정상, **회원 id·이메일 열 없음** |
| 10 | 제휴 | `/admin/partners` 켜기 → `/office/settings` 분류 켜기 → `/my-university/arrival` | 카드에 "제휴 · 수수료" 표시, 클릭 후 대시보드 클릭 수 +1 |
| 11 | 테넌트 경계 | 다른 Office id 로 `GET /api/v1/office-admin/{other}/notices` | 403 `OFFICE_FORBIDDEN` |
| 12 | 상한 | 같은 회원이 1분에 6번 질문 | 6번째 429 |

보안 회귀는 코드로 고정돼 있다: `./gradlew test --tests 'me.yeonjae.ovlo.adapter.in.web.Office*SecurityTest'`.

---

## 5. 메트릭·로그

`/actuator/prometheus` 는 **ADMIN 권한**이 있어야 열린다(SecurityConfig `/actuator/**`). 홈랩 Prometheus 가 긁으려면 bearer 토큰이나 파드 네트워크 예외가 필요하다 — 파일럿 중엔 아래 PromQL 대신 `/actuator/metrics/ovlo.office.inquiry` 를 관리자 세션으로 보는 것으로 충분하다.

| 메트릭 | 태그 | 보는 법 |
|---|---|---|
| `ovlo_office_inquiry_total` | outcome | 답변률 = ANSWERED / 전체 |
| `ovlo_office_inquiry_latency_seconds` | outcome | p95 가 10초를 넘으면 effort 를 `low` 로 |
| `ovlo_office_llm_calls_total` | purpose, result, model | `result=error` 증가 = 키·한도·네트워크 |
| `ovlo_office_llm_tokens_total` | purpose, direction, model | 비용 추정: 입력·출력 토큰 × 단가 |
| `ovlo_office_consultation_total` | action | opened 대비 resolved |
| `ovlo_office_partner_click_total` | category | |

로그 필터(Loki): `{namespace="ovlo"} |= "[LLM]"` — 호출마다 `model= stop= in= out= ms=` 한 줄. `stop=refusal` 이 반복되면 프롬프트·자료를 본다.

비용 상한: Office 당 하루 500 질문 × (입력 ~3k + 출력 ~0.5k 토큰). 단가는 `claude-api` 스킬로 그때 확인한다. 초과 전에 `OVLO_INQUIRY_OFFICE_DAILY_LIMIT` 을 줄인다.

---

## 6. 일상 운영

- **매일**: 대시보드 "근거 없음 반복 문의" → 자료 1건 등록. 만료 임박 목록 확인.
- **매주**: CSV 내보내기 보관(학생 식별 없음). `ovlo_office_llm_calls_total{result="error"}` 0 인지.
- **학기 시작 2주 전**: 지난 학기 자료 `term` 갱신 또는 보관. 제휴 링크 유효성(404) 확인.
- **담당자 퇴직**: `/office` 담당자 제거 → 진행 중 상담 채팅방에서 자동으로 빠진다. 마지막 OFFICE_ADMIN 은 제거할 수 없으니 먼저 다른 관리자를 올린다.

---

## 7. 장애 대응

| 증상 | 원인 후보 | 조치 |
|---|---|---|
| 질문이 전부 NO_GROUNDS | 자료 미승인·만료·언어 번역 없음(EN 질문인데 EN 번역 없음) | 자료 상태·`missingLangs` 확인, 재번역 |
| 질문 500 / `[LLM] answer failed` | 키 만료·한도·타임아웃 | 키 교체(1.2) → 재기동. 임시로 `OVLO_LLM_EFFORT=low` |
| 번역이 안 채워짐 | 비동기 리스너 예외 | 로그 `NoticeTranslationEventListener`, "재번역" 버튼 |
| 담당자가 403 | 초대 수락 뒤 재로그인 안 함, Office SUSPENDED | 재로그인 / `/admin/offices` 상태 |
| 학생이 403 | 그 대학 활성 인증 자격 없음 | `/admin/verifications`, 학교 이메일 재인증 |
| 기동 실패 `운영 설정이 잘못됐습니다` | env 누락 | 1.1. 급하면 `OVLO_OFFICE_ALLOW_DEGRADED=true` 로 띄우되 fake 답이 나간다는 뜻이므로 질문 화면을 닫을 것 |

롤백: 이전 이미지로 `kubectl rollout undo deploy/app -n ovlo`. V34~V38 은 추가 전용(테이블·컬럼 추가)이라 구 이미지도 뜬다.

---

## 8. 파일럿 종료 시 확인할 숫자 (제안서 §3 KPI)

| KPI | 출처 |
|---|---|
| 자동 안내 종결 비율 | 대시보드 답변률 (목표 60%+) |
| 담당자 첫 응답 시간 | 대시보드 평균 첫 답변 |
| 반복 문의 감소 | 주차별 "반복 문의" 상위 10 의 건수 추이 (CSV) |
| 학생 만족 | 도움됨 비율 |
| 제휴 전환 | 클릭 수 → 제휴사 CPA 정산 수치와 대조 |

---

## 9. 아직 안 한 것 (설계 §12 열린 질문)

- 국제처가 명단으로 학생을 **일괄 수동 인증**하는 기능 — 신입생 학교 이메일 지연 대비. 지금은 `/admin/verifications` 단건.
- 반복 문의 2차 묶기(LLM 카테고리) — 1차 정규화 묶기만 있다.
- Prometheus 스크레이프 경로 — 위 5 절.
- 프론트 타입체크는 이 작업트리에 node_modules 가 없어 돌리지 못했다. 배포 전 `cd frontend && npm ci && npm run build` 로 확인한다.
