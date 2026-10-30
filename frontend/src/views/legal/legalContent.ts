/**
 * 법적 문서 원문 (ko·en). UI 언어가 ko 면 한국어, 그 외는 영어를 보여 준다.
 * 문서 버전은 서버에 기록되는 동의 기록과 맞춰야 하므로 바꿀 때 version 과 updatedAt 을 같이 올린다.
 *
 * ⚠️ 대외 연락처가 아직 정해지지 않았다. CONTACT_PLACEHOLDER 가 남아 있는 동안은 회원 대규모 모집·파일럿을 시작하지 않는다.
 */

export const LEGAL_CONTACT_PLACEHOLDER = '[연락처 확정 후 기재 / to be announced]'

export type LegalSection = { heading: string; paragraphs: string[]; bullets?: string[] }
export type LegalDocument = {
  version: string
  updatedAt: string
  title: string
  intro: string
  sections: LegalSection[]
}

export const PRIVACY_POLICY: Record<'ko' | 'en', LegalDocument> = {
  ko: {
    version: 'v1',
    updatedAt: '2026-10-30',
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
          '소셜 로그인(Google) 이용 시: Google 계정 이메일, 이름, Google 사용자 식별자',
          '학교 이메일 인증 시: 인증한 학교 이메일 주소, 인증 시각, 인증 대학',
          '서비스 이용 중 생성되는 정보: 게시글·댓글·채팅 메시지·첨부 파일, 접속 기록(IP 주소, 접속 시각, 요청 경로), 기기·브라우저 정보',
          '쿠키: 로그인 유지용 리프레시 토큰(httpOnly), 화면 언어 설정',
        ],
      },
      {
        heading: '2. 개인정보의 수집·이용 목적',
        paragraphs: ['수집한 개인정보는 다음 목적에만 이용합니다.'],
        bullets: [
          '회원 식별, 로그인, 본인 확인(학교 이메일 인증)',
          '게시판·채팅 등 커뮤니티 기능 제공과 운영',
          '대학·교환학생 정보 제공, 화면 언어 맞춤',
          '부정 이용 방지, 보안 사고 대응, 접속 기록 보관',
          '문의 응대와 공지 전달',
        ],
      },
      {
        heading: '3. 대학 국제처 안내·상담 기능(준비 중)',
        paragraphs: [
          '서비스는 대학 국제처가 승인한 자료를 바탕으로 유학생에게 안내하고, 이용자가 동의한 경우에 한해 문의 내용을 해당 대학 국제처 담당자에게 전달하는 기능을 준비하고 있습니다.',
          '이 기능이 열리면 ① 이용자가 입력한 질문과 자동 답변은 서비스 품질 측정을 위해 기록되고, ② 질문에 답하기 위해 질문 본문이 외부 AI 서비스 제공자에게 처리 위탁될 수 있으며(이름·이메일 등 식별 정보는 보내지 않습니다), ③ 담당자에게 전달되는 내용과 범위는 전달 시점에 별도 동의 화면에서 안내합니다. 기능이 열리기 전에 이 방침을 갱신해 구체적인 수탁자와 보관 기간을 밝힙니다.',
        ],
      },
      {
        heading: '4. 보유·이용 기간',
        paragraphs: ['개인정보는 수집·이용 목적이 달성되면 지체 없이 파기합니다. 다만 다음은 명시한 기간 동안 보관합니다.'],
        bullets: [
          '회원 정보: 탈퇴 시까지. 탈퇴 즉시 계정 정보는 삭제하거나 식별할 수 없게 처리하고, 작성한 게시글·댓글은 비공개 처리합니다.',
          '접속 기록(IP, 접속 시각): 3개월(「통신비밀보호법」)',
          '학교 이메일 인증 기록: 인증일로부터 1년이 지나면 만료 처리하며, 탈퇴 시 삭제합니다.',
          '부정 이용 기록: 재발 방지를 위해 1년',
        ],
      },
      {
        heading: '5. 제3자 제공 및 처리 위탁',
        paragraphs: ['서비스는 이용자의 개인정보를 제3자에게 판매하거나 제공하지 않습니다. 서비스 운영을 위해 다음 업무를 위탁합니다.'],
        bullets: [
          'Google LLC: 소셜 로그인(Google OAuth) 인증',
          '이메일 발송 사업자: 학교 이메일 인증 코드 발송',
          '외부 AI 서비스 제공자: 국제처 안내 기능(3항)이 열릴 때 질문 본문 처리. 수탁자는 기능 개시 전에 이 방침에 적습니다.',
        ],
      },
      {
        heading: '6. 이용자의 권리',
        paragraphs: [
          '이용자는 언제든지 자신의 개인정보를 열람·정정·삭제하거나 처리 정지를 요구할 수 있으며, 프로필 화면에서 직접 수정하거나 아래 연락처로 요청할 수 있습니다. 회원 탈퇴는 프로필 화면에서 할 수 있습니다.',
        ],
      },
      {
        heading: '7. 쿠키와 로컬 저장소',
        paragraphs: [
          '로그인 유지를 위해 httpOnly 쿠키에 리프레시 토큰을 저장하고, 화면 언어 설정을 쿠키와 브라우저 저장소에 둡니다. 광고·추적 목적의 쿠키는 사용하지 않습니다. 브라우저 설정으로 쿠키를 거부할 수 있으나 로그인 유지가 되지 않을 수 있습니다.',
        ],
      },
      {
        heading: '8. 안전성 확보 조치',
        paragraphs: [
          '비밀번호는 복호화할 수 없는 해시로만 저장하고, 로그인 토큰은 해시로 저장하며, 전송 구간은 HTTPS 로 암호화합니다. 관리자 접근은 별도 인증과 접근 기록으로 통제합니다.',
        ],
      },
      {
        heading: '9. 개인정보 보호책임자와 문의',
        paragraphs: [
          `개인정보 보호책임자: 서비스 운영자 · 연락처: ${LEGAL_CONTACT_PLACEHOLDER}`,
          '개인정보 침해에 대한 신고·상담은 개인정보침해신고센터(privacy.kisa.or.kr, 118), 개인정보분쟁조정위원회(kopico.go.kr, 1833-6972)에 문의할 수 있습니다.',
        ],
      },
      {
        heading: '10. 방침의 변경',
        paragraphs: ['이 방침을 변경할 때는 시행 7일 전부터 서비스 공지로 알립니다. 수집 항목·목적·보유 기간이 이용자에게 불리하게 바뀌는 경우에는 30일 전에 알립니다.'],
      },
    ],
  },
  en: {
    version: 'v1',
    updatedAt: '2026-10-30',
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
          'With school email verification: the verified school email address, time of verification and the university',
          'Generated while using the Service: posts, comments, chat messages, attachments, access logs (IP address, time, request path), device and browser information',
          'Cookies: an httpOnly refresh token to keep you signed in, and your display language',
        ],
      },
      {
        heading: '2. Why we use it',
        paragraphs: ['We use personal data only for the purposes below.'],
        bullets: [
          'Identifying members, signing in, verifying identity (school email verification)',
          'Providing and operating community features such as boards and chat',
          'Providing university and exchange information, adapting the display language',
          'Preventing abuse, responding to security incidents, retaining access logs',
          'Answering inquiries and delivering notices',
        ],
      },
      {
        heading: '3. University international office guidance and consultation (in preparation)',
        paragraphs: [
          'We are preparing a feature that answers international students based on materials approved by their university\'s international office and, only with your consent, forwards your inquiry to that office\'s staff.',
          'When it launches: (1) your questions and the automated answers are recorded to measure service quality; (2) the text of your question may be processed by an external AI service provider to produce an answer (no name, email or other identifiers are sent); (3) what is shared with staff, and how far, is explained on a separate consent screen at the time of forwarding. We will update this policy with the specific processors and retention periods before the feature opens.',
        ],
      },
      {
        heading: '4. Retention',
        paragraphs: ['We delete personal data without delay once its purpose is fulfilled, except as follows.'],
        bullets: [
          'Account data: until you withdraw. On withdrawal we delete or de-identify account data and hide your posts and comments.',
          'Access logs (IP, time): 3 months (Protection of Communications Secrets Act)',
          'School email verification records: expire one year after verification; deleted on withdrawal.',
          'Abuse records: 1 year, to prevent recurrence',
        ],
      },
      {
        heading: '5. Third parties and processors',
        paragraphs: ['We do not sell or provide your personal data to third parties. We entrust the following tasks to processors.'],
        bullets: [
          'Google LLC: Google OAuth sign-in',
          'Email delivery provider: sending school email verification codes',
          'External AI service provider: processing question text when the guidance feature (Section 3) opens. The processor will be named here before launch.',
        ],
      },
      {
        heading: '6. Your rights',
        paragraphs: [
          'You may at any time access, correct or delete your personal data or ask us to stop processing it, either directly on your profile screen or by contacting us below. You can withdraw your account from the profile screen.',
        ],
      },
      {
        heading: '7. Cookies and local storage',
        paragraphs: [
          'We store a refresh token in an httpOnly cookie to keep you signed in, and your display language in a cookie and browser storage. We do not use advertising or tracking cookies. You may block cookies in your browser, but you may then not stay signed in.',
        ],
      },
      {
        heading: '8. Security measures',
        paragraphs: [
          'Passwords are stored only as irreversible hashes, sign-in tokens are stored as hashes, and all traffic is encrypted with HTTPS. Administrator access is controlled by separate authentication and access logging.',
        ],
      },
      {
        heading: '9. Data protection officer and contact',
        paragraphs: [
          `Data protection officer: the Service operator · Contact: ${LEGAL_CONTACT_PLACEHOLDER}`,
          'You may also contact the Korea Personal Information Infringement Report Center (privacy.kisa.or.kr, 118) or the Personal Information Dispute Mediation Committee (kopico.go.kr, 1833-6972).',
        ],
      },
      {
        heading: '10. Changes',
        paragraphs: ['We announce changes to this policy at least 7 days before they take effect, or 30 days before when the change is unfavorable to you (items collected, purposes or retention).'],
      },
    ],
  },
}

