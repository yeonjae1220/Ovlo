import apiClient from '../utils/axios'
import type { ExchangeUniversity, UniversityCatalogItem, VideoReview, PageResult } from '../types'

export interface UniversityReportSummary {
  id: number
  title: string
  summary?: string
  sourceVideoCount: number
  sourceWebCount: number
  supportedLangs: string[]
  createdAt?: string
}

export interface UniversityReportDetail {
  id: number
  globalUnivId?: number
  lang: string
  title: string
  summary?: string
  body: string
  content?: string   // raw JSON string
  sourceVideoCount: number
  sourceWebCount: number
  countryCode?: string | null   // 대학 소재국 — 비용 표시 통화 결정용
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

export interface ExchangeUniversityCountry {
  country: string
  countryCode: string
  universityCount: number
}

export const universityReportApi = {
  list: (lang = 'ko', keyword = '', page = 0, size = 20) =>
    apiClient
      .get<PageResult<UniversityReportSummary>>('/university-reports', {
        params: { lang, keyword: keyword || undefined, page, size },
      })
      .then((r) => r.data),

  getById: (id: number, lang = 'ko') =>
    apiClient
      .get<UniversityReportDetail>(`/university-reports/${id}`, { params: { lang } })
      .then((r) => r.data),

  getByUniversity: (globalUnivId: number, lang = 'ko') =>
    apiClient
      .get<UniversityReportDetail>(`/university-reports/by-university/${globalUnivId}`, { params: { lang } })
      .then((r) => r.data),

  getByExchangeUniversity: (exchangeUnivId: number, lang = 'ko') =>
    apiClient
      .get<UniversityReportDetail>(`/university-reports/by-exchange-university/${exchangeUnivId}`, { params: { lang } })
      .then((r) => r.data),

  getLanguages: (id: number) =>
    apiClient
      .get<string[]>(`/university-reports/${id}/languages`)
      .then((r) => r.data),
}

/** 통합 대학 카탈로그 — 콘텐츠(리포트/후기) 보유 대학을 한/영/현지어·국가코드로 검색 */
export const universityCatalogApi = {
  search: (keyword?: string, countryCode?: string, page = 0, size = 20) =>
    apiClient
      .get<PageResult<UniversityCatalogItem>>('/universities/catalog', {
        params: {
          keyword: keyword || undefined,
          countryCode: countryCode || undefined,
          page,
          size,
        },
      })
      .then((r) => r.data),

  /** 콘텐츠 보유 대학을 국가별 집계 (리포트만 있는 국가도 포함) */
  countries: () =>
    apiClient
      .get<ExchangeUniversityCountry[]>('/universities/catalog/countries')
      .then((r) => r.data),
}

export const exchangeUniversityApi = {
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
