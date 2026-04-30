import { useQuery } from '@tanstack/react-query'
import { universityReportApi } from '../api/universityReport'

export function useUniversityReportList(lang: string, page: number, size = 20) {
  return useQuery({
    queryKey: ['university-reports', lang, page, size],
    queryFn: () => universityReportApi.list(lang, page, size),
  })
}

export function useUniversityReport(id: number, lang: string) {
  return useQuery({
    queryKey: ['university-report', id, lang],
    queryFn: () => universityReportApi.getById(id, lang),
    enabled: !!id,
  })
}

export function useUniversityReportByUniv(globalUnivId: number | null, lang: string) {
  return useQuery({
    queryKey: ['university-report-by-univ', globalUnivId, lang],
    queryFn: () => universityReportApi.getByUniversity(globalUnivId!, lang),
    enabled: !!globalUnivId,
  })
}

export function useUniversityReportLanguages(id: number) {
  return useQuery({
    queryKey: ['university-report-langs', id],
    queryFn: () => universityReportApi.getLanguages(id),
    enabled: !!id,
  })
}
