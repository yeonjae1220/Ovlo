import apiClient from '../utils/axios'

/** 백엔드 domain/office 와 1:1. */
export type OfficeRole = 'OFFICE_ADMIN' | 'OFFICE_STAFF'
export type OfficeStatus = 'PILOT' | 'ACTIVE' | 'SUSPENDED'
export type NoticeStatus = 'DRAFT' | 'APPROVED' | 'EXPIRED' | 'ARCHIVED'
export type NoticeCategory = 'HOUSING' | 'VISA_STAY' | 'INSURANCE' | 'ACADEMIC' | 'SCHOLARSHIP' | 'ARRIVAL' | 'LIFE' | 'OTHER'
export type NoticeLang = 'KO' | 'EN' | 'JA' | 'ZH' | 'DE' | 'FR' | 'VI'
export type TranslationStatus = 'MACHINE' | 'REVIEWED'

export const NOTICE_CATEGORIES: NoticeCategory[] = ['HOUSING', 'VISA_STAY', 'INSURANCE', 'ACADEMIC', 'SCHOLARSHIP', 'ARRIVAL', 'LIFE', 'OTHER']
export const NOTICE_LANGS: NoticeLang[] = ['KO', 'EN', 'JA', 'ZH', 'DE', 'FR', 'VI']

/** GET /office/me */
export interface MyOffice {
  officeId: number
  officeName: string
  universityId: number
  status: OfficeStatus
  /** 담당자면 역할, 학생이면 null */
  role: OfficeRole | null
  verifiedStudent: boolean
  pendingInvitation: boolean
}

export interface NoticeTranslation {
  lang: NoticeLang
  title: string
  body: string
  status: TranslationStatus
  stale: boolean
  generatedAt: string
}

/** 담당자용 상세 */
export interface Notice {
  id: number
  officeId: number
  category: NoticeCategory
  title: string
  body: string
  sourceLang: NoticeLang
  sourceUrl: string | null
  attachmentMediaIds: number[]
  term: string
  effectiveFrom: string
  expiresAt: string
  status: NoticeStatus
  approvedBy: string | null
  approvedAt: string | null
  version: number
  activeToday: boolean
  createdAt: string
  updatedAt: string
  translations: NoticeTranslation[]
  missingLangs: NoticeLang[]
}

/** 담당자용 목록 행 */
export interface NoticeSummary {
  id: number
  category: NoticeCategory
  title: string
  term: string
  effectiveFrom: string
  expiresAt: string
  status: NoticeStatus
  version: number
  translatedLangCount: number
  missingLangCount: number
  updatedAt: string
}

/** 학생용 */
export interface StudentNotice {
  id: number
  category: NoticeCategory
  title: string
  body: string
  lang: NoticeLang
  machineTranslated: boolean
  sourceUrl: string | null
  attachmentMediaIds: number[]
  term: string
  effectiveFrom: string
  expiresAt: string
}

export type InquiryOutcome = 'ANSWERED' | 'NO_GROUNDS' | 'ESCALATED'

export interface Citation {
  index: number
  noticeId: number
  title: string
  sourceUrl: string | null
  expiresAt: string | null
}

/** POST /office/{id}/inquiries */
export interface AskResult {
  inquiryId: number
  outcome: InquiryOutcome
  lang: NoticeLang
  /** [N] 인용 번호 포함. NO_GROUNDS 면 null */
  answerText: string | null
  citations: Citation[]
  canEscalate: boolean
}

/** 담당자 문의 로그 행 (학생 식별 정보 없음) */
export interface InquiryLog {
  id: number
  lang: NoticeLang
  question: string
  outcome: InquiryOutcome
  answerText: string | null
  citedNoticeIds: number[]
  candidateCount: number
  askedAt: string
  latencyMs: number
  modelId: string | null
  helpful: boolean | null
  feedbackReason: string | null
}

export interface NoticeRequest {
  category: NoticeCategory
  title: string
  body: string
  sourceUrl?: string | null
  attachmentMediaIds?: number[]
  term: string
  effectiveFrom: string
  expiresAt: string
}

interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

export const officeApi = {
  me: () => apiClient.get<MyOffice>('/office/me').then((r) => r.data),

  acceptInvitation: () => apiClient.post('/office/invitations/accept').then((r) => r.data),

  // ── 담당자 ───────────────────────────────────────────────────────────────
  listNotices: (officeId: number, status?: NoticeStatus | '', category?: NoticeCategory | '', page = 0, size = 20) =>
    apiClient
      .get<PageResult<NoticeSummary>>(`/office-admin/${officeId}/notices`, {
        params: { status: status || undefined, category: category || undefined, page, size },
      })
      .then((r) => r.data),

  getNotice: (officeId: number, noticeId: number) =>
    apiClient.get<Notice>(`/office-admin/${officeId}/notices/${noticeId}`).then((r) => r.data),

  registerNotice: (officeId: number, req: NoticeRequest) =>
    apiClient.post<Notice>(`/office-admin/${officeId}/notices`, req).then((r) => r.data),

  updateNotice: (officeId: number, noticeId: number, req: NoticeRequest) =>
    apiClient.put<Notice>(`/office-admin/${officeId}/notices/${noticeId}`, req).then((r) => r.data),

  approveNotice: (officeId: number, noticeId: number) =>
    apiClient.post<Notice>(`/office-admin/${officeId}/notices/${noticeId}/approve`).then((r) => r.data),

  archiveNotice: (officeId: number, noticeId: number) =>
    apiClient.post(`/office-admin/${officeId}/notices/${noticeId}/archive`),

  reviewTranslation: (officeId: number, noticeId: number, lang: NoticeLang) =>
    apiClient.post(`/office-admin/${officeId}/notices/${noticeId}/translations/${lang.toLowerCase()}/review`),

  retranslateNotice: (officeId: number, noticeId: number) =>
    apiClient.post<Notice>(`/office-admin/${officeId}/notices/${noticeId}/retranslate`).then((r) => r.data),

  listInquiries: (officeId: number, outcome?: InquiryOutcome | '', page = 0, size = 20) =>
    apiClient
      .get<PageResult<InquiryLog>>(`/office-admin/${officeId}/inquiries`, {
        params: { outcome: outcome || undefined, page, size },
      })
      .then((r) => r.data),

  // ── 학생 ─────────────────────────────────────────────────────────────────
  ask: (officeId: number, question: string, lang: string) =>
    apiClient.post<AskResult>(`/office/${officeId}/inquiries`, { question, lang }).then((r) => r.data),

  inquiryFeedback: (officeId: number, inquiryId: number, helpful: boolean, reason?: string) =>
    apiClient.post(`/office/${officeId}/inquiries/${inquiryId}/feedback`, { helpful, reason: reason || null }),

  listStudentNotices: (officeId: number, lang: string, category?: NoticeCategory | '') =>
    apiClient
      .get<StudentNotice[]>(`/office/${officeId}/notices`, { params: { lang, category: category || undefined } })
      .then((r) => r.data),

  getStudentNotice: (officeId: number, noticeId: number, lang: string) =>
    apiClient.get<StudentNotice>(`/office/${officeId}/notices/${noticeId}`, { params: { lang } }).then((r) => r.data),
}
