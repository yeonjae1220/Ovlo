# 국제처 파일럿(1단계) 제품 설계

작성 2026-10-30. 2026-10-30 멘토링 결과(커뮤니티는 수익원이 아니다, 1단계는 대학 국제처 B2B SaaS + 정착 제휴)를
제품으로 옮기는 설계다. 2027학년도 봄학기 **국제처 1곳 무료 파일럿**에 필요한 최소 범위를 정하고, 기존 코드에서
무엇을 재사용하고 무엇을 새로 만드는지, 어떤 순서로 만드는지를 적는다. 사업 구조·경쟁자 비교는
`output/round2-monetization-20261030/`(git 미추적)에 있다.

관련 ADR: [0005 국제처 테넌트 권한 모델](adr/0005-office-tenant-authorization.md) ·
[0006 파일럿 안내 검색은 pgvector 없이 FTS + LLM](adr/0006-grounded-answer-without-vector-db.md)

---

## 1. 목표와 범위

**파일럿이 증명해야 하는 것**

1. 국제처가 올린 승인 자료만 근거로, 학생 모국어로, 출처를 달아 답할 수 있다. 근거가 없으면 답하지 않는다.
2. 사람이 판단할 문의는 학생 동의를 받아 담당자에게 넘어가고, 담당자는 한 화면에서 처리한다.
3. 학기 말에 국제처장이 볼 숫자가 나온다: 자동 안내 종결 비율, 이관 건수·응답 시간, 반복 문의 상위, 만료 갱신.
4. 입국 직후 필수 정착 제휴(유심·보험·숙소·픽업)가 학생 화면에 "제휴" 표시와 함께 노출되고 클릭이 집계된다.

**범위 밖 (1단계에서 만들지 않음)**

- 학적·성적·LMS 연동, 비자 서류 심사, 이탈 예측 (하이어보드·에듀메타·K-EduBridge 영역)
- 중고거래, 교류 매칭, 로컬 큐레이션, 일자리 매칭 (2단계)
- 다중 캠퍼스·부서 계층, SSO(대학 포털 연동), 요금 청구·결제
- 해외 파견 교환학생 리포트 변경 (별도 트랙에서 계속)

**가정** — 파일럿 대학 1곳, 유학생 수백 명, 공지 수십~수백 건, 담당자 1~5명. 설계는 대학 N곳을 전제하되
성능 최적화는 하지 않는다.

---

## 2. 용어

| 용어 | 뜻 |
|---|---|
| Office(국제처) | 대학 1곳의 국제처 테넌트. `global_universities` 1행에 1:1 |
| Staff(담당자) | Office에 속한 회원. OFFICE_ADMIN(설정·초대 가능) / OFFICE_STAFF |
| Notice(승인 자료) | 국제처가 등록하고 승인한 공지·안내문. 적용 학기와 만료일이 있다 |
| Inquiry(문의) | 학생이 던진 질문 1건과 그에 대한 자동 답변 기록 |
| Consultation(상담) | 학생 동의로 담당자에게 넘어간 문의. 채팅방과 상태를 가진다 |
| Partner(제휴) | 정착 제휴 링크. 분류·표시 문구·대학별 on/off |
| 소속 학생 | 그 대학에 **활성 인증 자격**(`verification_credential`)이 있는 회원 |

---

## 3. 기존 코드에서 재사용하는 것

