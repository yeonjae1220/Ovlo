/**
 * 법적 문서 원문. 한국어가 정본이고, 영어와 나머지 UI 언어 번역을 둔다(terms/·privacy/).
 * 번역이 없는 언어는 영어를 보여 준다.
 * 문서를 바꿀 때 version 과 updatedAt 을 모든 언어에서 같이 올린다(화면에 표시되는 값이며 서버에 기록되지는 않는다).
 * 방침의 보관 기간·위탁·국외 이전 문구는 코드와 운영 설정에서 확인한 사실만 적는다.
 */
import type { UiLanguage } from '@/i18n/messages'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT, YOUTUBE_TERMS_URL } from './legalConstants'
import { PRIVACY_DE } from './privacy/de'
import { PRIVACY_ES } from './privacy/es'
import { PRIVACY_FR } from './privacy/fr'
import { PRIVACY_JA } from './privacy/ja'
import { PRIVACY_PT } from './privacy/pt'
import { PRIVACY_RU } from './privacy/ru'
import { PRIVACY_ZH } from './privacy/zh'
import { TERMS_DE } from './terms/de'
import { TERMS_ES } from './terms/es'
import { TERMS_FR } from './terms/fr'
import { TERMS_JA } from './terms/ja'
import { TERMS_PT } from './terms/pt'
import { TERMS_RU } from './terms/ru'
import { TERMS_ZH } from './terms/zh'


export type LegalLink = { label: string; href: string }
export type LegalSection = { heading: string; paragraphs: string[]; bullets?: string[]; links?: LegalLink[] }

export type LegalDocument = {
  version: string
  updatedAt: string
  title: string
  intro: string
  sections: LegalSection[]
}

/** ko·en 은 반드시 있고, 다른 언어는 번역이 있을 때만 둔다. */
export type LegalDocuments = { ko: LegalDocument; en: LegalDocument } & Partial<Record<UiLanguage, LegalDocument>>

