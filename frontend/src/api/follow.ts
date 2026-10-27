import apiClient from '../utils/axios'
import type { FollowingStatus, MemberSummary, PageResult } from '../types'

export const followApi = {
  follow: (followeeId: string) =>
    apiClient.post('/follows', { followeeId: Number(followeeId) }),

  unfollow: (followeeId: string) => apiClient.delete(`/follows/${followeeId}`),

  getFollowers: (memberId: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<MemberSummary>>(`/follows/followers/${memberId}`, { params: { page, size } })
      .then((r) => r.data),

  getFollowings: (memberId: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<MemberSummary>>(`/follows/followings/${memberId}`, { params: { page, size } })
      .then((r) => r.data),

  getFollowingStatus: (memberIds: string[]) =>
    apiClient
      .get<FollowingStatus>('/follows/following-status', { params: { memberIds: memberIds.join(',') } })
      .then((r) => r.data),
}
