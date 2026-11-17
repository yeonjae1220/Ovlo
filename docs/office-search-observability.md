# 학교 챗봇 검색 최소 계측

[위키 검토 절차 OV-FEAT-017](https://github.com/yeonjae1220/llm-wiki/blob/b41cd3c5a771dabc36777ae8afbd0612a06f6b84/Projects/Ovlo/features/office-search-review-procedure.md)의 미구현 항목을
`origin/main@e600eb4`와 대조했다. S-04·S-05는 보류 상태로 유지하고, 조사할 검색 경로와 후속 질문의 후보 유무만 먼저 관측한다.
계획 §1 숫자는 재조회하지 않았다. 운영 질문 표본 수집, 검색 보정, 검색어 재작성, 추가 LLM 호출은 수행하지 않는다.

## 구현 범위

| 미구현 항목 | 이번 처리 | 이유 |
|---|---|---|
| S-03 전체 후보 사용 / 일반 검색 / 자격 자료 없음 | 구현 | 기존 분기 결과만 기록하여 별도 조회나 본문 처리가 없다. |
| 단발 / 후속 검색의 후보 유무·오류 | 구현 | 이미 권한 검증을 마친 history의 존재 여부와 검색 단계 결과만 사용한다. |
| 자격 공지 수·전체 본문 바이트 구간 | 보류 | 첫 단계는 기존 분기만으로 자료 없음·전체 후보·일반 검색을 구분한다. 추가 계산·세분화는 필요성이 입증된 뒤 검토한다. |
| 언어·학교별 coverage 및 후속×검색 경로 교차 집계 | 보류 | 작은 집단의 세분화를 최소화하고 식별자·새 상관관계 키를 만들지 않는다. |
| 정답 포함률·실패 유형·recall@8 | 자동 계측하지 않음 | 정답 자격과 개인정보 검토를 거친 합성 표본 평가가 필요하다. 후보 존재는 정답 존재가 아니다. |

API 응답·저장 스키마·공지 자격·검색 순서·후보 상한·LLM 요청은 그대로다. 기존 결과 counter의 태그도 바꾸지 않는다.
새 counter는 집계 관측이며 저장 성공이나 답변 성공을 뜻하지 않는다.

## 지표 정의

### `ovlo_office_search_completed_total`

Micrometer 이름: `ovlo.office.search.completed`.
검색 어댑터의 **정상 반환 한 번**마다 증가한다. 어댑터에서 예외로 끝나면 증가하지 않는다.

| 태그 | 허용값 | 의미 |
|---|---|---|
| `provider` | `KEYWORD`, `POSTGRES` | 최상위 호출을 받은 어댑터. PostgreSQL 내부 CJK 보조 검색은 별도로 집계하지 않는다. |
| `path` | `NO_ELIGIBLE` | S-03 자격 판정 후 공지가 없는 정상 반환. 학교에 저장된 공지 자체가 없다는 뜻은 아니다. |
| `path` | `SMALL_CORPUS` | 자격 있는 전체 공지가 candidateLimit 및 30,000 UTF-8 바이트 안에 들어가 전부 반환하는 분기. |
| `path` | `RANKED` | S-03 우회가 적용되지 않아 기존 검색을 실행한 분기. 검색어 토큰이 없어서 빈 결과를 반환하는 경우도 포함한다. |
| `candidates` | `EMPTY`, `PRESENT` | 어댑터 반환 후보의 존재 여부. 서비스의 최종 자격 재검사 전이다. |

경로는 최초 분기 판단 기준이다. 검색 도중 자료가 바뀌거나 PostgreSQL 내부 보조 검색이 다른 상태를 읽어도 상위 경로를 재분류하지 않는다.
운영의 양수 candidateLimit(기본 8)을 전제로 해석한다. `NO_ELIGIBLE`과 `SMALL_CORPUS`가 건수/바이트 등 정확한 자료 규모를 알려주지는 않는다.

### `ovlo_office_inquiry_retrieval_total`

Micrometer 이름: `ovlo.office.inquiry.retrieval`.
유효한 문의가 권한·history·입장 제한·범위 가드를 통과하여 **검색 단계에 들어온 뒤 한 번** 증가한다.

| 태그 | 허용값 | 의미 |
|---|---|---|
| `query_kind` | `SINGLE`, `FOLLOW_UP` | 검증된 저장 history가 없거나 있는지. 후속의 이전 질문이 실제 검색어에 얼마나 쓰였는지, 독립 대화/학생 수인지는 나타내지 않는다. |
| `result` | `EMPTY`, `PRESENT` | 어댑터 호출 후 서비스의 최종 자격 필터와 후보 상한을 적용한 결과. |
| `result` | `ERROR` | 검색 호출 또는 최종 자격 필터 중 RuntimeException. 원래 예외는 그대로 전파하며 빈 결과나 정상 답변으로 바꾸지 않는다. |

권한·history 오류, 입장 제한, 범위 가드 거절/오류처럼 검색 이전에 끝난 요청은 제외한다.
뒤의 답변 생성·근거 검증·저장 실패는 검색 결과를 ERROR로 다시 분류하지 않는다.
따라서 이 지표와 `ovlo_office_inquiry_total`(저장 결과 관측)의 분모는 다르며, 성공 커밋 수·현재 DB 행 수와 같다고 가정하지 않는다.
두 새 counter도 관측 위치와 호출자가 다르므로 조인하거나 차감해서 개별 요청의 자격 탈락을 추론하지 않는다.

## 개인정보·수집 경계

- 새 API는 enum 또는 boolean만 받아 태그를 만든다. 원문, 검색어, 답변, URL, 학생/학교/문의/공지 ID, 해시 ID,
  언어, 요청 시각, 본문 길이, 예외 메시지·클래스는 태그로 받지 않는다. 새 로그·DB 컬럼·원문 저장소도 없다.
- 기본 태그 조합은 어댑터 12개 + 문의 검색 6개로 고정한다. 프로세스 시작 시 0으로 등록하고 재시작하면 초기화된다.
  모든 조합을 미리 등록하므로 정상 경로에서 불가능한 조합도 0으로 존재한다. 미사용 provider도 0이다.
  수집 시스템이 추가하는 환경/인스턴스 공통 라벨은 이 수에 포함하지 않는다.
- 수집 범위는 기존 인증된 모니터링 경계다. 공개 API나 담당자 화면에 새 지표를 노출하지 않는다.
  모니터링 원시 시계열의 시간·인스턴스 정보까지 외부 공유해도 된다는 의미는 아니다.
- 개인정보 삭제·만료 뒤에도 이 집계는 특정 문의를 가리키지 않는다. 과거 문의/질문을 지표에 소급 적재하거나
  삭제된 후보 ID·피드백을 복원하지 않는다. 상담 이관 동의를 평가용 원문 사용 근거로 확대하지 않는다.

## 운영 수집 연결

#100 배포 후 운영 설정을 확인한 결과, `/actuator/prometheus` 노출만으로 수집되는 구조가 아니었다.
Prometheus는 OTEL Collector를 scrape하며 Collector의 metrics 입력은 OTLP뿐이고, 앱에는 OTLP registry/Java agent와
Collector 방향 egress 허용이 없었다. 기존 통계나 질문은 조회하지 않고 배포·수집 설정으로 이 차이를 확인했다.

- `SearchMetricsExportConfig`가 위 두 counter만 내부 Collector의 OTLP/HTTP 4318 포트로 30초마다 누적 전송한다.
  지표 이름·COUNTER 타입·태그 키 집합·각 값 모두 허용 목록과 일치해야 한다. 다른 지표나 추가 태그는 거부한다.
- Boot의 범용 OTLP 자동 전송은 꺼 두고 기존 Prometheus registry는 유지한다. prod 프로필에서 전용 registry가 켜진다.
  `OVLO_SEARCH_METRICS_EXPORT_ENABLED=false`로 다음 배포 시 전용 전송만 중단할 수 있다. 로컬/test 기본은 꺼짐이다.
- resource는 서비스명 `ovlo-search-metrics`, namespace `ovlo`, 인프라 인스턴스명(파드명, 미지정 시 프로세스 UUID)으로 제한한다.
  SDK가 붙이는 언어·버전 등의 기술 메타데이터와 집계 시계열 시각은 포함된다. 학생·학교·문의 식별자는 포함하지 않는다.
  trace/span exemplar 공급자를 연결하지 않으며 환경의 임의 OTLP header/resource 값을 상속하지 않는다.
- 앱 egress는 `monitoring` namespace의 `app=otel-collector` 파드 TCP 4318만 추가한다.
  공개 actuator 접근 권한·Collector 설정·다른 지표 수집 경로는 변경하지 않는다.
- 전송은 비동기이며 전송/수집 장애·재시작 전 미전송분의 유실이 가능하다. 전송 시각은 개별 문의 시각이 아니며,
  배포 후 두 이름의 시계열 존재와 수집 상태를 확인해야 한다. 정상 등록된 0과 시계열 부재를 구분한다.

2026-11-17 `469088a` 배포 뒤, 운영 Collector의 Prometheus exporter가 `namespace: app`을 적용하는 것을 확인했다.
위의 접두사 없는 이름은 앱의 Prometheus endpoint 기준이며, **현재 운영 Prometheus 조회 이름은**
`app_ovlo_office_search_completed_total`, `app_ovlo_office_inquiry_retrieval_total`이다.
Collector의 resource processor는 `service.namespace`를 `k3s-cluster`로 바꾼다. 운영 selector에 `service_namespace="ovlo"`를 쓰지 않는다.
`service_name="ovlo-search-metrics"`와 승인된 모니터링 환경 selector를 사용하고, `service_instance_id`/`exported_instance`는 인프라 파드 식별자로만 해석한다.
이 수집 설정은 변경하지 않았다. 새 지표 두 개만 조회하여 검색 12개·문의 검색 6개 조합의 수집을 확인했으며, 기존 §1 통계/질문 조회나 검증용 운영 질문 생성은 하지 않았다.

구현 근거: [Micrometer OTLP registry](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html).
로컬 수신기로 실제 protobuf 전송을 파싱해 허용 지표와 빈 exemplar를 검증하고, prod 설정 로딩으로 registry 중복·필터 범위를 확인한다.

## 해석과 검토 예시

관찰 기간 W·시간대·배포 SHA·provider·수집 누락을 함께 기록한다. 다음은 **미실행 예시**이고 7일은 필수 관찰 기간이 아니다.
승인된 모니터링 환경 selector를 붙이고 인스턴스별 increase를 먼저 계산한 뒤 합산한다. scrape 누락/재시작 사이 손실은 복원되지 않는다.
운영 합성 검증도 같은 경로를 거치면 집계된다. 검증 시간대를 기록하고 실사용 관찰 기간과 분리한다.
이 지표에는 계정 구분 태그가 없으므로 합성 요청만 사후에 골라 차감하거나 질문/계정 조회로 연결하지 않는다.

```promql
sum by (provider, path, candidates) (increase(app_ovlo_office_search_completed_total{service_name="ovlo-search-metrics"}[7d]))
sum by (query_kind, result) (increase(app_ovlo_office_inquiry_retrieval_total{service_name="ovlo-search-metrics"}[7d]))
```

- S-03 사용 비중: 같은 provider·W에서 SMALL_CORPUS / 정상 완료 검색 전체. NO_ELIGIBLE은 별도 비중으로 표시한다.
- 일반 검색의 빈 후보 비중: 같은 provider·W에서 RANKED+EMPTY / RANKED 전체. 이것은 정답 누락률이 아니다.
- 후속 빈 후보 비중: FOLLOW_UP+EMPTY / FOLLOW_UP의 (EMPTY+PRESENT). ERROR는 성공 분모에서 제외하고 건수와
  ERROR / FOLLOW_UP 전체 비중을 함께 기록한다. 단발도 같은 정의로 비교한다.
- 분모가 0이면 N/A이며 비율과 함께 분자·분모를 표시한다. 원값 단순 차감이나 0/시계열 부재의 혼동을 피한다.
- 같은 학생의 반복 질문·언어·학교 편향을 제거할 수 없다. 작은 표본의 증가나 한 기간의 비율로 재착수를 자동 승인하지 않는다.

S-04는 자격 있는 정답 자료가 있었던 실사용 실패를 승인된 합성 표본으로 동등 재현하고, 해당 어휘 보정의 이득과 기존 성공 무회귀를 확인해야 한다.
S-05는 같은 앞 질문 정보로 후속 baseline 미달과 재작성 이득을 보여야 한다. 추가 호출은 예약 토큰·ADR-0007·개인정보/비용 경계 검토가 별도로 필요하다.
후보가 있어도 정답이 없거나 앞 3,000자 밖에 있을 수 있으므로 이 계측만으로 보류를 해제하지 않는다.

## 검증

- 일반 테스트: 단발/후속·빈 후보/정상/오류, 만료 후보 재필터, 검색 전 차단, 모델 단계 실패의 분리, 고정 태그·Prometheus 출력 이름.
- 어댑터 테스트: 자료 자격·S-03 경계·건수/바이트 초과 검색·빈 토큰·CJK 내부 호출 중복 방지.
- 실제 PostgreSQL 골든셋: 70개 검색이 최상위 POSTGRES 관측 70개이며 내부 KEYWORD 관측은 0개. 두 어댑터 기존 0.8 및 성공 56개 하한 유지.
- 합성 fixture만 사용하며 추가 유료 LLM 호출이나 운영 학생 질문 조회는 없다.
