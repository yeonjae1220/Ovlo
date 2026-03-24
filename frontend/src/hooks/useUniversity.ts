import { useQuery } from '@tanstack/react-query'
import { universityApi, exchangeUniversityApi } from '../api/university'

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

export function useExchangeUniversitySearch(keyword: string, country?: string) {
  return useQuery({
    queryKey: ['exchange-universities', 'search', keyword, country],
    queryFn: () => exchangeUniversityApi.search(keyword || undefined, country || undefined),
    enabled: keyword.length >= 1 || (!!country && country.length >= 1),
  })
}

export function useExchangeUniversity(id: number) {
  return useQuery({
    queryKey: ['exchange-university', id],
    queryFn: () => exchangeUniversityApi.getById(id),
    enabled: id > 0,
  })
}

export function useExchangeUniversityReviews(id: number) {
  return useQuery({
    queryKey: ['exchange-university-reviews', id],
    queryFn: () => exchangeUniversityApi.getReviews(id),
    enabled: id > 0,
  })
}
