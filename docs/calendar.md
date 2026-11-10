# 학교·개인 캘린더 1차

## 이용 경로

- `/register`: 일반 회원 / 학교 담당자 가입 선택.
- `/register/member`: 기존 학생 회원 가입.
- `/register/university`: 담당자 계정 생성과 신청을 한 트랜잭션으로 처리. 전공·학년을 요구하지 않는다.
- `/university-account`: 로그인한 기존 회원의 담당자 신청, 승인·반려·해제 상태 및 사유 확인.
- `/calendar`: 학교 구독, 월간 달력, 모바일 일정 목록, 개인 일정 추가·수정·삭제.
- `/calendar/manage`: 담당 학교의 초안 작성·수정·게시·행사 취소·초안 삭제.
- `/admin/calendar`: 기존 관리자 세션으로 담당자 심사·권한 해제, 학교 검색 및 공식 출처 기반 대리 등록.

가입은 모두 MEMBER로 시작한다. ADMIN이 학교 공식 연락처·직원 안내 등을 확인하고 신청을 승인하면 UNIV 역할과 해당 학교 배정이 함께 저장된다. 회원 관리 화면에서도 MEMBER / UNIV / ADMIN 역할을 선택할 수 있으나, **UNIV 역할만으로는 어느 학교에도 작성 권한이 생기지 않는다**. 배정은 담당자 신청 승인으로 수행한다. 마지막 학교의 권한을 해제해도 계정 역할은 UNIV로 유지하며, 배정이 없으므로 일정 관리 권한은 없다. 필요하면 회원 관리 화면에서 MEMBER로 변경한다.

## 권한과 공개 범위

- 학교 일정 변경 시 DB의 현재 회원 상태·역할·학교 배정을 매번 확인한다. 강등·정지·배정 해제 후 기존 JWT로도 변경할 수 없다.
- 학교 프로필·학교 이메일·학교 구독은 담당자 권한을 부여하지 않는다.
- 학교 일정은 로그인 회원에게 공개되는 공지로 한정한다. 학교를 구독하면 게시 및 취소 상태의 일정이 보인다. 초안은 해당 학교 담당자와 운영진만 조회한다.
- 개인 일정은 소유 회원 조건으로 조회·변경·삭제한다. ADMIN도 다른 회원의 개인 일정을 볼 수 없다.
- 기존 `/admin/**` 세션 인증과 CSRF 보호를 그대로 사용한다. JWT UNIV로 관리자 화면에 접근할 수 없다.
- Google 로그인은 UNIV를 토큰에 반영하며, 기존 Google 로그인 시 ADMIN을 발급하지 않는 정책을 유지한다.

## 모델과 날짜

`calendar` 도메인의 `UniversityEvent`, `PersonalEvent`, `UniversityApplication`은 식별자와 생명주기가 있는 엔티티다. `EventPeriod`, `EventDetails`, `EventSource`는 불변 record 값 객체다. 도메인은 Spring/JPA에 의존하지 않으며 서비스는 입출력 포트로 저장소에 접근한다.

학교/개인 일정은 별도 테이블에 저장하고 조회 시 합친다. 학교 일정 원본을 학생별로 복사하지 않으므로 수정·취소가 다음 조회에 반영된다.

- 종일 일정: `LocalDate startDate/endDate`; **endDate는 미포함**. 화면은 마지막 날을 포함해서 입력받고 API 전송 시 하루를 더한다.
- 시간 지정 일정: `Instant startsAt/endsAt`와 IANA `timeZone`. 종료 시각 미포함.
- `deadlineAt`은 행사 종료와 별개인 선택 마감 시각이다.
- 종일 일정은 표시 시간대 변경으로 날짜가 이동하지 않는다. 시간 지정 일정의 달력 배치는 사용자 시간대를 사용하고 상세에는 등록 시간대를 표시한다.
- 입력한 현지 시각이 서머타임 전환으로 존재하지 않거나 두 번 나타나면 조용히 보정하지 않고 거부한다.
- 기간 조회는 겹치는 일정도 포함하며 최대 93일, 통합 결과 최대 500건이다. 초과 시 기간을 줄이라는 오류를 반환한다.
- 학교 구독은 계정당 최대 20개를 기본 제한으로 둔다.

## 출처와 이력

