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

interface GlobalUniversityItem {
  id: number
  nameEn: string
  countryCode?: string
  city?: string
}

/** global_universities 테이블 (10,150개) — 회원가입/온보딩 대학 선택용 */
export const globalUniversityApi = {
  search: (keyword: string, countryCode?: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<GlobalUniversityItem>>('/global-universities', {
        params: { keyword, countryCode, page, size },
      })
      .then((r) =>
        r.data.content.map((u) => ({
          id: u.id,
          name: u.nameEn,
          countryCode: u.countryCode ?? '',
          city: u.city ?? '',
        }))
      ),
}

export const exchangeUniversityApi = {
  search: (keyword?: string, countryCode?: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<ExchangeUniversity>>('/exchange-universities', {
        params: { keyword, countryCode, page, size },
      })
      .then((r) => r.data),

  getById: (id: number) =>
    apiClient
      .get<ExchangeUniversity>(`/exchange-universities/${id}`)
      .then((r) => r.data),

  getReviews: (id: number, direction?: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<VideoReview>>(`/exchange-universities/${id}/reviews`, {
        params: { direction: direction || undefined, page, size },
      })
      .then((r) => r.data),
}