export const TERMS_OF_SERVICE: Record<'ko' | 'en', LegalDocument> = {
  ko: {
    version: 'v1',
    updatedAt: '2026-10-30',
    title: '이용약관',
    intro: '이 약관은 Ovlo(이하 "서비스")의 이용 조건과 절차, 이용자와 서비스의 권리·의무를 정합니다.',
    sections: [
      {
        heading: '1. 서비스의 내용',
        paragraphs: [
          '서비스는 교환학생·유학생을 위한 대학 정보, 커뮤니티(게시판·채팅), 학교 이메일 인증, 그리고 대학 국제처가 승인한 자료에 기반한 안내·상담 연결 기능(준비 중)을 제공합니다.',
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
        heading: '6. 제휴 안내',
        paragraphs: [
          '서비스는 유심·보험·숙소·픽업 등 정착에 필요한 외부 서비스를 "제휴"로 표시해 안내할 수 있고, 그 이용으로 수수료를 받을 수 있습니다. 제휴 표시는 항상 붙이며 국제처 승인 안내 본문에는 넣지 않습니다. 외부 서비스의 계약과 책임은 해당 사업자에게 있습니다.',
        ],
      },
      {
        heading: '7. 서비스의 변경·중단',
        paragraphs: [
          '서비스는 기능을 추가·변경·중단할 수 있으며, 중요한 변경은 사전에 공지합니다. 천재지변·시스템 장애 등 불가피한 사유로 일시 중단될 수 있습니다.',
        ],
      },
      {
        heading: '8. 계약 해지',
        paragraphs: ['회원은 언제든지 프로필 화면에서 탈퇴할 수 있습니다. 탈퇴 시 개인정보는 개인정보처리방침에 따라 처리됩니다.'],
      },
      {
        heading: '9. 준거법과 분쟁',
        paragraphs: ['이 약관은 대한민국 법을 따르며, 분쟁은 민사소송법에 따른 관할 법원에서 다룹니다.'],
      },
      {
        heading: '10. 약관의 변경',
        paragraphs: [`약관을 바꿀 때는 시행 7일 전(이용자에게 불리한 변경은 30일 전)에 공지합니다. 문의: ${LEGAL_CONTACT_PLACEHOLDER}`],
      },
    ],
  },
  en: {
    version: 'v1',
    updatedAt: '2026-10-30',
    title: 'Terms of Service',
    intro: 'These terms set out the conditions for using Ovlo (the "Service") and the rights and obligations of users and the Service.',
    sections: [
      {
        heading: '1. The Service',
        paragraphs: [
          'The Service provides university information for exchange and international students, community features (boards and chat), school email verification, and, in preparation, guidance and consultation handoff based on materials approved by university international offices.',
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
        heading: '6. Partner notices',
        paragraphs: [
          'The Service may present external settlement services such as SIM cards, insurance, housing and airport pickup labeled as "Partner", and may receive a fee when you use them. The label is always shown and partner links never appear inside office-approved guidance. Contracts with and responsibility for external services rest with those providers.',
        ],
      },
      {
        heading: '7. Changes and interruptions',
        paragraphs: ['The Service may add, change or discontinue features, with advance notice for significant changes. Service may be interrupted for unavoidable reasons such as disasters or system failures.'],
      },
      {
        heading: '8. Termination',
        paragraphs: ['You may withdraw at any time from your profile screen. Personal data is then handled under the Privacy Policy.'],
      },
      {
        heading: '9. Governing law',
        paragraphs: ['These terms are governed by the laws of the Republic of Korea; disputes are heard by the competent court under the Civil Procedure Act.'],
      },
      {
        heading: '10. Changes to these terms',
        paragraphs: [`We announce changes at least 7 days in advance (30 days for changes unfavorable to users). Contact: ${LEGAL_CONTACT_PLACEHOLDER}`],
      },
    ],
  },
}

/**
 * 상담 이관 동의 문구 v1. 학생이 "담당자에게 전달"을 누를 때 보여 주고, 서버는 version 과 함께 동의 사실을 기록한다.
 * 문구를 바꾸면 version 을 올린다(옛 동의 기록은 옛 version 으로 남는다).
 */
export const CONSULTATION_CONSENT = {
  version: 'v1',
  ko: {
    title: '국제처 담당자에게 전달하기',
    body: [
      '아래 내용이 소속 대학 국제처 담당자에게 전달됩니다.',
    ],
    shared: ['지금 질문한 내용과 자동 안내 답변', '사용 언어', '학교 이메일 인증 여부(이메일 주소 자체는 전달하지 않음)', '이후 상담방에서 주고받는 메시지'],
    notes: [
      '담당자의 답변은 내 언어로 번역되어 표시되며 원문도 볼 수 있습니다.',
      '상담 내용은 학기 종료 후 1년까지 보관한 뒤 본문을 삭제합니다. 상담 종료 전에는 언제든 상담방에서 종료를 요청할 수 있습니다.',
      '동의하지 않아도 자동 안내는 계속 이용할 수 있습니다.',
    ],
    agree: '동의하고 전달',
    decline: '전달하지 않기',
  },
  en: {
    title: 'Forward to your international office',
    body: ['The following will be shared with staff at your university\'s international office.'],
    shared: ['Your question and the automated answer', 'Your language', 'Whether your school email is verified (the address itself is not shared)', 'Messages exchanged in the consultation room afterwards'],
    notes: [
      'Staff replies are shown translated into your language; the original is also available.',
      'Consultation content is kept until one year after the semester ends, then the text is deleted. You can ask to close the consultation at any time.',
      'You can keep using automated guidance without consenting.',
    ],
    agree: 'Agree and forward',
    decline: 'Do not forward',
  },
} as const
