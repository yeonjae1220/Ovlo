import apiClient from '../utils/axios'
import type { Board, CreateBoardRequest } from '../types'

interface BoardPageResult {
  content: Board[]
  totalElements: number
  page: number
  size: number
}

export const boardApi = {
  page: (keyword = '', page = 0, category?: string, scope?: string) =>
    apiClient.get<BoardPageResult>('/boards', { params: { keyword, page, size: 20, category, scope } }).then(r => r.data),
  list: (keyword?: string, page = 0, size = 20) =>
    apiClient
      .get<BoardPageResult>('/boards', { params: { keyword, page, size } })
      .then((r) => r.data.content),

  getById: (id: string) =>
    apiClient.get<Board>(`/boards/${id}`).then((r) => r.data),

  create: (req: CreateBoardRequest) =>
    apiClient.post<Board>('/boards', req).then((r) => r.data),

  subscribe: (boardId: string) =>
    apiClient.post(`/boards/${boardId}/subscriptions`),

  unsubscribe: (boardId: string) =>
    apiClient.delete(`/boards/${boardId}/subscriptions`),
}
