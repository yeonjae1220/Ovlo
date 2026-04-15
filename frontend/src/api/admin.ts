import apiClient from '../utils/axios'
import type { MemberRole } from '../types'

export interface AdminMemberResponse {
  id: number
  nickname: string
  email: string
  status: string
  role: MemberRole
  provider: string
}

export interface AdminStatsResponse {
  totalMembers: number
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const adminApi = {
  getMembers: (page = 0, size = 20) =>
    apiClient
      .get<SpringPage<AdminMemberResponse>>('/admin/members', { params: { page, size } })
      .then((r) => r.data),

  updateMemberRole: (id: number, role: MemberRole) =>
    apiClient
      .patch<AdminMemberResponse>(`/admin/members/${id}/role`, { role })
      .then((r) => r.data),

  getStats: () =>
    apiClient.get<AdminStatsResponse>('/admin/stats').then((r) => r.data),
}
