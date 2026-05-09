import { useQuery } from '@tanstack/react-query'
import { universityApi, globalUniversityApi, exchangeUniversityApi, universityReportApi } from '../api/university'

export function useExchangeUniversityCountries() {
  return useQuery({
    queryKey: ['exchange-university-countries'],
    queryFn: () => exchangeUniversityApi.countries(),
    staleTime: 1000 * 60 * 10,
  })
}

export function useUniversitySearch(keyword: string, countryCode?: string) {
  return useQuery({
    queryKey: ['global-universities', 'search', keyword, countryCode],
    queryFn: () => globalUniversityApi.search(keyword, countryCode),
    enabled: keyword.length >= 1,
  })
}

export function useGlobalUniversitySearch(keyword: string, countryCode?: string) {
  return useQuery({
    queryKey: ['global-universities', 'search', keyword, countryCode],
    queryFn: () => globalUniversityApi.search(keyword, countryCode),
    enabled: keyword.length >= 1,
  })
}

export function useUniversity(id: string) {
  return useQuery({
    queryKey: ['university', id],
    queryFn: () => universityApi.getById(id),
    enabled: !!id,
  })
}

export function useExchangeUniversitySearch(keyword: string, countryCode?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['exchange-universities', 'search', keyword, countryCode, page, size],
    queryFn: () => exchangeUniversityApi.search(keyword || undefined, countryCode || undefined, page, size),
    enabled: keyword.length >= 1 || (!!countryCode && countryCode.length >= 1),
  })
}

export function useExchangeUniversity(id: number) {
  return useQuery({
    queryKey: ['exchange-university', id],
    queryFn: () => exchangeUniversityApi.getById(id),
    enabled: id > 0,
  })
}

export function useExchangeUniversityReviews(id: number, direction?: string) {
  return useQuery({
    queryKey: ['exchange-university-reviews', id, direction],
    queryFn: () => exchangeUniversityApi.getReviews(id, direction),
    enabled: id > 0,
  })
}

export function useUniversityReports(lang: string, keyword: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['university-reports', lang, keyword, page, size],
    queryFn: () => universityReportApi.list(lang, keyword, page, size),
    staleTime: 1000 * 60 * 5,
  })
}

export function useUniversityReport(id: number, lang: string) {
  return useQuery({
    queryKey: ['university-report', id, lang],
    queryFn: () => universityReportApi.getById(id, lang),
    enabled: id > 0,
  })
}

export function useUniversityReportByUniv(globalUnivId: number | null | undefined, lang: string) {
  return useQuery({
    queryKey: ['university-report-by-univ', globalUnivId, lang],
    queryFn: () => universityReportApi.getByUniversity(globalUnivId!, lang),
    enabled: !!globalUnivId,
  })
}

export function useUniversityReportByExchangeUniv(exchangeUnivId: number | null | undefined, lang: string) {
  return useQuery({
    queryKey: ['university-report-by-exchange-univ', exchangeUnivId, lang],
    queryFn: () => universityReportApi.getByExchangeUniversity(exchangeUnivId!, lang),
    enabled: !!exchangeUnivId,
  })
}

export function useUniversityReportLanguages(id: number) {
  return useQuery({
    queryKey: ['university-report-langs', id],
    queryFn: () => universityReportApi.getLanguages(id),
    enabled: id > 0,
  })
}