- UNIV 생성: `UNIVERSITY`, 학교 담당자 등록.
- ADMIN 생성: `OVLO_CURATED`, Ovlo 등록·학교 공식 공지 참고. HTTP(S) 원문 링크와 출처 확인일 필수.
- 등록 경로와 최초 작성자는 서버가 설정하고 수정 시 보존한다. 학교 담당자가 운영진 등록 일정을 이어서 수정해도 최초 등록 경로는 바뀌지 않는다.
- URL은 링크로만 사용하며 서버에서 해당 주소를 가져오지 않는다. 공식성은 운영진이 원문을 확인해 판단한다.
- 관리자 화면에서 출처 확인일이 14일보다 오래된 일정에 재확인 안내를 표시한다. 자동 수집이나 자동 갱신 기능은 없다.
- 일정 변경·게시·취소·초안 삭제 및 담당자 심사는 `calendar_audit`에 같은 트랜잭션으로 기록한다. 개인 일정 내용은 이 운영 감사 테이블에 복사하지 않는다.
- 관리자 세션 작성의 `created_by/updated_by=0`은 비회원 운영 계정을 뜻한다. 실제 관리자 식별자는 감사 행의 `actor=admin:<로그인명>`에 남는다.
- 편집/상태 변경/삭제에는 `version`이 필요하며 이전 버전은 409로 거부한다.

## API

| 경로 | 용도 |
|---|---|
| `POST /api/v1/members/university-registration` | 공개 담당자 가입·신청, 기존 가입 rate limit 적용 |
| `GET /api/v1/calendar?from=&to=&timeZone=` | 구독 학교 + 본인 일정 |
| `GET /api/v1/calendar/options` | 현재 역할, 구독 학교, 본교/교환학교 후보, 관리 학교 |
| `PUT/DELETE /api/v1/calendar/subscriptions/{schoolId}` | 학교 구독·해제 |
| `POST /api/v1/calendar/personal` | 개인 일정 추가 |
| `PUT/DELETE /api/v1/calendar/personal/{id}` | 개인 일정 수정·삭제 |
| `GET/POST /api/v1/calendar/schools/{schoolId}/events` | 담당 학교 조회·초안 생성 |
| `PUT/DELETE /api/v1/calendar/school-events/{id}` | 학교 일정 수정·초안 삭제 |
| `POST /api/v1/calendar/school-events/{id}/status` | `publish` / `cancel` |
| `GET/POST /api/v1/calendar/applications` | 본인 담당자 신청 내역·새 신청 |

모든 `/calendar/**` 요청은 인증이 필요하다. 사용자/역할/출처 유형은 입력 JSON으로 지정할 수 없다. 학교 일정은 제목·분류·기간·장소·설명·대상·신청 링크·별도 마감을 지원한다. 학교 공개 범위를 인증된 구성원으로 제한하는 기능은 포함하지 않는다.

## 검증과 실행

```bash
./gradlew test
./gradlew integrationTest
cd frontend
node scripts/test-calendar.cjs
npx tsc --noEmit
npm run build
```

- `CalendarApiSecurityTest`: 실제 보안 필터 + H2로 가입/승인/격리/취소/버전 충돌/관리자 HTML 렌더링 검증.
- `CalendarPostgresIntegrationTest`: 같은 흐름을 PostgreSQL 16 + 전체 Flyway 마이그레이션으로 검증. Docker 필요.
- `CalendarDomainTest`: 기간·출처·신청 및 일정 상태 전환 규칙.
- `scripts/test-calendar.cjs`: 실제 TypeScript 날짜 유틸리티의 날짜 경계·시간대·DST 테스트.
- `scripts/calendar-smoke.cjs`: 실행 중인 프론트에 Playwright로 접근. API 응답은 독립 fixture로 대체하여 가입 분기, 개인 일정 입력, 출처 표시, 학교 초안→게시, 모바일 너비를 검증한다. `playwright-core` 및 해당 Chromium이 설치된 환경에서 실행하며 `CALENDAR_QA_URL`(기본 `http://127.0.0.1:3108`), `CALENDAR_QA_OUTPUT`으로 대상을 지정할 수 있다. 백엔드 통합 검증을 대체하지 않는다.

마이그레이션은 `V41__calendar_and_university_applications.sql`이다. 기존 데이터를 변경하지 않고 새 테이블·제약·인덱스를 추가한다. UNIV 값을 읽을 수 없는 이전 백엔드가 남아 있을 때 권한을 승인하지 말고 모든 백엔드 인스턴스 업데이트 후 활성화한다. UNIV 계정이 생성된 뒤 구버전으로 롤백할 때는 역할 호환성 처리가 먼저 필요하다.

신규 사용자 화면은 한국어/영어를 제공하며 다른 UI 언어에서는 영어로 표시된다. 기존 일반 회원 가입의 다국어 지원은 유지한다. 푸시 알림, 반복 일정, 외부 캘린더 동기화, 자동 출처 수집, 참가 신청은 후속 범위다.

배포 준비 시 main 및 운영 DB에 V34~V40이 이미 존재함을 확인하여, 초안의 V34를 V41로 재채번했다. 기존 Office 소속 클레임, 관리자 추가 계정, 내 학교 메뉴 및 관련 보안 규칙을 보존한다.
