# ADR-0005: UNIVERSITY 범위 게시판 쓰기는 그 대학의 활성 인증 자격으로만 판정한다

- 상태: Accepted
- 날짜: 2026-10-30
- 결정자: Backend / Security
- 관련: ADR-0001(도메인 간 ID 참조), `PostCommandService`, `TrustLevel`, `VerificationCredential`

## 맥락

`Board`는 `LocationScope.UNIVERSITY`일 때 `universityId`를 가진다("특정 대학 학생만"). 그런데 글쓰기 게이트는
게시판 `minTrustLevel`과 작성자 `TrustLevel`만 비교했고, **작성자가 그 대학과 관계가 있는지는 어디서도 보지 않았다.**
STUDENT 이상이기만 하면 어느 대학 게시판에나 글과 댓글을 쓸 수 있었다. 댓글은 게이트 자체가 없었다.

소속을 판정할 수 있는 근거는 두 가지다.

| 근거 | 출처 | 신뢰도 |
|---|---|---|
| `VerificationCredential` (활성, `universityId` 일치) | 학교 이메일 코드 인증 또는 관리자 수동 인증 | 제3자 검증됨 |
| `Member.homeUniversityId` | 가입(`RegisterMemberRequest`)·온보딩(`CompleteOnboardingRequest`)에서 본인이 선택 | 자기 신고값, 검증 없음 |

## 결정

1. UNIVERSITY 범위 게시판의 **글 작성과 댓글 작성**은 작성자가 게시판 `universityId`와 같은 대학의
   **활성**(`isActive()`, 즉 `VERIFIED`) `VerificationCredential`을 가질 때만 허용한다. 타입(SCHOOL_EMAIL·ADMIN_VERIFIED)은 가리지 않는다.
2. **`homeUniversityId`만으로는 소속으로 인정하지 않는다.** 본인이 고른 값이라 인정하면 아무 대학이나 골라 그
   게시판에 쓸 수 있고, 닫으려던 구멍이 그대로 남는다. 본교 인증을 받은 회원은 1번으로 통과하므로 정상 사용자에게 추가 부담은 없다.
3. 거부는 기존 `PostException.ErrorType.FORBIDDEN`(HTTP 403, `POST_FORBIDDEN`)으로 돌려준다.
   게시판 존재는 이미 목록 API로 공개돼 있어 404 로 숨길 이유가 없다.
4. 판정은 application 레이어(`PostCommandService`)가 `GetMyVerificationStatusQuery.verifiedUniversities`(활성 자격만 담김)로 한다.
   `Board` 도메인은 verification 도메인을 참조하지 않는다(기존 트러스트 게이트와 같은 위치).
5. 트러스트 게이트(`minTrustLevel`)는 기존대로 글쓰기에만 적용한다. 댓글에는 소속 게이트만 추가했다.
   인증 현황 조회는 두 게이트 중 하나라도 필요할 때 한 번만 한다.
6. **반응(좋아요·싫어요)과 읽기는 이 결정의 범위 밖이며 지금처럼 로그인 회원 전체에 연다.**
   반응은 내용을 남기지 않고, 대학 게시판 글은 목록·상세 API 에서 이미 모두에게 보인다. 닫기로 하면 별도 ADR 로 다룬다.

## 결과

- 교환 파견 중인 학생이 파견교 게시판에 쓰려면 파견교 이메일 인증(또는 관리자 인증)이 있어야 한다. 이것이 의도한 동작이다.
- 자격이 만료·취소되면 그 대학 게시판에 더 쓸 수 없다. 기존 글·댓글은 남는다.
- 자기 신고 본교를 소속 근거로 쓰고 싶어지면 이 ADR 을 대체하는 ADR 과 함께 바꾼다.
- 고정 테스트: `PostCommandServiceTest`(단위), `PostUniversityBoardApiSecurityTest`(실 보안 체인 + H2).