| 필요 | 기존 자산 | 위치 | 비고 |
|---|---|---|---|
| 학생 신원·대학 소속 | `VerificationCredential`(SCHOOL_EMAIL / ADMIN_VERIFIED), `TrustLevel` | `domain/verification` | 소속 판정은 `homeUniversityId`가 아니라 **활성 자격**으로 한다 |
| 담당자가 학생을 수동 인증 | `AdminVerificationService.issueManual` | `application/service/admin` | ADMIN 전용 → 자기 대학 한정으로 OFFICE_ADMIN에게도 연다 |
| 대학 마스터 | `global_universities`, `domain` 컬럼(학교 이메일 도메인) | `domain/university` | Office는 `global_univ_id`를 가리킨다 |
| 담당자↔학생 대화 | `ChatRoom(GROUP)`, STOMP `/topic/chat/{id}`, 구독 멤버십 검사 | `domain/chat`, `shared/config/WebSocketConfig` | 상담방 = GROUP 채팅방. 메시지 모델은 그대로, 번역은 별도 테이블 |
| 다국어 화면 | `i18n/messages.ts` 9개 언어, `I18nProvider` | `frontend/src/i18n` | 담당자 화면은 ko·en만 우선 |
| 안내 번역 7개 언어 | 리포트 번역 체계(ko·en·ja·zh·de·fr·vi), 게이트 발상 | `scripts/report_regen`, V28~V33 | 번역 생성은 앱 안에서(포트) 한다. 게이트는 "숫자 일치·언어 섞임" 검사를 Notice에 맞게 축소 |
| 플랫폼 관리자 | `/admin/**` Thymeleaf + `AdminApiController` | `adapter/in/web`, `templates/admin` | Office 생성·첫 담당자 초대만 여기 추가. 담당자 화면은 **Next.js** |
| 파일 첨부 | `MediaFile`, `StoragePort` | `domain/media` | 공지 첨부(PDF·이미지) |
| 이메일 | SMTP(인증 코드 발송) | 인증 어댑터 | 담당자 초대·이관 알림 |
| 요청 제한 | `RateLimiterService` | `shared` | 질문 API(LLM 호출)에 필수 |
| 만료 스케줄러 패턴 | `VerificationCredentialExpiryScheduler` | `adapter/in/scheduler` | Notice 만료에 같은 패턴. 건별 트랜잭션(GLOBAL-PIT-128) |

**기존 코드의 알려진 틈** — UNIVERSITY 범위 게시판은 `minTrustLevel`만 보고 **그 대학 소속인지 확인하지 않는다**
(`PostCommandService.enforceTrustGate`). Office 콘텐츠는 이 틈을 반복하지 않도록 소속 검사를 정책 객체로 분리한다(§7).
게시판 쪽 수정은 이 설계 범위 밖이며 별도 이슈로 다룬다.

---

## 4. 도메인 모델 (새 Bounded Context `office`)

`domain/office/model/` — 프레임워크 import 없음. 식별자는 기존 관례대로 `Long`/VO.

### 4.1 Office (Aggregate Root)

| 필드 | 타입 | 불변식 |
|---|---|---|
| id | Long | |
| globalUnivId | Long | 필수. **Office당 대학 1곳, 대학당 Office 1개**(UNIQUE) |
| name | String | 표시 이름. 기본값은 대학 이름 |
| status | `OfficeStatus` {PILOT, ACTIVE, SUSPENDED} | SUSPENDED면 학생·담당자 모든 쓰기 거부 |
| answerLanguages | Set\<Lang\> | 기본 7개 언어. 비울 수 없음 |
| partnerSettings | Map\<PartnerCategory, Boolean\> | 대학별 제휴 노출 on/off. 기본 전부 off |
| createdAt | Instant | |

### 4.2 OfficeStaff (Entity, Office 하위)

| 필드 | 불변식 |
|---|---|
| officeId, memberId | (officeId, memberId) UNIQUE |
| role `OfficeRole` {OFFICE_ADMIN, OFFICE_STAFF} | Office에 OFFICE_ADMIN이 최소 1명 남아야 한다(마지막 ADMIN 강등·제거 금지) |
| status {INVITED, ACTIVE, REMOVED} | INVITED는 초대 이메일의 회원이 로그인해 수락하면 ACTIVE |
| invitedBy, invitedAt, acceptedAt | |

담당자는 일반 `Member`다. `MemberRole`은 바꾸지 않는다(플랫폼 ADMIN과 섞지 않는다). 권한 부여는 §7.

### 4.3 Notice (Aggregate Root)