export const PRIVACY_POLICY: LegalDocuments = {
  ko: {
    version: 'v4',
    updatedAt: '2026-11-28',
    title: '개인정보처리방침',
    intro:
      'Ovlo(이하 "서비스")는 「개인정보 보호법」에 따라 이용자의 개인정보를 보호하고 관련 고충을 신속하게 처리하기 위해 다음과 같이 개인정보처리방침을 둡니다.',
    sections: [
      {
        heading: '1. 수집하는 개인정보 항목',
        paragraphs: ['서비스는 회원가입·이용 과정에서 다음 항목을 수집합니다.'],
        bullets: [
          '필수: 이메일, 비밀번호(해시로만 저장), 닉네임, 이름, 출신지, 소속 대학, 전공·학위 과정·학년',
          '선택: 프로필 사진, 자기소개, 생년월일, 연락처, 언어 능력, 유학 경험',
          'Google 로그인 이용 시: Google 계정 이메일, 이름, Google 사용자 식별자',
          'Apple 로그인 이용 시: Apple 사용자 식별자, Apple 이 알려 주는 이메일(이메일 가리기를 고르면 Apple 의 중계 주소), 이용자가 알려 준 이름',
          '학교 이메일 인증 시: 인증한 학교 이메일 주소, 인증 시각, 인증 대학',
          '대학 국제처 안내·상담 이용 시: 질문과 자동 답변, 답변에 인용한 공지, 답변 평가, 상담 요청 시 동의한 문구와 그 버전, 상담방에서 주고받은 메시지와 그 번역',
          '중고거래 이용 시: 판매 글(제목·설명·가격·지역·학교·거래 장소·거래 가능 날짜), 상품 사진, 거래 채팅, 신고 사유',
          '캘린더 이용 시: 개인 일정(제목·설명·장소·시간), 학교 일정 관리자 신청 내용(부서·연락 이메일·메시지)',
          '신고·차단 시: 신고한 대상과 사유·설명, 차단한 회원',
          '의견 보내기 이용 시: 의견 내용, 적은 경우 회신 이메일, 앱 버전과 브라우저 정보',
          '서비스 이용 중 생성되는 정보: 게시글·댓글·채팅 메시지·첨부 파일, 접속 기록(IP 주소, 접속 시각, 요청 경로, 회원 번호), 기기·브라우저 정보',
          '쿠키: 로그인 유지용 리프레시 토큰(httpOnly), 화면 언어 설정',
        ],
      },
      {
        heading: '2. 개인정보의 수집·이용 목적',
        paragraphs: ['수집한 개인정보는 다음 목적에만 이용합니다.'],
        bullets: [
          '회원 식별, 로그인, 본인 확인(학교 이메일 인증)',
          '게시판·채팅·중고거래·캘린더 등 커뮤니티 기능 제공과 운영',
          '대학 국제처 공지에 근거한 자동 안내, 국제처 담당자와의 상담 연결, 서비스 사용법 안내',
          '공지·채팅 메시지·중고거래 글의 번역, 대학·교환학생 정보 제공, 화면 언어 맞춤',
          '부정 이용 방지, 신고 처리, 보안 사고 대응, 접속 기록 보관',
          '문의 응대와 공지 전달',
        ],
      },
      {
        heading: '3. AI 를 쓰는 기능',
        paragraphs: [
          '서비스의 다음 기능은 외부 AI 서비스로 답변이나 번역을 만듭니다. 외부 AI 서비스에는 질문·메시지 본문과 답변에 필요한 공지·도움말만 보내고, 이름·이메일·회원 번호 같은 식별 정보는 보내지 않습니다. 다만 이용자가 본문에 직접 적은 내용은 그대로 전달되므로 민감한 개인정보는 적지 않기를 권합니다.',
        ],
        bullets: [
          '대학 국제처 자동 안내: 학교 이메일 인증을 마친 학생이 소속 대학 국제처가 승인한 공지를 근거로 답을 받습니다. 질문과 답변은 답변 품질을 확인하고 상담에 이어 쓰기 위해 저장합니다(보관 기간은 5항).',
          '국제처 상담 연결: 이용자가 별도 화면에서 동의한 경우에만 대화 내용을 소속 대학 국제처 담당자에게 전달합니다. 담당자에게는 선택한 대화의 질문과 자동 답변(최대 20회), 사용 언어, 학교 이메일 인증 여부, 상담방에 표시되는 닉네임과 프로필 사진, 이후 상담방 메시지가 보이고, 새 상담 알림 메일에 첫 질문이 함께 전달됩니다. 이메일 주소와 이름은 전달하지 않습니다. 상담방 메시지는 서로의 언어로 번역됩니다.',
          '서비스 도움말: 로그인한 회원이 서비스 사용법을 물으면 서비스가 쓴 도움말 문서를 근거로 답합니다. 질문 본문은 답을 만드는 동안만 처리하고 저장하지 않습니다.',
          '번역: 국제처 공지와 중고거래 글을 이용자의 언어로 번역합니다.',
        ],
      },
      {
        heading: '4. 국제처 담당자가 보는 정보',
        paragraphs: [
          '국제처 담당자는 서비스 회원 중 해당 대학 국제처가 초대한 사람입니다. 담당자는 자기 대학에 들어온 상담만 볼 수 있고, 담당에서 빠지면 상담방에 접근할 수 없습니다. 담당자가 내려받는 상담 목록에는 상담 상태·시각·언어·첫 질문이 들어가며, 학생의 이메일과 이름은 들어가지 않습니다.',
        ],
      },
      {
        heading: '5. 보유·이용 기간',
        paragraphs: ['개인정보는 수집·이용 목적이 달성되면 지체 없이 파기합니다. 다만 다음은 명시한 기간 동안 보관합니다.'],
        bullets: [
          '회원 정보: 탈퇴 시까지. 탈퇴하면 즉시 계정 정보를 식별할 수 없게 처리하고, 올린 파일(프로필 사진·첨부 파일)을 삭제합니다. 게시글·댓글·채팅 메시지·중고거래 글은 삭제하지 않고 다른 이용자에게 보이지 않게 숨깁니다. 탈퇴한 계정은 되살릴 수 없습니다.',
          '학교 이메일 인증 기록: 인증일로부터 1년이 지나면 만료 처리하며, 탈퇴 시 삭제합니다.',
          '국제처 질문·답변과 상담 내용: 질문한 날(상담은 상담을 연 날)부터 1년이 지나면 본문을 삭제하고, 탈퇴하면 즉시 삭제합니다. 이용 시각과 처리 결과는 통계용으로 남습니다.',
          '서비스 도움말 질문: 저장하지 않습니다. 이용 횟수 제한을 위한 횟수 기록만 최대 하루 동안 둡니다.',
          '캘린더 개인 일정과 학교 일정 관리자 신청: 탈퇴 시 삭제합니다.',
          '신고 기록: 처리가 끝난 날(중고거래 신고는 접수한 날)부터 1년이 지나면 신고한 회원·신고 대상·사유를 지우고, 통계용으로 신고 유형·처리 결과·날짜만 남깁니다.',
          '차단 기록: 차단을 풀거나 어느 한쪽이 탈퇴할 때까지',
          '접속 기록(IP 주소, 접속 시각, 요청 경로, 회원 번호): 3개월(「통신비밀보호법」)',
          '로그인·가입·검색 횟수 제한을 위한 IP 주소와 이메일: 10분 이내에 자동 삭제',
          '데이터베이스 백업: 장애 복구를 위해 백업본을 따로 보관하므로, 삭제한 정보도 백업본이 순차 삭제될 때까지 백업 안에 남아 있을 수 있습니다. 백업본은 복구 외의 목적으로 쓰지 않습니다.',
        ],
      },
      {
        heading: '6. 처리 위탁',
        paragraphs: ['서비스는 이용자의 개인정보를 제3자에게 판매하거나 제공하지 않습니다. 서비스 운영을 위해 다음 업무를 위탁합니다.'],
        bullets: [
          '주식회사 AI3(BizRouter): AI 요청 중계 — 국제처 자동 안내, 상담 메시지·공지·중고거래 글 번역, 서비스 도움말 답변',
          'Anthropic PBC: AI 답변과 번역 생성(BizRouter 를 거쳐 처리)',
          'Google LLC: Google 로그인 인증, Gmail 을 통한 메일 발송(학교 이메일 인증 코드, 국제처 담당자 알림)',
          'Apple Inc.: Apple 로그인 인증과 탈퇴 시 Apple 연결 해제',
          'Cloudflare, Inc.: 서비스 접속 중계(전송 보안)와 문의 메일 전달',
        ],
      },
      {
        heading: '7. 개인정보의 국외 이전',
        paragraphs: [
          '위탁 업무를 처리하기 위해 다음과 같이 개인정보가 국외로 이전됩니다. 모든 전송은 이용자가 해당 기능을 쓸 때마다 암호화된 네트워크(HTTPS)로 이루어집니다. 받는 곳의 보유 기간은 각 회사의 개인정보처리방침과 서비스 약관을 따릅니다.',
        ],
        bullets: [
          'Anthropic PBC(미국, privacy@anthropic.com): 국제처 질문과 이전 대화(최대 20회), 답변 근거로 쓰는 공지·도움말, 번역할 메시지·공지·중고거래 글 본문 — AI 답변·번역 생성',
          'Google LLC(미국): Google 로그인 시 계정 정보, 발송하는 메일의 받는 주소와 내용(인증 코드, 상담 첫 질문) — 로그인 인증·메일 발송',
          'Apple Inc.(미국): Apple 로그인 확인 정보와 탈퇴 시 연결 해제에 쓰는 토큰 — 로그인 인증·연결 해제',
          'Cloudflare, Inc.(미국): 서비스에 접속할 때 오가는 요청(IP 주소 포함)과 문의 메일 — 접속 중계·메일 전달',
          '거부 방법: AI 기능과 Google·Apple 로그인을 쓰지 않으면 해당 정보는 이전되지 않습니다. Cloudflare 를 거치는 접속 중계는 서비스 이용에 꼭 필요해 거부하면 서비스를 이용할 수 없습니다.',
        ],
      },
      {
        heading: '8. 이용자의 권리',
        paragraphs: [
          '이용자는 언제든지 자신의 개인정보를 열람·정정·삭제하거나 처리 정지를 요구할 수 있으며, 프로필 화면에서 직접 수정하거나 아래 연락처로 요청할 수 있습니다. 회원 탈퇴는 프로필 화면에서 할 수 있고, 국제처 상담은 상담방에서 언제든 종료를 요청할 수 있습니다.',
        ],
      },
      {
        heading: '9. 쿠키와 로컬 저장소',
        paragraphs: [
          '로그인 유지를 위해 httpOnly 쿠키에 리프레시 토큰을 저장하고, 화면 언어 설정을 쿠키와 브라우저 저장소에 둡니다. 광고·추적 목적의 쿠키는 사용하지 않습니다. 브라우저 설정으로 쿠키를 거부할 수 있으나 로그인 유지가 되지 않을 수 있습니다.',
        ],
      },
      {
        heading: '10. 외부 영상 링크와 YouTube API 서비스',
        paragraphs: [
          '서비스는 대학 리포트 화면에 보여 주는 YouTube 영상 링크 목록을 만들기 위해 YouTube API 서비스를 사용합니다. 이 과정에서 서비스는 이용자의 개인정보를 YouTube 나 Google 에 보내지 않고, YouTube 를 통해 이용자의 정보를 수집하지도 않습니다.',
          '다만 영상 미리보기 이미지는 이용자의 브라우저가 YouTube 의 이미지 서버에서 직접 불러오므로, 영상 목록이 있는 화면을 열면 접속 IP 주소와 브라우저 정보가 Google 에 전달될 수 있습니다. 링크를 눌러 YouTube 로 이동한 뒤의 정보 처리는 Google 개인정보처리방침을 따릅니다.',
        ],
        links: [{ label: 'Google 개인정보처리방침', href: GOOGLE_PRIVACY_URL }],
      },
      {
        heading: '11. 안전성 확보 조치',
        paragraphs: [
          '비밀번호는 복호화할 수 없는 해시로만 저장하고, 로그인 토큰은 해시로 저장하며, 전송 구간은 HTTPS 로 암호화합니다. 관리자 접근은 별도 인증과 접근 기록으로 통제합니다.',
          '게시글·채팅에 올린 사진은 원본 그대로 저장되어 촬영 위치 같은 사진 속 정보가 함께 남을 수 있습니다. 위치가 드러나지 않기를 원하면 올리기 전에 사진의 위치 정보를 지워 주세요. 중고거래 상품 사진은 서비스가 다시 저장하면서 이런 정보를 지웁니다.',
        ],
      },
      {
        heading: '12. 개인정보 보호책임자와 문의',
        paragraphs: [
          `개인정보 보호책임자: 서비스 운영자 · 연락처: ${LEGAL_CONTACT}`,
          '개인정보 침해에 대한 신고·상담은 개인정보침해신고센터(privacy.kisa.or.kr, 118), 개인정보분쟁조정위원회(kopico.go.kr, 1833-6972)에 문의할 수 있습니다.',
        ],
      },
      {
        heading: '13. 방침의 변경',
        paragraphs: ['이 방침을 변경할 때는 시행 7일 전부터 서비스 공지로 알립니다. 수집 항목·목적·보유 기간이 이용자에게 불리하게 바뀌는 경우에는 30일 전에 알립니다.'],
      },
      {
        heading: '14. 언어',
        paragraphs: ['이 방침은 한국어로 작성됩니다. 이용자의 편의를 위해 다른 언어 번역본을 제공하며, 번역본과 한국어 원문의 내용이 다르면 한국어 원문이 우선합니다.'],
      },
    ],
  },
  en: {
    version: 'v4',
    updatedAt: '2026-11-28',
    title: 'Privacy Policy',
    intro:
      'Ovlo (the "Service") protects your personal data under the Personal Information Protection Act of the Republic of Korea and handles related requests promptly, as described below.',
    sections: [
      {
        heading: '1. Data we collect',
        paragraphs: ['We collect the following when you sign up for and use the Service.'],
        bullets: [
          'Required: email, password (stored only as a hash), nickname, name, hometown, home university, major, degree program and year',
          'Optional: profile photo, bio, date of birth, contact details, language skills, study-abroad experience',
          'With Google sign-in: Google account email, name and Google user identifier',
          'With Apple sign-in: Apple user identifier, the email Apple provides (an Apple relay address if you choose Hide My Email), and the name you give us',
          'With school email verification: the verified school email address, time of verification and the university',
          'With university international office guidance and consultation: your questions and the automated answers, the notices cited in answers, your ratings of answers, the consent text and version you agreed to when requesting a consultation, and messages exchanged in the consultation room with their translations',
          'With the marketplace: listings (title, description, price, area, university, meeting point, pickup dates), item photos, trade chats and report reasons',
          'With the calendar: personal events (title, description, place, time) and applications to manage a university calendar (department, contact email, message)',
          'With reports and blocks: what you reported, with the reason and description, and the members you blocked',
          'With the feedback form: your feedback, a reply email if you give one, the app version and browser information',
          'Generated while using the Service: posts, comments, chat messages, attachments, access logs (IP address, time, request path, member number), device and browser information',
          'Cookies: an httpOnly refresh token to keep you signed in, and your display language',
        ],
      },
      {
        heading: '2. Why we use it',
        paragraphs: ['We use personal data only for the purposes below.'],
        bullets: [
          'Identifying members, signing in, verifying identity (school email verification)',
          'Providing and operating community features such as boards, chat, the marketplace and the calendar',
          'Automated guidance based on university international office notices, connecting you with office staff, and explaining how to use the Service',
          'Translating notices, chat messages and marketplace listings; providing university and exchange information; adapting the display language',
          'Preventing abuse, handling reports, responding to security incidents, retaining access logs',
          'Answering inquiries and delivering notices',
        ],
      },
      {
        heading: '3. Features that use AI',
        paragraphs: [
          'The features below use an external AI service to produce answers or translations. We send the external AI service only the text of the question or message and the notices or help articles needed to answer it; we do not send identifiers such as your name, email or member number. Anything you type into the text itself is sent as written, so please do not include sensitive personal data.',
        ],
        bullets: [
          'International office guidance: students who have verified their school email get answers based on notices approved by their university\'s international office. Questions and answers are stored to check answer quality and to carry them into a consultation (retention in Section 5).',
          'Consultation with office staff: only when you consent on a separate screen, the conversation is forwarded to staff at your university\'s international office. Staff can see the questions and automated answers in the conversation you choose (up to 20 turns), your language, whether your school email is verified, the nickname and profile photo shown in the consultation room, and later messages in the room; the email that tells them about a new consultation includes your first question. Your email address and name are not shared. Messages in the room are translated into each side\'s language.',
          'Service help: when a signed-in member asks how to use the Service, we answer from help articles we wrote. The question is processed only while the answer is produced and is not stored.',
          'Translation: international office notices and marketplace listings are translated into your language.',
        ],
      },
      {
        heading: '4. What international office staff can see',
        paragraphs: [
          'International office staff are members of the Service invited by that university\'s office. Staff can see only consultations made to their own university and lose access to the consultation rooms once removed. The consultation list staff can download contains each consultation\'s status, times, language and first question, but not the student\'s email or name.',
        ],
      },
      {
        heading: '5. Retention',
        paragraphs: ['We delete personal data without delay once its purpose is fulfilled, except as follows.'],
        bullets: [
          'Account data: until you withdraw. On withdrawal we immediately de-identify account data and delete the files you uploaded (profile photo, attachments). Posts, comments, chat messages and marketplace listings are not deleted but hidden from other members. A withdrawn account cannot be restored.',
          'School email verification records: expire one year after verification; deleted on withdrawal.',
          'International office questions, answers and consultations: the text is deleted one year after the question was asked (for consultations, one year after the consultation was opened), or immediately on withdrawal. The time of use and the outcome remain for statistics.',
          'Service help questions: not stored. Only a count used to limit usage is kept, for at most one day.',
          'Calendar personal events and applications to manage a university calendar: deleted on withdrawal.',
          'Reports: one year after a report is resolved (for marketplace reports, one year after it is filed), we remove the reporting member, the reported target and the reason, keeping only the report type, outcome and dates for statistics.',
          'Blocks: until you unblock, or until either member withdraws',
          'Access logs (IP address, time, request path, member number): 3 months (Protection of Communications Secrets Act)',
          'IP addresses and emails used to limit sign-in, sign-up and search attempts: deleted automatically within 10 minutes',
          'Database backups: we keep separate backups for disaster recovery, so deleted data may remain in a backup until that backup is deleted in turn. Backups are used only for recovery.',
        ],
      },
      {
        heading: '6. Processors',
        paragraphs: ['We do not sell or provide your personal data to third parties. We entrust the following tasks to processors.'],
        bullets: [
          'AI3 Inc. (BizRouter): relaying AI requests — international office guidance, translation of consultation messages, notices and marketplace listings, and service help answers',
          'Anthropic PBC: producing AI answers and translations (via BizRouter)',
          'Google LLC: Google sign-in, and sending email through Gmail (school email verification codes, notifications to international office staff)',
          'Apple Inc.: Apple sign-in, and disconnecting Apple on withdrawal',
          'Cloudflare, Inc.: relaying connections to the Service (transport security) and forwarding contact email',
        ],
      },
      {
        heading: '7. Transfers outside Korea',
        paragraphs: [
          'To carry out the entrusted tasks, personal data is transferred abroad as follows. Each transfer happens whenever you use the feature concerned, over an encrypted network (HTTPS). The recipients keep the data according to their own privacy policies and terms of service.',
        ],
        bullets: [
          'Anthropic PBC (United States, privacy@anthropic.com): international office questions and earlier turns (up to 20), the notices and help articles used as grounds, and the text of messages, notices and marketplace listings to translate — to produce AI answers and translations',
          'Google LLC (United States): account data on Google sign-in, and the recipient address and content of the email we send (verification codes, the first question of a consultation) — sign-in and email delivery',
          'Apple Inc. (United States): Apple sign-in data and the token used to disconnect Apple on withdrawal — sign-in and disconnection',
          'Cloudflare, Inc. (United States): requests sent when you use the Service (including your IP address) and contact email — relaying connections and forwarding email',
          'How to refuse: if you do not use the AI features or Google or Apple sign-in, the related data is not transferred. Relaying connections through Cloudflare is required to use the Service, so refusing it means you cannot use the Service.',
        ],
      },
      {
        heading: '8. Your rights',
        paragraphs: [
          'You may at any time access, correct or delete your personal data or ask us to stop processing it, either directly on your profile screen or by contacting us below. You can withdraw your account from the profile screen, and you can ask to close an international office consultation at any time in the consultation room.',
        ],
      },
      {
        heading: '9. Cookies and local storage',
        paragraphs: [
          'We store a refresh token in an httpOnly cookie to keep you signed in, and your display language in a cookie and browser storage. We do not use advertising or tracking cookies. You may block cookies in your browser, but you may then not stay signed in.',
        ],
      },
      {
        heading: '10. External video links and YouTube API Services',
        paragraphs: [
          'We use YouTube API Services to build the list of YouTube video links shown on university report pages. In doing so we do not send your personal data to YouTube or Google, and we do not collect information about you through YouTube.',
          'Video preview images, however, are loaded by your browser directly from YouTube\'s image servers, so opening a page that lists videos may disclose your IP address and browser information to Google. Once you follow a link to YouTube, your data is handled under the Google Privacy Policy.',
        ],
        links: [{ label: 'Google Privacy Policy', href: GOOGLE_PRIVACY_URL }],
      },
      {
        heading: '11. Security measures',
        paragraphs: [
          'Passwords are stored only as irreversible hashes, sign-in tokens are stored as hashes, and all traffic is encrypted with HTTPS. Administrator access is controlled by separate authentication and access logging.',
          'Photos uploaded to posts and chats are stored as uploaded, so information inside the photo, such as where it was taken, may remain. If you do not want your location revealed, remove the photo\'s location data before uploading. Marketplace item photos are re-saved by the Service, which removes such information.',
        ],
      },
      {
        heading: '12. Data protection officer and contact',
        paragraphs: [
          `Data protection officer: the Service operator · Contact: ${LEGAL_CONTACT}`,
          'You may also contact the Korea Personal Information Infringement Report Center (privacy.kisa.or.kr, 118) or the Personal Information Dispute Mediation Committee (kopico.go.kr, 1833-6972).',
        ],
      },
      {
        heading: '13. Changes',
        paragraphs: ['We announce changes to this policy at least 7 days before they take effect, or 30 days before when the change is unfavorable to you (items collected, purposes or retention).'],
      },
      {
        heading: '14. Language',
        paragraphs: ['This policy is written in Korean. Translations into other languages are provided for your convenience; if a translation and the Korean original differ, the Korean original prevails.'],
      },
    ],
  },
  ja: PRIVACY_JA,
  zh: PRIVACY_ZH,
  es: PRIVACY_ES,
  fr: PRIVACY_FR,
  de: PRIVACY_DE,
  pt: PRIVACY_PT,
  ru: PRIVACY_RU,
}

