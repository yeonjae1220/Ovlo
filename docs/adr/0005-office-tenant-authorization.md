# ADR-0005: 국제처 테넌트 권한은 JWT 클레임 + 서비스 레벨 범위 검사로 한다

- 상태: Proposed
- 날짜: 2026-10-30
- 결정자: Backend / Security
- 관련 설계: [국제처 파일럿 제품 설계](../intl-office-pilot-design.md) §7
- 관련 Pitfall: GLOBAL-PIT-005, GLOBAL-PIT-189, GLOBAL-PIT-126

## 맥락

1단계 제품은 대학 국제처(Office)를 테넌트로 둔다. 담당자는 자기 Office의 자료·상담만 보고 써야 하고, 학생은 소속 대학의
Office 콘텐츠만 봐야 한다. 기존 권한 모델은 다음과 같다.

- `MemberRole {MEMBER, ADMIN}` 하나뿐이다. ADMIN은 플랫폼 운영자이며 `/admin/**` Thymeleaf 체인은 회원 테이블과 무관한
  env 기반 단일 계정으로 인증한다(`adminAuthenticationManager`).
- JWT 클레임은 `memberId`, `role`. `JwtAuthenticationFilter`가 `ADMIN` 또는 `ROLE_USER` 권한을 준다.
- 리소스 범위 검사는 어노테이션이 아니라 서비스 안에서 한다(예: `PostCommandService.enforceTrustGate`).
- UNIVERSITY 범위 게시판은 신뢰 등급만 보고 **그 대학 소속인지는 보지 않는다**. 같은 틈을 반복하면 안 된다.

선택지:

A. `MemberRole`에 `OFFICE_STAFF`를 추가하고 역할만으로 판단 — 어느 Office인지 표현이 안 되고, 플랫폼 ADMIN과 의미가 섞인다.
B. 요청마다 DB에서 `office_staff`를 조회해 권한 결정 — 정확하지만 모든 요청에 조회가 붙고, 기존 무상태 JWT 설계와 어긋난다.
C. 로그인·재발급 때 `office_staff`를 조회해 JWT에 `officeId`·`officeRole`을 넣고, 필터는 권한만 부여하며 범위 검사는 서비스가 한다.
D. `@PreAuthorize("@officeAccess.check(#officeId)")` 메서드 보안 도입 — 기존 코드에 없는 방식이고 테스트 관례도 없다.

## 결정

**C를 택한다.**

1. `MemberRole`은 바꾸지 않는다. 담당자 자격은 `office_staff(office_id, member_id, role, status)`에 둔다.
2. 토큰 발급·재발급 시 활성 `OfficeStaff`가 있으면 클레임 `officeId`, `officeRole`을 넣는다. 없으면 클레임을 생략한다.
3. `JwtAuthenticationFilter`는 클레임이 있을 때 `OFFICE_STAFF`를, `officeRole == OFFICE_ADMIN`이면 `OFFICE_ADMIN`을 추가로 부여한다.
   플랫폼 `ADMIN`과는 독립이다. admin 필터 체인은 건드리지 않는다.
4. `SecurityConfig`에서 `/api/v1/office-admin/**`은 `hasAnyAuthority("OFFICE_STAFF","OFFICE_ADMIN")`, `/api/v1/office/**`는 `authenticated()`.
   새 경로에 `permitAll`을 추가하지 않는다.
5. 범위 검사는 두 정책 객체로 모은다.
   - `OfficeAccessPolicy.assertStaff(principal, officeId[, requiredRole])` — 클레임 `officeId`와 URL `officeId`가 같아야 한다.
   - `StudentOfficeAccessPolicy.assertMember(memberId, officeId)` — 그 Office의 대학에 **활성 `VerificationCredential`**이 있어야 한다.
     `homeUniversityId`는 자기 신고 값이라 쓰지 않는다.
   모든 Office 유스케이스는 첫 줄에서 둘 중 하나를 호출한다. 실패는 `OFFICE_FORBIDDEN`(403).
6. 담당자 제거·역할 강등은 다음 재발급(접근 토큰 TTL 15분)부터 반영된다. 즉시 차단이 필요하면 그 회원의 세션을 폐기한다
   (`TokenStorePort`에 회원 단위 폐기가 없으면 S1에서 추가).
7. Office당 OFFICE_ADMIN이 최소 1명 남아야 한다는 불변식은 도메인(`Office`)이 지킨다.

## 결과

- 장점: 요청 경로에 DB 조회가 늘지 않는다. 기존 필터·예외 처리·테스트 관례를 그대로 쓴다. 테넌트 경계가 정책 객체 두 개에
  모여 보안 테스트가 쉽다.
- 단점: 권한 변경 반영이 최대 15분 늦다(세션 폐기로 보완). 한 회원이 두 Office의 담당자가 되는 경우를 1단계에서는 지원하지 않는다
  (`office_staff`는 허용하지만 클레임은 하나만 — 필요해지면 클레임을 배열로 바꾼다).
- 테스트: 보안 통합 테스트에 "다른 Office 경로 403", "클레임 없는 토큰으로 office-admin 403", "미인증 학생의 office 콘텐츠 403"을
  고정한다(GLOBAL-PIT-189의 "실제 보안 필터를 태운 테스트").