| 필드 | 타입 | 불변식 |
|---|---|---|
| id, officeId | | |
| category | `NoticeCategory` {HOUSING, VISA_STAY, INSURANCE, ACADEMIC, SCHOLARSHIP, ARRIVAL, LIFE, OTHER} | |
| title, body | String | body는 원문(대개 한국어). 공백 금지 |
| sourceUrl | String? | 원 공지 링크 |
| attachmentMediaIds | List\<Long\> | |
| term | String | 적용 학기 `2027-1` 형식 |
| effectiveFrom, expiresAt | LocalDate | **expiresAt > effectiveFrom**. APPROVED 전이에 둘 다 필수 |
| status | `NoticeStatus` {DRAFT, APPROVED, EXPIRED, ARCHIVED} | DRAFT→APPROVED(approvedBy 필수) · APPROVED→EXPIRED(만료일 경과, 시스템) · APPROVED/EXPIRED→ARCHIVED · EXPIRED→APPROVED는 **새 만료일**을 줘야 가능 |
| approvedBy, approvedAt | | |
| version | int | 본문 수정 시 +1, 상태를 DRAFT로 되돌리고 번역을 무효화 |
| translations | List\<NoticeTranslation\> | |

`NoticeTranslation(lang, title, body, status {MACHINE, REVIEWED}, sourceVersion)`. `sourceVersion != notice.version`이면 stale로
간주해 답변 근거에서 제외한다.

**답변 근거 자격** = `status == APPROVED && effectiveFrom <= today <= expiresAt && 해당 언어 번역이 stale 아님`.
이 판정은 도메인 메서드 `Notice.isGroundingEligible(LocalDate today, Lang lang)` 한 곳에 둔다.

### 4.4 Inquiry (Aggregate Root)

| 필드 | 불변식 |
|---|---|
| id, officeId, memberId | |
| lang | 학생이 질문한 언어 |
| question | 공백 금지, 길이 상한(예: 1,000자) |
| outcome `InquiryOutcome` {ANSWERED, NO_GROUNDS, ESCALATED} | |
| answerText | ANSWERED일 때만 비어 있지 않음 |
| citedNoticeIds | ANSWERED면 1개 이상, **검색 후보 집합의 부분집합** |
| candidateNoticeIds | 검색이 돌려준 후보(감사용) |
| askedAt, latencyMs, modelId | 운영 지표·비용 추적 |

Inquiry는 수정하지 않는다(append-only). 통계의 원천이다.

### 4.5 Consultation (Aggregate Root)

| 필드 | 불변식 |
|---|---|
| id, officeId, memberId | |
| inquiryId | nullable (학생이 질문 없이 바로 상담 요청 가능) |
| consentAt, consentText | **없으면 생성 불가**. 학생이 본 동의 문구 버전을 그대로 저장 |
| sharedContext | 학생이 동의한 공유 범위에 따라 질문·자동답변·언어 |
| status {OPEN, IN_PROGRESS, RESOLVED, CLOSED} | OPEN→IN_PROGRESS(담당자 배정) → RESOLVED(담당자) → CLOSED(학생 확인 또는 7일 경과). CLOSED에서 재개 불가, 새 상담 생성 |
| assignedStaffMemberId | IN_PROGRESS 이후 필수 |
| chatRoomId | 생성 시 GROUP 채팅방을 만들어 연결(학생 + Office 활성 담당자 전원) |
| openedAt, firstStaffReplyAt, resolvedAt, closedAt | 응답 시간 지표 |

`MessageTranslation(messageId, lang, text)` — 담당자 메시지를 학생 언어로, 학생 메시지를 한국어로 번역한 결과. 원문은
`message.content` 그대로. 번역 실패 시 원문만 보여 주고 실패를 기록한다(빈 값으로 치환 금지, GLOBAL-PIT-108의 서버판).

### 4.6 Partner (Aggregate Root, 플랫폼 공통)

| 필드 | 불변식 |
|---|---|
| id, category `PartnerCategory` {SIM, INSURANCE, HOUSING, PICKUP} | |
| name, descriptionKey(i18n), targetUrl | 제휴 표시는 화면이 강제(§6.3) |
| enabled | 플랫폼 수준 on/off. 대학별은 `Office.partnerSettings` |
| `PartnerClick(partnerId, officeId, memberId?, clickedAt)` | 집계용 append-only |

---

## 5. 핵심 흐름

### 5.1 자료 등록 → 승인 → 번역 → 만료

```
담당자 등록(DRAFT) → 승인(APPROVED, expiresAt 필수)
  → [비동기] 7개 언어 번역 생성(MACHINE) → 간이 게이트(숫자·날짜 일치, 언어 섞임) 실패 시 그 언어만 미생성 + 담당자 알림
  → 매일 00:10 스케줄러: expiresAt < today → EXPIRED (건별 트랜잭션), 만료 7일 전 담당자 이메일
담당자 본문 수정 → version+1, DRAFT로 복귀, 번역 stale
```