export const TERMS_OF_SERVICE: LegalDocuments = {
  ko: {
    version: 'v4',
    updatedAt: '2026-11-28',
    title: '이용약관',
    intro: '이 약관은 Ovlo(이하 "서비스")의 이용 조건과 절차, 이용자와 서비스의 권리·의무를 정합니다.',
    sections: [
      {
        heading: '1. 서비스의 내용',
        paragraphs: [
          '서비스는 교환학생·유학생을 위한 대학 정보, 커뮤니티(게시판·채팅·중고거래·캘린더), 학교 이메일 인증, 대학 국제처가 승인한 자료에 기반한 자동 안내와 국제처 담당자 상담 연결, 서비스 사용법 안내를 제공합니다.',
        ],
      },
      {
        heading: '2. 계정',
        paragraphs: [
          '회원은 정확한 정보로 가입해야 하며 계정과 비밀번호를 스스로 관리할 책임이 있습니다. 타인의 정보로 가입하거나 계정을 양도·대여할 수 없습니다. 학교 이메일 인증은 그 이메일에 대한 접근 권한을 확인하는 것이며, 재학 여부나 작성 내용의 정확성을 보증하지 않습니다.',
        ],
      },
      {
        heading: '3. 정보의 성격과 책임의 한계',
        paragraphs: [
          '대학 리포트 등 서비스가 제공하는 정보는 공개 자료와 대학 공식 자료를 AI 로 정리한 참고 자료이며 출처와 기준 시점을 함께 표시합니다. 비자·학사·금액 등 중요한 사항은 반드시 해당 기관의 공식 안내로 확인해야 하며, 서비스는 정보의 정확성·최신성을 보증하지 않습니다.',
          '국제처 안내 기능은 해당 대학이 승인한 자료의 범위 안에서만 답하고, 개인 상황에 대한 판단(체류 자격 가능 여부 등)은 하지 않습니다. 안내는 대학의 공식 결정이나 법률 자문이 아닙니다.',
        ],
      },
      {
        heading: '4. 이용자의 게시물',
        paragraphs: [
          '회원이 올린 게시글·댓글·메시지·파일의 권리는 회원에게 있습니다. 회원은 서비스가 그 게시물을 서비스 안에서 보여 주고 저장·전송·번역하는 데 필요한 범위에서 이용하는 것을 허락합니다. 타인의 저작물·개인정보를 동의 없이 올려서는 안 됩니다.',
        ],
      },
      {
        heading: '5. 금지 행위',
        paragraphs: ['다음 행위는 금지되며, 위반 시 게시물 삭제·이용 제한·탈퇴 처리될 수 있습니다.'],
        bullets: [
          '타인 사칭, 허위 정보로 인증 시도, 계정 공유',
          '욕설·혐오·차별·괴롭힘, 음란물, 불법 정보 게시',
          '광고·스팸·영리 목적의 반복 게시(서비스가 표시한 제휴 안내는 제외)',
          '서비스의 보안을 우회하거나 자동화 수단으로 대량 수집하는 행위',
          '국제처 안내·상담 기능을 허위 신고·장난·위협에 쓰는 행위',
        ],
      },
      {
        heading: '6. 불쾌한 콘텐츠와 악성 이용자에 대한 무관용 원칙',
        paragraphs: [
          '서비스는 불쾌하거나 부적절한 콘텐츠와 다른 이용자를 괴롭히는 이용자를 일절 용인하지 않습니다. 회원가입 시 이 원칙에 동의해야 하며, 게시글·댓글·채팅 메시지·프로필 등 회원이 올리는 모든 콘텐츠에 적용됩니다. 특히 다음 콘텐츠는 금지됩니다.',
        ],
        bullets: [
          '스팸·광고·사기 또는 같은 내용의 반복 게시',
          '괴롭힘·따돌림·위협, 특정인을 겨냥한 모욕',
          '인종·민족·국적·종교·성별·성적 지향·장애 등에 근거한 혐오 표현과 차별',
          '음란물·성적으로 노골적인 콘텐츠, 미성년자를 성적으로 다루는 모든 콘텐츠',
          '폭력·자해를 조장하거나 미화하는 콘텐츠',
          '불법 상품·서비스 거래, 그 밖에 법령을 위반하는 콘텐츠',
          '타인의 연락처·주소·신분증 등 개인정보를 동의 없이 공개하는 행위',
        ],
      },
      {
        heading: '7. 신고·차단과 처리 기한',
        paragraphs: [
          '회원은 게시글·댓글·채팅 메시지·회원 프로필의 "신고" 기능으로 위 콘텐츠나 이용자를 신고할 수 있고, "차단" 기능으로 특정 회원을 차단할 수 있습니다. 차단하면 서로의 게시글과 댓글이 보이지 않고, 서로 메시지를 주고받을 수 없으며, 서로의 팔로우가 해제됩니다. 차단은 프로필의 "차단한 회원" 화면에서 언제든 해제할 수 있습니다.',
          '접수된 신고는 운영자가 24시간 이내에 검토하고, 위반이 확인되면 해당 콘텐츠를 삭제하며, 위반한 이용자의 이용을 제한하거나 계정을 정지·탈퇴 처리합니다. 허위 신고나 신고 기능의 남용도 이용 제한 사유가 됩니다.',
        ],
      },
      {
        heading: '8. 제휴 안내',
        paragraphs: [
          '서비스는 유심·보험·숙소·픽업 등 정착에 필요한 외부 서비스를 "제휴"로 표시해 안내할 수 있고, 그 이용으로 수수료를 받을 수 있습니다. 제휴 표시는 항상 붙이며 국제처 승인 안내 본문에는 넣지 않습니다. 외부 서비스의 계약과 책임은 해당 사업자에게 있습니다.',
        ],
      },
      {
        heading: '9. 외부 영상 링크(YouTube)',
        paragraphs: [
          '서비스는 대학 리포트 화면에 그 대학과 관련된 YouTube 공개 영상의 링크를 제목·채널명·미리보기 이미지와 함께 보여 줍니다. 영상을 서비스에 저장하거나 서비스 안에서 재생하지 않으며, 링크를 누르면 YouTube 로 이동합니다. 이 기능은 YouTube API 서비스를 사용하며, 이 기능을 이용하는 이용자는 YouTube 서비스 약관에도 동의하는 것으로 봅니다.',
          `영상의 내용과 권리는 각 영상의 게시자에게 있고, 서비스는 영상의 내용을 보증하지 않습니다. 삭제되거나 비공개로 바뀐 영상의 링크는 주기적으로 확인해 내립니다. 영상 게시자나 영상에 등장하는 분이 링크를 내려 달라고 요청하면 지체 없이 내립니다. 요청: ${LEGAL_CONTACT}`,
        ],
        links: [{ label: 'YouTube 서비스 약관', href: YOUTUBE_TERMS_URL }],
      },
      {
        heading: '10. 서비스의 변경·중단',
        paragraphs: [
          '서비스는 기능을 추가·변경·중단할 수 있으며, 중요한 변경은 사전에 공지합니다. 천재지변·시스템 장애 등 불가피한 사유로 일시 중단될 수 있습니다.',
        ],
      },
      {
        heading: '11. 계약 해지',
        paragraphs: ['회원은 언제든지 프로필 화면에서 탈퇴할 수 있습니다. 탈퇴 시 개인정보는 개인정보처리방침에 따라 처리됩니다.'],
      },
      {
        heading: '12. 준거법과 분쟁',
        paragraphs: ['이 약관은 대한민국 법을 따르며, 분쟁은 민사소송법에 따른 관할 법원에서 다룹니다.'],
      },
      {
        heading: '13. 약관의 변경',
        paragraphs: [`약관을 바꿀 때는 시행 7일 전(이용자에게 불리한 변경은 30일 전)에 공지합니다. 문의: ${LEGAL_CONTACT}`],
      },
      {
        heading: '14. 언어',
        paragraphs: ['이 약관은 한국어로 작성됩니다. 이용자의 편의를 위해 다른 언어 번역본을 제공하며, 번역본과 한국어 원문의 내용이 다르면 한국어 원문이 우선합니다.'],
      },
    ],
  },
  en: {
    version: 'v4',
    updatedAt: '2026-11-28',
    title: 'Terms of Service',
    intro: 'These terms set out the conditions for using Ovlo (the "Service") and the rights and obligations of users and the Service.',
    sections: [
      {
        heading: '1. The Service',
        paragraphs: [
          'The Service provides university information for exchange and international students, community features (boards, chat, marketplace and calendar), school email verification, automated guidance based on materials approved by university international offices with handoff to office staff, and help on how to use the Service.',
        ],
      },
      {
        heading: '2. Accounts',
        paragraphs: [
          'You must sign up with accurate information and are responsible for your account and password. You may not sign up with someone else\'s information or transfer or lend your account. School email verification confirms access to that email address; it does not guarantee enrollment status or the accuracy of what you post.',
        ],
      },
      {
        heading: '3. Nature of the information and limits of liability',
        paragraphs: [
          'University reports and similar content are reference material compiled with AI from public sources and official university materials, shown with sources and an as-of date. Always confirm important matters such as visas, academics and costs with the relevant institution. The Service does not guarantee accuracy or currency.',
          'The international office guidance feature answers only within materials approved by that university and does not judge personal circumstances (such as visa eligibility). Guidance is not an official university decision or legal advice.',
        ],
      },
      {
        heading: '4. Your content',
        paragraphs: [
          'You own the posts, comments, messages and files you upload. You grant the Service the right to display, store, transmit and translate them to the extent needed to operate the Service. Do not upload others\' copyrighted works or personal data without consent.',
        ],
      },
      {
        heading: '5. Prohibited conduct',
        paragraphs: ['The following is prohibited and may lead to content removal, restriction or termination.'],
        bullets: [
          'Impersonation, attempting verification with false information, sharing accounts',
          'Abuse, hate, discrimination, harassment, obscene or illegal content',
          'Advertising, spam or repeated commercial posting (partner notices shown by the Service excepted)',
          'Circumventing security or bulk collection by automated means',
          'Using the guidance or consultation feature for false reports, pranks or threats',
        ],
      },
      {
        heading: '6. Zero tolerance for objectionable content and abusive users',
        paragraphs: [
          'The Service has zero tolerance for objectionable content and for users who abuse others. You must agree to this when you sign up, and it applies to everything members post, including posts, comments, chat messages and profiles. In particular, the following content is prohibited:',
        ],
        bullets: [
          'Spam, advertising, scams or repeated posting of the same content',
          'Harassment, bullying, threats or insults aimed at a person',
          'Hate speech or discrimination based on race, ethnicity, nationality, religion, sex, sexual orientation, disability or similar characteristics',
          'Pornographic or sexually explicit content, and any content that sexualizes minors',
          'Content that promotes or glorifies violence or self-harm',
          'Trading in illegal goods or services, or other content that breaks the law',
          'Sharing other people\'s contact details, addresses, ID documents or other personal data without consent',
        ],
      },
      {
        heading: '7. Reporting, blocking and review',
        paragraphs: [
          'You can report such content or users with the "Report" option on posts, comments, chat messages and member profiles, and you can block a member with the "Block" option. When you block someone, you no longer see each other\'s posts and comments, you cannot message each other, and any follows between you are removed. You can unblock at any time from "Blocked members" in your profile.',
          'Our moderators review reports within 24 hours. When a violation is confirmed, we remove the content and restrict, suspend or terminate the offending account. False reports and misuse of the reporting tools may also lead to restrictions.',
        ],
      },
      {
        heading: '8. Partner notices',
        paragraphs: [
          'The Service may present external settlement services such as SIM cards, insurance, housing and airport pickup labeled as "Partner", and may receive a fee when you use them. The label is always shown and partner links never appear inside office-approved guidance. Contracts with and responsibility for external services rest with those providers.',
        ],
      },
      {
        heading: '9. External video links (YouTube)',
        paragraphs: [
          'On university report pages the Service shows links to public YouTube videos related to that university, with their title, channel name and preview image. Videos are not stored or played within the Service; following a link takes you to YouTube. This feature uses YouTube API Services, and by using it you also agree to be bound by the YouTube Terms of Service.',
          `The content of and rights in each video belong to its publisher, and the Service does not guarantee video content. Links to videos that are deleted or made private are checked periodically and removed. If a video's publisher or a person appearing in it asks us to remove a link, we remove it without delay. Requests: ${LEGAL_CONTACT}`,
        ],
        links: [{ label: 'YouTube Terms of Service', href: YOUTUBE_TERMS_URL }],
      },
      {
        heading: '10. Changes and interruptions',
        paragraphs: ['The Service may add, change or discontinue features, with advance notice for significant changes. Service may be interrupted for unavoidable reasons such as disasters or system failures.'],
      },
      {
        heading: '11. Termination',
        paragraphs: ['You may withdraw at any time from your profile screen. Personal data is then handled under the Privacy Policy.'],
      },
      {
        heading: '12. Governing law',
        paragraphs: ['These terms are governed by the laws of the Republic of Korea; disputes are heard by the competent court under the Civil Procedure Act.'],
      },
      {
        heading: '13. Changes to these terms',
        paragraphs: [`We announce changes at least 7 days in advance (30 days for changes unfavorable to users). Contact: ${LEGAL_CONTACT}`],
      },
      {
        heading: '14. Language',
        paragraphs: ['These terms are written in Korean. Translations into other languages are provided for your convenience; if a translation and the Korean original differ, the Korean original prevails.'],
      },
    ],
  },
  ja: TERMS_JA,
  zh: TERMS_ZH,
  es: TERMS_ES,
  fr: TERMS_FR,
  de: TERMS_DE,
  pt: TERMS_PT,
  ru: TERMS_RU,
}

