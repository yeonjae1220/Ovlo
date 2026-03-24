import apiClient from '../utils/axios'
import type { University, ExchangeUniversity, VideoReview, PageResult } from '../types'

interface UniversityPageResult {
  content: University[]
  totalElements: number
  page: number
  size: number
}

export const universityApi = {
  search: (keyword: string, countryCode?: string, page = 0, size = 20) =>
    apiClient
      .get<UniversityPageResult>('/universities', {
        params: { keyword, countryCode, page, size },
      })
      .then((r) => r.data.content),

  getById: (id: string) =>
    apiClient.get<University>(`/universities/${id}`).then((r) => r.data),
}

export const exchangeUniversityApi = {
  search: (keyword?: string, country?: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<ExchangeUniversity>>('/exchange-universities', {
        params: { keyword, country, page, size },
      })
      .then((r) => r.data),

  getById: (id: number) =>
    apiClient
      .get<ExchangeUniversity>(`/exchange-universities/${id}`)
      .then((r) => r.data),

  getReviews: (id: number, page = 0, size = 20) =>
    apiClient
      .get<PageResult<VideoReview>>(`/exchange-universities/${id}/reviews`, {
        params: { page, size },
      })
      .then((r) => r.data),
}
