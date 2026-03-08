import apiClient from '../utils/axios'
import type { Member } from '../types'

export const followApi = {
  follow: (followeeId: string) =>
    apiClient.post('/follows', { followeeId: Number(followeeId) }),

  unfollow: (followeeId: string) => apiClient.delete(`/follows/${followeeId}`),

  getFollowers: (memberId: string) =>
    apiClient.get<Member[]>(`/follows/followers/${memberId}`).then((r) => r.data),

  getFollowings: (memberId: string) =>
    apiClient.get<Member[]>(`/follows/followings/${memberId}`).then((r) => r.data),
}
