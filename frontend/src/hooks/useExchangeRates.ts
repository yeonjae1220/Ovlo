import { useQuery } from '@tanstack/react-query'

/**
 * 원화(KRW) 기준 실시간 환율. open.er-api.com (무키·160+ 통화).
 * rates[X] = 1 KRW 당 X 통화. staleTime 12h 로 과도한 호출 방지.
 */
interface ErApiResponse {
  result: string
  rates: Record<string, number>
}

async function fetchKrwRates(): Promise<Record<string, number>> {
  const res = await fetch('https://open.er-api.com/v6/latest/KRW')
  if (!res.ok) throw new Error(`FX fetch failed: ${res.status}`)
  const data: ErApiResponse = await res.json()
  if (data.result !== 'success' || !data.rates) throw new Error('FX response invalid')
  return data.rates
}

export function useExchangeRates() {
  return useQuery({
    queryKey: ['fx-rates', 'KRW'],
    queryFn: fetchKrwRates,
    staleTime: 1000 * 60 * 60 * 12, // 12h
    gcTime: 1000 * 60 * 60 * 24,
    retry: 1,
  })
}
