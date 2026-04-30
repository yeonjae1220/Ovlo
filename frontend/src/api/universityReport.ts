import apiClient from '../utils/axios'

export interface UniversityReportSummary {
  id: number
  globalUnivId: number
  exchangeUnivId: number | null
  lang: string
  title: string
  summary: string | null
  sourceVideoCount: number
  sourceWebCount: number
  costCurrency: string | null
  supportedLangs: string[]
  createdAt: string
}

export interface UniversityReportDetail extends UniversityReportSummary {
  body: string
  content: string
}

export interface UniversityReportPageResult {
  content: UniversityReportSummary[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
}

export const universityReportApi = {
  list: (lang = 'en', page = 0, size = 20) =>
    apiClient
      .get<UniversityReportPageResult>('/university-reports', {
        params: { lang, page, size },
      })
      .then((r) => r.data),

  getById: (id: number, lang = 'en') =>
    apiClient
      .get<UniversityReportDetail>(`/university-reports/${id}`, { params: { lang } })
      .then((r) => r.data),

  getByUniversity: (globalUnivId: number, lang = 'en') =>
    apiClient
      .get<UniversityReportDetail>(`/university-reports/by-university/${globalUnivId}`, {
        params: { lang },
      })
      .then((r) => r.data),

  getLanguages: (id: number) =>
    apiClient
      .get<string[]>(`/university-reports/${id}/languages`)
      .then((r) => r.data),
}
