import apiClient from '../utils/axios'

export type ReportTargetType = 'POST' | 'COMMENT' | 'CHAT_MESSAGE' | 'MEMBER'

export const REPORT_REASONS = [
  'SPAM',
  'HARASSMENT',
  'HATE',
  'SEXUAL',
  'VIOLENCE',
  'ILLEGAL',
  'PERSONAL_INFO',
  'OTHER',
] as const
export type ReportReason = (typeof REPORT_REASONS)[number]

/** 서버 ContentReport.MAX_DETAIL_LENGTH 와 같아야 한다. */
export const REPORT_DETAIL_MAX_LENGTH = 1000

export interface ReportRequest {
  targetType: ReportTargetType
  targetId: number
  reason: ReportReason
  detail?: string
}

export interface BlockedMember {
  memberId: number
  nickname: string
}

export interface BlockedMemberPage {
  content: BlockedMember[]
  nextBefore: number | null
}

export interface BlockStatus {
  blockedByMe: boolean
}

export const moderationApi = {
  report: (req: ReportRequest): Promise<void> =>
    apiClient.post('/reports', req).then(() => undefined),

  blockedMembers: (before?: number): Promise<BlockedMemberPage> =>
    apiClient.get<BlockedMemberPage>('/blocks', { params: { before } }).then((r) => r.data),

  blockStatus: (memberId: number): Promise<BlockStatus> =>
    apiClient.get<BlockStatus>(`/blocks/${memberId}`).then((r) => r.data),

  block: (memberId: number): Promise<void> =>
    apiClient.put(`/blocks/${memberId}`).then(() => undefined),

  unblock: (memberId: number): Promise<void> =>
    apiClient.delete(`/blocks/${memberId}`).then(() => undefined),
}

/** 응답이 있는 실패의 HTTP 상태. 네트워크 오류 등 응답이 없으면 null. */
export function httpStatusOf(error: unknown): number | null {
  const status = (error as { response?: { status?: unknown } } | null)?.response?.status
  return typeof status === 'number' ? status : null
}

/** 신고 실패를 사용자에게 보여 줄 메시지 키로. 알 수 없는 실패도 반드시 실패 문구로 남긴다. */
export function reportErrorKey(error: unknown): string {
  switch (httpStatusOf(error)) {
    case 400: return 'moderation.report.error.invalid'
    case 404: return 'moderation.report.error.notFound'
    case 429: return 'moderation.report.error.rateLimited'
    default: return 'moderation.report.error.generic'
  }
}
