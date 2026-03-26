import apiClient from '../utils/axios'
import type { Member, RegisterMemberRequest, UpdateMemberProfileRequest, CompleteOnboardingRequest } from '../types'

export const memberApi = {
  register: (req: RegisterMemberRequest) =>
    apiClient.post<Member>('/members', req).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<Member>(`/members/${id}`).then((r) => r.data),

  search: (nickname: string) =>
    apiClient.get<Member[]>('/members/search', { params: { nickname } }).then((r) => r.data),

  updateProfile: (id: string, req: UpdateMemberProfileRequest) =>
    apiClient.put<Member>(`/members/${id}`, req).then((r) => r.data),

  updateProfileImage: (id: string, mediaId: string) =>
    apiClient.patch<Member>(`/members/${id}/profile-image`, { mediaId }).then((r) => r.data),

  withdraw: (id: string) => apiClient.delete(`/members/${id}`),

  completeOnboarding: (req: CompleteOnboardingRequest) =>
    apiClient.patch<Member>('/members/me/onboarding', req).then((r) => r.data),
}
