import apiClient from '../utils/axios'
import type {
  Member,
  MemberSummary,
  NicknameAvailability,
  RegisterMemberRequest,
  UpdateMemberProfileRequest,
  CompleteOnboardingRequest,
  WithdrawMemberRequest,
} from '../types'

export const memberApi = {
  register: (req: RegisterMemberRequest) =>
    apiClient.post<Member>('/members', req).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<Member>(`/members/${id}`).then((r) => r.data),

  search: (nickname: string) =>
    apiClient.get<MemberSummary[]>('/members/search', { params: { nickname } }).then((r) => r.data),

  checkNickname: (nickname: string) =>
    apiClient.get<NicknameAvailability>('/members/check-nickname', { params: { nickname } }).then((r) => r.data),

  updateProfile: (id: string, req: UpdateMemberProfileRequest) =>
    apiClient.put<Member>(`/members/${id}`, req).then((r) => r.data),

  updateProfileImage: (id: string, mediaId: string) =>
    apiClient.patch<Member>(`/members/${id}/profile-image`, { mediaId }).then((r) => r.data),

  /** Apple 로그인 계정은 apple 재인증을 함께 보내야 한다 — 서버가 Apple 토큰을 폐기한 뒤 탈퇴시킨다. */
  withdraw: (id: string, body?: WithdrawMemberRequest) =>
    apiClient.delete(`/members/${id}`, body ? { data: body } : undefined),

  completeOnboarding: (req: CompleteOnboardingRequest) =>
    apiClient.patch<Member>('/members/me/onboarding', req).then((r) => r.data),
}
