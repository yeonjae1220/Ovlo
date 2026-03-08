import apiClient from '../utils/axios'
import type { University } from '../types'

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