/**
 * 상담 이관 동의 문구. 서버 정본은 ConsultationConsent.java — 같은 version 의 문구를 맞춘다. 학생이 "담당자에게 전달"을 누를 때 보여 주고, 서버는 version 과 함께 동의 사실을 기록한다.
 * 문구를 바꾸면 version 을 올린다(옛 동의 기록은 옛 version 으로 남는다).
 */
export const CONSULTATION_CONSENT = {
  version: 'v3',
  ko: {
    title: '국제처 담당자에게 전달하기',
    body: [
      '아래 내용이 소속 대학 국제처 담당자에게 전달됩니다.',
    ],
    shared: ['선택한 대화의 모든 질문과 자동 안내 답변(최대 20회)', '사용 언어', '학교 이메일 인증 여부(이메일 주소 자체는 전달하지 않음)', '상담방에 표시되는 내 닉네임과 프로필 사진', '이후 상담방에서 주고받는 메시지'],
    notes: [
      '담당자에게는 새 상담이 왔다는 알림 메일에 첫 질문이 함께 전달됩니다.',
      '주고받는 메시지는 외부 AI 서비스로 번역되며, 이름·이메일 같은 식별 정보는 번역에 보내지 않습니다. 원문도 볼 수 있습니다.',
      '상담 내용은 상담을 연 날부터 1년이 지나면 본문을 삭제합니다. 탈퇴하면 바로 삭제합니다. 상담 종료 전에는 언제든 상담방에서 종료를 요청할 수 있습니다.',
      '동의하지 않아도 자동 안내는 계속 이용할 수 있습니다.',
    ],
    agree: '동의하고 전달',
    decline: '전달하지 않기',
  },
  en: {
    title: 'Forward to your international office',
    body: ['The following will be shared with staff at your university\'s international office.'],
    shared: ['All questions and automated answers in this conversation (up to 20 turns)', 'Your language', 'Whether your school email is verified (the address itself is not shared)', 'Your nickname and profile photo as shown in the consultation room', 'Messages exchanged in the consultation room afterwards'],
    notes: [
      'The email that tells staff about a new consultation includes your first question.',
      'Messages are translated by an external AI service; identifiers such as your name or email are not sent for translation. The original is also available.',
      'The consultation text is deleted one year after the consultation was opened, or immediately if you withdraw. You can ask to close the consultation at any time.',
      'You can keep using automated guidance without consenting.',
    ],
    agree: 'Agree and forward',
    decline: 'Do not forward',
  },
} as const
