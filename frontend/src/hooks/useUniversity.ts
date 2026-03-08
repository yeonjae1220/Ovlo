import { useQuery } from '@tanstack/react-query'
import { universityApi } from '../api/university'

export function useUniversitySearch(keyword: string, countryCode?: string) {
  return useQuery({
    queryKey: ['universities', 'search', keyword, countryCode],
    queryFn: () => universityApi.search(keyword, countryCode),
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
