import { useQuery } from '@tanstack/react-query'
import { universityApi, globalUniversityApi, exchangeUniversityApi } from '../api/university'

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

export function useExchangeUniversitySearch(keyword: string, countryCode?: string) {
  return useQuery({
    queryKey: ['exchange-universities', 'search', keyword, countryCode],
    queryFn: () => exchangeUniversityApi.search(keyword || undefined, countryCode || undefined),
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