번역은 `TranslationPort.translate(text, from, to)`로 추상화한다. 구현은 LLM 어댑터(§8). 리포트 재생성에서 쓴 "확인 못 한 내용은
쓰지 않는다·숫자는 원문 그대로" 지침을 프롬프트에 넣는다.

### 5.2 학생 질문 → 근거 기반 답변

```
POST /api/v1/office/{officeId}/inquiries {question, lang}
1. 소속 검사(§7) · 요청 제한(회원당 분당 N회, 일당 M회) · 길이 검사
2. 후보 검색: 그 Office의 자격 있는 Notice(§4.3)에서 PostgreSQL FTS(원문 + 학생 언어 번역) 상위 K(기본 8)
   후보 0건 → outcome NO_GROUNDS, 답변 대신 "국제처에 문의/상담 요청" 버튼
3. LLM 호출(AnswerGenerationPort): 시스템 지침 + 후보 Notice 본문(ID 포함) + 질문
   지침: 후보에 있는 내용만, 학생 언어로, 문장마다 [N] 인용, 후보로 답할 수 없으면 정확히 "NO_GROUNDS"만 출력,
   날짜·금액·기간은 원문 그대로, 개인 상황 판단(체류 자격 가능 여부 등)은 하지 않고 상담을 권한다
4. 후처리 검증: 인용 ID ⊆ 후보 집합이 아니면 실패로 처리(NO_GROUNDS로 강등 + 경고 로그). 인용 0개면 NO_GROUNDS
5. Inquiry 저장(후보·인용·지연·모델) → 응답: answer, citations[{noticeId, title, sourceUrl, expiresAt}], canEscalate=true
```

학생 질문은 **신뢰하지 않는 입력**이다. 질문은 사용자 메시지에만 넣고, Notice 본문과 지침은 분리한다. 도구 사용·외부 호출은
주지 않는다. 답변에 URL이 있으면 Notice의 `sourceUrl`만 허용한다.

### 5.3 상담 이관

```
학생: 답변 화면 "담당자에게 전달" → 동의 화면(공유되는 것: 질문·자동 답변·내 언어·학교 이메일 인증 여부 / 보관 기간 / 철회 방법)
  → POST /consultations {inquiryId?, consentVersion, message?}
서버: Consultation(OPEN) + GROUP ChatRoom(학생 + Office 활성 담당자) + 담당자 이메일 알림
담당자: 받은함에서 "맡기" → IN_PROGRESS(assigned) → 채팅(한국어 입력 → 학생 언어 번역 병기) → "해결" → RESOLVED
학생: "확인" → CLOSED. 7일 무응답도 CLOSED
```

상담방 메시지는 채팅 도메인에 그대로 저장되고, Consultation은 상태·시각만 가진다. 담당자가 REMOVED되면 그 Office의 열린 상담방에서
빠진다(채팅 참가자 변경 유스케이스 추가).

### 5.4 대시보드 지표 (학기 단위, Office 범위)

| 지표 | 계산 |
|---|---|
| 자동 안내 종결 비율 | ANSWERED / 전체 Inquiry |
| 근거 없음 비율 | NO_GROUNDS / 전체 → 어떤 질문이 많은지 목록 = **공지 보강 후보** |
| 이관 건수·첫 응답 시간·해결 시간 | Consultation 시각 필드 |
| 반복 문의 상위 10 | 1차: 정규화 텍스트(소문자·공백·문장부호 제거) 그룹. 2차(주 1회 배치): LLM이 카테고리·대표 질문으로 묶음 |
| 만료 임박·만료된 공지 | Notice 날짜 |
| 제휴 클릭 | PartnerClick 집계(분류별) |
| 학생 만족도 | 답변 하단 👍/👎 + 선택 사유 → Inquiry에 부가 테이블 |

CSV 내보내기는 Inquiry(개인 식별 제거: memberId 해시), Consultation(상태·시각), Notice 목록.

---

## 6. 화면

### 6.1 담당자 웹 (Next.js, 새 라우트 그룹 `(office)` → `/office/...`)

