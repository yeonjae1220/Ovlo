# Ovlo iOS · Android 스토어 출시 계획

> 작성 2026-11-25 · 기준 커밋 `origin/main@9fe040e`
> 범위: 이미 있는 Capacitor 셸을 App Store·Google Play 에 올리는 데 필요한 작업과 순서.

---

## 1. 현재 구조 (이미 있는 것)

앱을 새로 만들 필요는 없다. **Capacitor Hybrid 셸이 이미 레포에 있다** (OV-ADR-013, PR #26·#29).

```
┌─ iOS 앱 (frontend/ios)  ─┐      ┌─ Android 앱 (frontend/android) ─┐
│  WKWebView               │      │  Android WebView                │
└──────────┬───────────────┘      └───────────────┬─────────────────┘
           │  server.url = https://ovlo.mungji.com (원격 로드)
           ▼
   Next.js 15 SSR (k3s, ovlo-frontend)  ──/api, /ws──▶  Spring Boot (k3s)  ─▶ PostgreSQL · Redis
```

| 항목 | 상태 |
|---|---|
| 방식 | 정적 번들을 싣지 않고 배포 사이트를 WebView 로 로드 (같은 origin → CORS·쿠키·CSP·SockJS 무수정) |
| 번들 ID | `com.mungji.ovlo` (iOS·Android 공통) |
| 플러그인 | `@capacitor/app`, `@capacitor/browser` 뿐 (v8.5) |
| 네이티브 코드 | `NativeShell.tsx` — 뒤로가기, 딥링크 수신 / `nativeOAuth.ts` — Google OAuth 를 시스템 브라우저로 |
| 딥링크 | Universal Links·App Links 가 `/oauth/callback*` 만 앱으로 넘김 |
| iOS | 최소 15.0, `MARKETING_VERSION 1.0` / build 1, Associated Domains entitlement 있음 |
| Android | min 24 / target·compile 36, `versionCode 1` |
| 법적 고지 | `/terms`, `/privacy` 페이지 있음, 가입 화면에서 링크 |
| 회원 탈퇴 | 프로필 화면에서 앱 안에서 가능 (`DELETE /api/v1/members/{id}`) |

**이 구조를 유지하는 걸 권한다.** 웹을 계속 운영하므로 재작성(RN·Flutter)은 프론트를 두 벌 유지하는 비용이 크고,
웹을 고치면 앱에도 바로 반영된다(스토어 재심사 없이). 대신 아래 심사 리스크를 직접 메워야 한다.

---

## 2. 출시를 막는 것 (심사 기준 갭)

### 🔴 반드시 해결 (없으면 리젝 확률 높음)

| # | 갭 | 근거 | 현재 |
|---|---|---|---|
| B1 | **웹사이트 래퍼로 보임** | Apple 4.2 최소 기능 | 원격 URL 로드 + 플러그인 2개뿐. 네이티브 기능 사실상 없음 |
| B2 | **게시글·댓글·채팅·회원 신고/차단 없음** | Apple 1.2 UGC, Google UGC 정책 | 신고·차단은 **마켓플레이스에만** 있음(`/marketplace/{id}/reports`, `/marketplace/blocks`) |
| B3 | **Sign in with Apple** | Apple 4.8 — 제3자 로그인(Google)을 제공하면 동급 개인정보 보호 로그인 옵션 필요 | 코드 완료(`feat/sign-in-with-apple`). 아래 §3.1 설정 전에는 버튼이 숨겨짐. 탈퇴 시 Apple 토큰 폐기 코드 완료(`feat/apple-token-revocation`, §3.1 4번 키 필요) |
| B4 | AASA 의 `TEAMID` 자리표시자 | Universal Links 검증 실패 → OAuth 콜백이 앱으로 안 돌아옴 | `appIDs: ["TEAMID.com.mungji.ovlo"]` |
| B5 | `assetlinks.json` 지문 | Play App Signing 을 쓰면 **Google 이 재서명**해 지문이 바뀜 → App Links 검증 실패 | 현재 지문 1개(로컬 키로 추정) |
| B6 | Privacy Manifest 없음 | Apple 필수(2024~) | `PrivacyInfo.xcprivacy` 없음 |

### 🟡 출시 전 준비 (코드 외)

- **심사용 데모 계정** — 이메일 인증·학교 인증 없이 핵심 기능(게시판·채팅·마켓)을 볼 수 있는 계정. 심사 노트에 기재.
- **Apple 개인정보 라벨 / Google 데이터 보안 양식** — 수집 항목: 이메일, 닉네임, 학교, 사진(업로드), 채팅 내용, 기기 식별자(푸시 도입 시).
- **Google Play 계정 삭제 URL** — 앱 밖에서도 삭제 요청할 수 있는 웹 경로 필요 (웹 프로필 탈퇴 화면 또는 안내 페이지).
- **스크린샷·아이콘** — iOS 6.9"/6.5" 아이폰(+iPad 지원 시 13"), Android 폰 스크린샷·512 아이콘·1024×500 피처 그래픽.
- **연령 등급** — 사용자 간 채팅·UGC 가 있어 iOS 는 12+~17+ 설문 결과로 정해짐, Google 은 IARC 설문.
- **수출 규정** — HTTPS 만 쓰면 `ITSAppUsesNonExemptEncryption = NO` 를 Info.plist 에 넣어 매 빌드 질문을 없앤다.

### ⏱️ 일정을 가장 크게 좌우하는 것

**Google Play 신규 개인 개발자 계정은 프로덕션 출시 전에 비공개 테스트(테스터 12명 이상, 14일 연속 참여)가 필수**다.
가입 직후부터 테스터 12명을 모으는 걸 시작해야 Android 일정이 밀리지 않는다.
(정확한 인원·기간은 가입 시점 Play Console 안내로 재확인 — 정책이 바뀐 적이 있다.)

---

## 3. 단계별 계획

### Phase 0 — 계정·기반 (1주, 병렬)

| 작업 | 어디서 |
|---|---|
| Apple: App Store Connect 에 앱 등록(`com.mungji.ovlo`), Team ID 확인, Associated Domains·Push·Sign in with Apple capability 활성화 | Mac |
| Android: Play Console 가입(신원 확인 수일 소요), 앱 생성, **테스터 12명 모집 시작** | — |
| gpunode WSL 에 Android 빌드 환경 확인: JDK 21, Android SDK cmdline-tools·platform 36·build-tools, `npx cap sync android` 동작 | gpunode |
| 업로드 키스토어 생성 → **레포 밖**에 보관(비밀번호는 비밀 저장소), Play App Signing 사용 | gpunode |

### Phase 1 — 심사 블로커 해소 (2~3주) — 백엔드·웹 중심, gpunode 또는 Mac 어디서든

1. **B2 신고·차단 일반화** (가장 큼)
   - 마켓플레이스 전용 신고·차단을 공통 `moderation` 영역으로 끌어올려 게시글·댓글·채팅 메시지·회원 프로필에 적용
   - 차단 시: 피드·댓글·채팅에서 상대 콘텐츠 숨김, DM 차단
   - 관리자 신고 처리 화면(기존 admin 에 탭 추가), 24시간 내 처리 원칙을 약관에 명시
   - 가입 시 "불쾌한 콘텐츠 무관용" 약관 동의(Apple 1.2 요구 문구)
2. **B3 Sign in with Apple** — 구현됨. 백엔드 `POST /api/v1/auth/apple`(identity token 서명·iss·aud·exp 검증, 원본 nonce 의 SHA-256 대조, `sub` 로 회원 연결),
   웹은 Apple JS 팝업, iOS 앱은 앱 안 네이티브 플러그인(`AppleSignInPlugin.swift` — 커뮤니티 플러그인은 Capacitor 7 SPM 에 묶여 8 과 충돌),
   Android 앱은 버튼 숨김. 같은 이메일이 다른 방법으로 가입돼 있으면 합치지 않고 409. 설정 절차는 §3.1.
3. **B4·B5 딥링크 정합** — AASA 에 실제 Team ID, assetlinks 에 Play App Signing 지문(+업로드 키 지문) 추가 후 배포·검증.

### 3.1 Sign in with Apple 켜기 (계정 준비 후, 순서대로)

1. Apple Developer → Identifiers: App ID `com.mungji.ovlo` 에 **Sign in with Apple** capability 켜기.
2. Services ID 생성(예: `com.mungji.ovlo.web`) → Sign in with Apple 설정에 도메인 `ovlo.mungji.com`,
   Return URL `https://ovlo.mungji.com/login` 등록(웹 팝업의 `redirectURI` 와 같아야 한다).
3. Xcode → Signing & Capabilities 에 Sign in with Apple 추가. `App.entitlements` 에 키는 이미 있다
   (Associated Domains 와 함께 이때 `CODE_SIGN_ENTITLEMENTS` 가 연결된다).
4. 백엔드: `ovlo-secret` 에 `APPLE_CLIENT_IDS=com.mungji.ovlo.web,com.mungji.ovlo` 추가 후 재시작(없으면 모든 Apple 요청 401).
   탈퇴 시 토큰 폐기용으로 Keys 에서 Sign in with Apple 키(.p8)를 만들고 같은 secret 에
   `APPLE_TEAM_ID`(10자)·`APPLE_KEY_ID`(10자)·`APPLE_PRIVATE_KEY`(.p8 내용 그대로, 줄바꿈은 `\n` 도 됨)를 넣는다.
   셋 중 하나라도 없으면 Apple 회원의 탈퇴가 503 으로 막힌다(오류 로그 `Apple 토큰 폐기 설정…`) — 5번보다 먼저.
5. 프론트: GitHub repo **variable** `APPLE_SERVICES_ID=com.mungji.ovlo.web` (공개값이라 secret 아님) → 다음 배포부터 버튼이 보인다.
6. 개인정보처리방침에 Apple(로그인 제공자, 수집 항목: 이메일·이름·Apple 사용자 식별자)을 추가한 뒤에 5번을 한다.
7. 확인: 웹(Safari·Chrome)에서 가입→온보딩→재로그인, iOS 실기기에서 같은 계정으로 로그인되는지(`sub` 는 웹·앱 공통).
8. 탈퇴 확인: 웹·iOS 에서 Apple 계정 탈퇴 → 설정 > Apple ID > 'Apple로 로그인' 목록에서 Ovlo 가 사라지는지.

**탈퇴 시 토큰 폐기(Apple 5.1.1(v))**: refresh token 을 저장하지 않고 **탈퇴 시점에 재인증**한다.
프로필 → 회원 탈퇴 → 'Apple로 확인하고 탈퇴' 가 Apple 인증을 다시 받아 `{identityToken, nonce, authorizationCode,
redirectUri}` 를 `DELETE /api/v1/members/{id}` 본문으로 보낸다. 서버(`MemberWithdrawalService`)는 토큰 검증·nonce·
`sub` 일치를 확인하고, code 를 `/auth/token` 에서 교환(교환 결과의 `sub` 도 재확인)한 뒤 `/auth/revoke` 로 폐기하고,
**폐기가 성공해야만** DB 탈퇴를 한다(Apple 호출은 트랜잭션 밖). 웹 code 는 팝업의 `redirectURI` 와 같은 값으로 교환한다.
- 저장하지 않는 이유: 장기 자격증명(refresh token)을 DB 에 두지 않아도 되고, 재인증이 '본인 확인' 역할도 한다.
- **Android 앱은 Apple 인증을 띄울 수 없어** Apple 계정 탈퇴는 웹·iOS 앱에서 하도록 안내 문구만 보인다.

### Phase 2 — 네이티브 가치 추가 (2~3주) — B1 대응

"웹을 감쌌을 뿐"이 아니라는 걸 보여 줄 기능. Ovlo 성격상 효과 순:

| 기능 | 플러그인 | 비고 |
|---|---|---|
| **푸시 알림** (채팅 메시지·댓글·팔로우) | `@capacitor/push-notifications` + FCM(Android)/APNs(iOS) | 가장 효과 큼. 백엔드에 `PushPort` + 디바이스 토큰 테이블, 알림 설정 화면. 웹 푸시(VAPID)는 WebView 에서 안 됨(PIT-118) |
| 오프라인·서버 장애 화면 | SW navigation fallback 또는 네이티브 에러 화면 | 현재 서버가 죽으면 흰 화면(PIT-111, 123) |
| 네이티브 공유 | `@capacitor/share` | 게시글·대학 리포트 공유 |
| 사진 선택·카메라 | `@capacitor/camera` | 업로드 UX, iOS 권한 문구 필요 |
| 스플래시·상태바·햅틱 | `splash-screen`, `status-bar`, `haptics` | 앱다운 마감 |
| 딥링크 확장 | AASA·App Links 경로에 `/posts/*`, `/universities/*` 추가 | 공유 링크가 앱으로 열림 |

플러그인을 추가할 때마다 **양 플랫폼 각각 `cap sync` + 네이티브 재빌드**가 필요하다(PIT-113) — 웹 배포만으로는 반영 안 됨.

### Phase 3 — 빌드·테스트 배포 (1~2주)

| | iOS (Mac) | Android (gpunode) |
|---|---|---|
| 서명 | Xcode 자동 서명, Distribution 인증서 | 업로드 키로 AAB 서명 (`./gradlew bundleRelease`) |
| 버전 | `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` 증가 | `versionName` / `versionCode` 증가 |
| 테스트 배포 | TestFlight 내부 → 외부 테스터 | 내부 테스트 → **비공개 테스트 14일** |
| 실기기 확인 | 시뮬레이터 + 실제 아이폰 | 실기기(adb) — 원격 adb 서버 쓸 때 PIT-122 주의 |
| 회귀 항목 | 로그인(이메일·Google·Apple), 콜드스타트 딥링크(PIT-112), 채팅 왕복, 사진 업로드(HEIC), 탈퇴, 신고·차단, 푸시 수신(포그라운드·백그라운드·종료 상태) | 동일 + 하드웨어 뒤로가기 |

### Phase 4 — 심사 제출·출시 (1~2주)

- 스토어 등록정보(한/영), 스크린샷, 개인정보 라벨·데이터 보안 양식, 연령 등급
- 심사 노트: 데모 계정, "웹 서비스와 같은 계정·콘텐츠를 쓰는 네이티브 앱이며 푸시·공유·카메라를 제공" 설명
- iOS 는 리젝 시 대응 1~2회 왕복을 일정에 포함
- Android 는 비공개 테스트 14일이 끝난 뒤 프로덕션 신청

**대략 총 7~10주.** Android 는 Phase 0 에서 테스터를 바로 모으면 14일 대기가 Phase 1~2 와 겹쳐 사라진다.

---

## 4. Mac / gpunode 역할 분담

| 작업 | Mac | gpunode (WSL) |
|---|---|---|
| 백엔드(신고·차단, Apple 로그인, 푸시 서버) | ○ | ◎ 주력 |
| 웹 프론트(Next.js) | ○ | ◎ 주력 |
| `frontend/ios` 빌드·서명·TestFlight·시뮬레이터 | ◎ 전용 | ✕ |
| `frontend/android` 빌드·서명·AAB 업로드 | ○ (가능) | ◎ 주력 |

주의:
- `frontend/ios`·`frontend/android` 는 **같은 레포**라 양쪽에서 커밋한다. `cap sync` 는 두 플랫폼 디렉터리를 모두 건드릴 수 있으니
  **플랫폼을 지정해서** (`npx cap sync ios` / `npx cap sync android`) 각 머신에서 자기 플랫폼만 커밋.
- 플러그인 추가(`package.json`)는 한쪽에서 먼저 커밋·push → 반대쪽 pull 후 sync.
- 키스토어·`google-services.json`·APNs 키(.p8)는 **커밋 금지**. `.gitignore` 를 `git check-ignore -v` 로 확인(PIT-188).

---

## 5. 결정이 필요한 것

1. **신고·차단 범위** — 게시글·댓글·채팅·회원 4곳 모두(권장) vs 최소(게시글·회원)
2. **푸시 범위** — 1차 출시에 넣기(4.2 대응에 가장 효과적, 권장) vs 출시 후
3. **iPad 지원** — 끄면(iPhone 전용) 스크린샷·레이아웃 부담이 줄어든다(권장: 1차는 iPhone 전용)
4. **스토어 표시 이름·카테고리** — 예: "Ovlo - 교환학생 커뮤니티" / 소셜 네트워킹(연령 등급에 영향) vs 교육
