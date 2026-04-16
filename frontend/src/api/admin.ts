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

export interface AdminBoardResponse {
  id: number
  name: string
  category: string
  scope: string
  creatorId: number
  universityId: number | null
  active: boolean
}

export interface AdminPostResponse {
  id: number
  boardId: number
  authorId: number
  title: string
  likeCount: number
  dislikeCount: number
  deleted: boolean
}

export interface AdminUniversityResponse {
  id: number
  name: string
  localName: string | null
  countryCode: string
  city: string
}

export interface AdminStatsResponse {
  totalMembers: number
  totalBoards: number
  totalPosts: number
  totalUniversities: number
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

  getBoards: (page = 0, size = 20) =>
    apiClient
      .get<SpringPage<AdminBoardResponse>>('/admin/boards', { params: { page, size } })
      .then((r) => r.data),

  getPosts: (page = 0, size = 20) =>
    apiClient
      .get<SpringPage<AdminPostResponse>>('/admin/posts', { params: { page, size } })
      .then((r) => r.data),

  getUniversities: (page = 0, size = 20) =>
    apiClient
      .get<SpringPage<AdminUniversityResponse>>('/admin/universities', { params: { page, size } })
      .then((r) => r.data),

  getStats: () =>
    apiClient.get<AdminStatsResponse>('/admin/stats').then((r) => r.data),
}