PC 우선. 접근은 OFFICE_STAFF 권한이 있는 회원만(layout에서 `/api/v1/office/me`로 확인). ko 기본, en 제공.

| 경로 | 내용 |
|---|---|
| `/office` | 대시보드: 이번 학기 지표 카드, 만료 임박 공지, 열린 상담 |
| `/office/notices` | 목록(상태·분류·만료일 필터), 등록, 승인, 보관 |
| `/office/notices/[id]` | 본문·첨부·번역 7개 탭(기계/검토됨 표시, 검토 완료 버튼) |
| `/office/consultations` | 받은함(OPEN/IN_PROGRESS/RESOLVED), 맡기 |
| `/office/consultations/[id]` | 공유된 맥락 + 채팅(원문/번역 병기) + 해결 버튼 |
| `/office/inquiries` | 질문 로그(답변·근거 없음), 반복 문의 상위 |
| `/office/settings` | 담당자 초대·역할, 제휴 노출 on/off, 답변 언어, 학생 수동 인증 |

### 6.2 학생 (기존 앱 안, `(protected)` 그룹)

| 경로 | 내용 |
|---|---|
| `/my-university` | 소속 대학이 Office를 운영하면 진입. 미인증이면 학교 이메일 인증으로 안내 |
| `/my-university/ask` | 질문 입력 → 답변 + 출처 카드(제목·링크·유효기간) + "담당자에게 전달" + 👍/👎 |
| `/my-university/notices` | 분류별 승인 공지(내 언어), 만료일 표시 |
| `/my-university/consultations` | 내 상담 목록 → 채팅방 |
| `/my-university/arrival` | 입국 직후 정착: 제휴 카드(§6.3) + 관련 공지(ARRIVAL 분류) |

### 6.3 제휴 표시 규칙

- 카드마다 "제휴 · Ovlo가 수수료를 받을 수 있습니다" 문구를 i18n 키로 **항상** 표시한다. 끌 수 없다.
- 공지 본문·자동 답변 본문에는 제휴 링크를 넣지 않는다(프롬프트·후처리에서 Partner URL 차단).
- 대학이 끈 분류는 그 대학 학생에게 보이지 않는다.
- 클릭은 `POST /api/v1/office/{officeId}/partners/{id}/click` 이 집계한 뒤 목적지 URL 을 돌려주고 화면이 새 탭으로 연다(S5 구현). 설계 초안의 서버 302 리다이렉트는 Bearer 인증이라 `<a href>` 로 못 쓴다. 카드 응답에는 목적지 URL 이 없다. 개인 식별은 memberId만 `partner_click` 에 남고 외부에 넘기지 않는다.

---

## 7. 권한과 보안

세부 결정은 ADR-0005. 요지:

- **권한 부여**: 로그인·토큰 재발급 시 활성 `OfficeStaff`가 있으면 JWT에 `officeId`, `officeRole` 클레임을 넣고,
  `JwtAuthenticationFilter`가 `OFFICE_STAFF`(및 OFFICE_ADMIN이면 `OFFICE_ADMIN`) 권한을 추가한다. 역할 변경은 다음 재발급(≤15분)부터
  반영된다. 즉시 차단이 필요하면(제거) 그 회원의 세션을 폐기한다.
- **Office 범위 검사**는 URL의 `{officeId}`와 클레임의 `officeId`를 서비스 진입에서 비교하는 `OfficeAccessPolicy`로 한다.
  `@PreAuthorize`는 도입하지 않는다(기존 코드와 일관).
- **학생 소속 검사**: `StudentOfficeAccessPolicy.assertMember(memberId, officeId)` = 그 대학에 활성 `VerificationCredential`이 있을 것.
  `homeUniversityId`만으로는 통과시키지 않는다(자기 신고 값).
- **SecurityConfig**: 새 경로는 모두 `authenticated()` 아래 둔다. `permitAll` 추가 없음(GLOBAL-PIT-189). admin 체인은 건드리지 않는다
  (GLOBAL-PIT-005). `/api/v1/office-admin/**`은 `hasAnyAuthority("OFFICE_STAFF","OFFICE_ADMIN")`을 1차 관문으로 두고 범위 검사는 서비스에서.
