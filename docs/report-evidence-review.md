# 리포트 출처 검토

2026-11-24 공개 리포트 읽기 전용 스냅샷: 724개 리포트, 5,068개 번역.
전체에 HTTP(S) 출처와 유효한 최근 기준일이 있었다. 국가 코드 형식 오류도 없었다.
이 결과는 국가 배정이나 개별 문장의 사실성을 전수 확인했다는 뜻이 아니다.

`python3 scripts/audit_report_evidence.py snapshot.jsonl queue.json --as-of 2026-11-24`는
출처 누락·날짜 오류·180일 초과·국가 코드 형식을 검사한다. 국가의 실제 배정은 공식 자료로 별도 대조한다.
입력과 같은 경로에 출력할 수 없고 DB·외부 사이트에는 연결하지 않는다.
비용·비자·기숙사·입학 조건의 번역 필드 42,231개를 검토 대상으로 추출했다.
번역별 중복이 포함된 수이며, `UNREVIEWED`는 **이번 감사에서 아직 검토하지 않았음**을 뜻한다.
기존 재생성 과정의 검토 기록을 취소하거나 덮어쓰지 않는다.

자료를 반영할 때는 기존 `scripts/report_regen/check_report.py`의 출처·날짜·언어·통화·숫자 검증을
통과하고, 공식 근거의 적용 학기·대상·추천과 지원 구분을 확인한다. 출처 URL의 존재만으로 자동 승인하지 않는다.
비용·비자·자격·마감처럼 변동이 큰 정보는 사용자가 연결된 원문을 확인하도록 안내한다.

## 공식 자료 표본 대조

영문 요약 필드를 대상으로 6개 학교를 확인했다. 검토일은 모두 2026-11-24다.
아래 `일치`는 기재한 범위에만 적용한다. 원문의 장래 변경, 다른 학기·언어·본문 전체를 보증하지 않는다.
원자료를 자동 수정하거나 보고서 전체에 검증 완료 배지를 부여하지 않았다.

| 학교 / 리포트 ID | 필드 | 판정·적용 범위 | 공식 근거 |
|---|---|---|---|
| Barcelona / 1 | academics.deadline | 일치. 2026–27 2학기 지원 마감은 학부에 따라 10월 30일 또는 11월 13일. 추천 마감과 구분 | [UB 일정](https://www.ub.edu/mobilitat-internacional/en/node/309/deadlines-and-calendar) |
| Barcelona / 1 | costs.monthly_total, costs.rent | 일치. 월 1,300–1,500유로, 공동 주택 방 450–650유로는 **UPC의 바르셀로나 추산**이라는 조건을 유지 | [UPC 비용 추산](https://www.upc.edu/sri/en/mobility_office/students-mobility-office/incomings/studying-at-the-upc/copy_of_cost-of-living-and-prices) |
| Stanford / 33 | housing.dorm_price, academics.deadline | 확인 보류. 공식 페이지가 403을 반환해 2026 여름 비용·2027 I-20 마감의 본문을 대조하지 못함 | [비용](https://summer.stanford.edu/cost/tuition-fees-calculator), [일정](https://summer.stanford.edu/applying/dates-and-deadlines) |
| NTU / 59 | academics.deadline | 일치. 1학기 3월 30일·2학기 9월 30일은 지원 마감이며, 추천은 각각 15일까지. 날짜 변경 가능 | [교환학생 절차](https://www.ntu.edu.sg/education/student-exchanges/exchange-programmes/inbound-programmes) |
| NTU / 59 | housing.dorm_price | 확인 보류. 해당 기숙사 페이지 조회가 시간 초과되어 2026/27 객실별 요금을 대조하지 못함 | [Hall 2](https://www.ntu.edu.sg/life-at-ntu/accommodation/undergraduate-housing/detail/hall-2) |
| TUM / 70 | housing.dorm_price | 일치. 월 400–600유로·보증금 500–900유로는 해당 학생 기숙사 조건이며 모든 민간 주택 가격이 아님 | [TUM 숙소](https://www.global.tum.de/en/global/accommodation/) |
| TUM / 70 | academics.deadline | 일치. 겨울 학기 5월 10일·여름 학기 10월 31일 | [TUM 교환학생 일정](https://www.global.tum.de/en/global/exchangestudents/general-information-for-international-students/) |
| Tohoku / 143 | academics.deadline, academics.language_req | 일치. 가을 1월 10일–2월 20일·봄 10월 1일–11월 15일. 영어 과정 TOEFL iBT 79 상당, DEEP는 JLPT N1이라는 프로그램 구분 유지 | [Tohoku 교환학생 안내](https://www.insc.tohoku.ac.jp/english/exchange/) |
| SKKU / 266 | academics.deadline | 부분 확인. 2027 봄 추천 10월 15일은 일치. 실제 지원은 10월 30일까지이며, 이 공지는 가을 추천 4월 15일을 검증하지 않음 | [SKKU 2027 봄 안내](https://www.skku.edu/eng/International/StudySKKU/BulletinBoard.do?mode=view&articleNo=140159&article.offset=0&articleLimit=10&no=77) |

확인 보류 항목을 다음 검토의 우선 대상으로 삼는다. 근거를 확보한 뒤에는 리포트 ID·언어·필드·원래 값·
새 값·공식 URL·확인일·적용 학기를 함께 기록하고, 기존 재생성 검증과 백업 절차를 거쳐 반영한다.
접속 실패를 사실 오류나 정보 부재로 취급하지 않는다.