- **플랫폼 ADMIN**: Office 생성·정지, 첫 OFFICE_ADMIN 초대만. Office 내부 자료·상담은 보지 않는다(필요하면 감사 목적 별도 권한).
- **개인정보**: 상담 내용은 학생 동의 문구 버전과 함께 저장. 보관 기간(예: 학기 종료 후 1년) 지나면 메시지 본문 삭제, 통계용 시각만 유지.
  학생 탈퇴 시 상담 메시지 본문 익명화. **개인정보처리방침·이용약관 페이지는 파일럿 전 필수**(처리 위탁: LLM 제공자 명시).
- **LLM 입력 최소화**: 질문·Notice 본문만 보낸다. 학생 이름·이메일·memberId는 보내지 않는다. 상담 번역은 메시지 본문만.
- **요청 제한·비용**: 질문 API 회원당 분당 5회·일당 50회(설정값). Office당 일일 상한도 둔다. 초과 시 429 `OFFICE_RATE_LIMITED`.
- **오류 코드**: `OFFICE_NOT_FOUND`, `OFFICE_FORBIDDEN`(소속 아님·범위 밖), `NOTICE_INVALID_STATE`, `CONSULTATION_CONSENT_REQUIRED`,
  `OFFICE_SUSPENDED`. 기존 `ErrorResponse(code, error, fieldErrors)` 형식과 `GlobalExceptionHandler` 매핑 추가.

---

## 8. 외부 연동 포트

| 포트 | 역할 | 1단계 구현 |
|---|---|---|
| `AnswerGenerationPort.answer(context, question, lang)` | 근거 기반 답변 | Anthropic Claude API 어댑터. 모델 ID·최대 토큰은 설정값(`ovlo.llm.answer.model`) |
| `TranslationPort.translate(text, from, to)` | 공지·메시지 번역 | 같은 어댑터. 번역 전용 지침 |
| `InquiryClusteringPort.cluster(questions)` | 주 1회 반복 문의 묶기 | 같은 어댑터. 실패해도 1차 정규화 그룹만으로 화면은 동작 |

공통: 타임아웃·재시도 1회·실패 시 예외(빈 값 치환 금지). 호출마다 모델·입출력 토큰을 로그·메트릭으로 남긴다.
API 키는 환경 변수(`ANTHROPIC_API_KEY`)로만 주입하고 기동 시 존재를 검사한다. 로컬·테스트 프로필은 `FakeLlmAdapter`(결정적 응답)를 쓴다.
모델 선택·가격은 구현 시점에 `claude-api` 스킬로 확인한다.

---

## 9. 데이터 (Flyway V34~)

| 마이그레이션 | 내용 |
|---|---|
| V34 `office_tenant` (S1 구현됨) | `office(id, global_univ_id UNIQUE FK, name, status, created_at, updated_at)` · `office_staff(id, office_id FK, member_id FK, role, status, invited_by, invited_at, accepted_at, removed_at, UNIQUE(office_id, member_id))`. `answer_languages`·`partner_settings` 는 쓰는 스프린트(S2·S5)에서 추가한다. 테스트 프로필(H2)은 엔티티로 스키마를 만들므로 배열·jsonb 컬럼은 피한다 |
| V35 `office_notice` | `office_notice(id, office_id FK, category, title, body, source_url, attachment_media_ids BIGINT[], term, effective_from, expires_at, status, approved_by, approved_at, version, created_by, created_at, updated_at)` · `office_notice_translation(notice_id FK, lang, title, body, status, source_version, generated_at, PK(notice_id, lang))` · 인덱스 `(office_id, status, expires_at)` · FTS: `tsvector` 생성 컬럼(원문 `simple` 구성 + 번역별) 과 GIN |
| V36 `office_inquiry` | `office_inquiry(id, office_id, member_id, lang, question, outcome, answer_text, cited_notice_ids BIGINT[], candidate_notice_ids BIGINT[], asked_at, latency_ms, model_id)` · `office_inquiry_feedback(inquiry_id PK, helpful BOOL, reason, created_at)` · 인덱스 `(office_id, asked_at)` |
| V37 `office_consultation` | `office_consultation(id, office_id, member_id, inquiry_id?, chat_room_id FK, status, assigned_staff_member_id?, consent_version, consent_text, shared_context JSONB, opened_at, first_staff_reply_at, resolved_at, closed_at)` · `chat_message_translation(message_id FK, lang, text, PK(message_id, lang))` |
| V38 `partner` | `partner(id, category, name, description_key, target_url, enabled, created_at)` · `partner_click(id, partner_id FK, office_id FK, member_id?, clicked_at)` |

제약: `office_notice` CHECK `expires_at > effective_from`. 상태 전이는 도메인에서만. 삭제는 없고 ARCHIVED/REMOVED 상태만 쓴다
(소프트 삭제 도입 시 인증 경로 전수 확인, GLOBAL-PIT-126). 유니크 위반을 catch해 멱등 처리하지 않는다(GLOBAL-PIT-184).

---

## 10. API 목록

접두 `/api/v1`. 인증 필수. 응답 DTO는 목록용(요약)과 상세용을 분리한다.

**학생**

| 메서드·경로 | 내용 |
|---|---|
| GET `/office/me` | 내 소속 Office(없으면 404), 인증 여부, 제휴 노출 분류 |
| GET `/office/{officeId}/notices?category&lang` | 자격 있는 공지 목록(내 언어) |
| GET `/office/{officeId}/notices/{id}?lang` | 공지 상세 + 출처 + 유효기간 |
| POST `/office/{officeId}/inquiries` | 질문 → 답변/근거 없음 |
| POST `/office/{officeId}/inquiries/{id}/feedback` | 👍/👎 |
| POST `/office/{officeId}/consultations` | 상담 요청(동의 포함) |
| GET `/office/{officeId}/consultations` · PATCH `.../{id}/close` | 내 상담, 확인 종료 |
| GET `/office/{officeId}/partners` · POST `/office/{officeId}/partners/{id}/click` | 제휴 카드(대학이 켠 분류만), 클릭 집계 후 목적지 URL 반환 |
| GET `/office-admin/{officeId}/dashboard?from&to` · GET `/office-admin/{officeId}/export/{inquiries,consultations,notices}.csv` · GET/PUT `/office-admin/{officeId}/settings/partners` | 대시보드(§5.4)·CSV(BOM, 학생 식별 없음)·제휴 분류 on/off(PUT 은 OFFICE_ADMIN) |
| `/api/v1/admin/partners` · `/admin/partners` | 플랫폼 관리자 제휴 등록·수정·켜기/끄기 (REST + Thymeleaf) |

**담당자** (`/office-admin/{officeId}/...`, OFFICE_STAFF 이상)

| 메서드·경로 | 내용 |
|---|---|
| GET `/dashboard?term` | §5.4 지표 |
| GET/POST `/notices`, GET/PUT `/notices/{id}`, POST `/notices/{id}/approve`, POST `/notices/{id}/archive`, POST `/notices/{id}/translations/{lang}/review` | 자료 관리 |
| GET `/consultations?status`, POST `/consultations/{id}/assign`, POST `/consultations/{id}/resolve` | 상담 처리 |
| GET `/inquiries?outcome&from&to`, GET `/inquiries/clusters` | 질문 로그·반복 문의 |
| GET `/export/inquiries.csv` 등 | 내보내기 |
| (OFFICE_ADMIN) GET/POST `/staff`, PATCH `/staff/{memberId}`, PUT `/settings`, POST `/students/{memberId}/verify` | 초대·역할·설정·수동 인증 |

**플랫폼 관리자** — Thymeleaf `/admin/offices`: Office 생성(대학 검색), 정지, 첫 OFFICE_ADMIN 초대. `AdminApiController`에 대응 엔드포인트.

**WebSocket** — 상담방은 기존 `/topic/chat/{roomId}` 그대로. 번역 메시지는 REST(`GET /chat/rooms/{id}/messages?lang`)로 병기 조회하거나,
메시지 브로드캐스트 페이로드에 `translations` 필드를 추가한다(2차).

---

## 11. 개발 순서와 테스트

TDD. 각 스프린트는 도메인 테스트 → 유스케이스 테스트(Mockito, 고정 Clock) → 어댑터/컨트롤러 테스트 → 보안 통합 테스트
(`@SpringBootTest @ActiveProfiles("test")`, 실제 필터 체인) 순. 기존 예: `SchoolEmailVerificationServiceTest`,
`VerificationApiControllerTest`, `MemberSearchApiSecurityTest`.

| 스프린트 | 산출물 | 반드시 있는 테스트 |
|---|---|---|
| S0 (병행) | 개인정보처리방침·이용약관 페이지(ko·en), 상담 동의 문구 v1 | 라우트 렌더 테스트 |
| S1 테넌트·권한 | `Office`·`OfficeStaff` 도메인, V34, Office 생성·초대·수락 유스케이스, JWT 클레임·권한, `OfficeAccessPolicy`, `StudentOfficeAccessPolicy`, admin 화면 | 다른 Office 경로 접근 403 · 미인증 학생 403 · 마지막 OFFICE_ADMIN 제거 거부 · 클레임 없는 토큰으로 office-admin 403 |
| S2 승인 자료 | `Notice`·번역, V35, 등록/승인/보관/수정→DRAFT, 만료 스케줄러, `TranslationPort` + Fake, 담당자 공지 화면, 학생 공지 화면 | 상태 전이 전수 · `isGroundingEligible` 경계(만료일 당일, stale 번역) · 스케줄러 건별 트랜잭션(한 건 실패가 다른 건을 막지 않음) |
| S3 질문·답변 | `Inquiry`, V36, FTS 검색 어댑터, `AnswerGenerationPort` + Fake, 인용 검증 후처리, 요청 제한, 학생 질문 화면·피드백 | 후보 0건→NO_GROUNDS · 인용이 후보 밖→NO_GROUNDS 강등 · 만료 공지는 후보에서 제외 · 다른 Office 공지 섞이지 않음 · 429 |
| S4 상담 이관 | `Consultation`, V37, 동의 필수 생성, GROUP 채팅방 연결, 배정/해결/종료, 메시지 번역, 담당자 받은함·학생 상담 화면, 이메일 알림 | 동의 없이 생성 400 · 상태 전이 · 담당자 REMOVED 시 방에서 제거 · 번역 실패 시 원문 유지 |
| S5 대시보드·제휴 (구현됨) | 지표 집계 쿼리(`LoadOfficeStatsPort`), 반복 문의 1차 그룹(정규화 완전 일치, `InquiryClustering`), CSV, `Partner`·클릭, V38(`partner`·`partner_click`·`office.enabled_partner_categories`), 대학별 on/off, 제휴 카드 | 집계 수치 고정 데이터 검증(PG 통합) · 끈 분류 미노출 · 클릭 집계 후 URL 반환 |
| S6 파일럿 준비 | 운영 설정(모델·상한·키), 메트릭 대시보드, 시드(파일럿 Office), 운영 점검표 | 보안 통합 테스트 전체 회귀 |

S1~S3가 끝나면 국제처에 **자료 등록 + 질문 데모**를 보여 줄 수 있다. S4까지가 파일럿 최소 범위, S5는 파일럿 중 추가해도 된다.

---

## 12. 열린 질문 (결정 필요)

1. 소속 학생 인증 — 학교 이메일이 늦게 발급되는 신입생을 위해 **국제처가 명단으로 일괄 수동 인증**하는 기능을 S1에 넣을지(권장: S1에 단건, 일괄은 S6).
2. 답변 언어 — 학생 UI 언어와 질문 언어가 다를 때 어느 쪽으로 답할지(권장: 질문 언어).
3. 상담방 참가자 — Office 담당자 전원을 넣을지, 배정 전엔 담당자 없이 "대기"로 둘지(권장: 전원 참가, 배정은 표시만).
4. 번역 검토 — 기계 번역을 학생에게 바로 노출할지, 담당자 검토 뒤에만 노출할지(권장: 바로 노출하되 "기계 번역" 표시, 검토되면 표시 제거).
5. 리포트 재생성 도구와 공유 — `scripts/report_regen`의 게이트 함수를 앱 안 Java로 옮길지, 번역 게이트만 새로 작게 만들지(권장: 새로 작게).
6. 모델·비용 상한 — 질문당 토큰 상한과 Office 일일 상한의 초기값. 구현 시 `claude-api` 스킬로 가격을 확인해 정한다.
