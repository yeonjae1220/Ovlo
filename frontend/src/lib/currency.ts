/**
 * 디스플레이 레이어 통화 변환 유틸.
 *
 * 리포트 비용은 프로즈에 원화(만원 단위)로 적혀 있다(예: "150-180만원 ($135-165)").
 * 여기서 원화 범위를 파싱해, 대학 소재국 통화로 실시간 환율 변환해 표시한다.
 * (원 숫자 자체의 신뢰도 문제는 별개 — 표시 계층은 있는 값을 일관되게 환산할 뿐.)
 */

/** ISO-3166 alpha-2 국가코드 → ISO-4217 통화코드. 미매핑 국가는 표시 통화 없음(원문 유지). */
export const COUNTRY_CURRENCY: Record<string, string> = {
  US: 'USD', GB: 'GBP', CN: 'CNY', JP: 'JPY', KR: 'KRW', AU: 'AUD', CA: 'CAD',
  TW: 'TWD', AE: 'AED', CO: 'COP', RU: 'RUB', BR: 'BRL', IN: 'INR', TR: 'TRY',
  TH: 'THB', CL: 'CLP', CZ: 'CZK', AR: 'ARS', PL: 'PLN', EG: 'EGP', MX: 'MXN',
  ID: 'IDR', SA: 'SAR', HK: 'HKD', ZA: 'ZAR', LB: 'LBP', IL: 'ILS', PE: 'PEN',
  HU: 'HUF', SE: 'SEK', JO: 'JOD', BD: 'BDT', MY: 'MYR', CH: 'CHF', NO: 'NOK',
  DK: 'DKK', SG: 'SGD', NZ: 'NZD', PH: 'PHP', VN: 'VND', PK: 'PKR', NG: 'NGN',
  KZ: 'KZT', UA: 'UAH', MA: 'MAD', KE: 'KES', QA: 'QAR', KW: 'KWD', BH: 'BHD',
  OM: 'OMR', LK: 'LKR', RO: 'RON', BG: 'BGN', HR: 'EUR', RS: 'RSD', IS: 'ISK',
  // 유로존
  ES: 'EUR', IT: 'EUR', DE: 'EUR', NL: 'EUR', FR: 'EUR', IE: 'EUR', PT: 'EUR',
  AT: 'EUR', BE: 'EUR', FI: 'EUR', GR: 'EUR', SK: 'EUR', SI: 'EUR', LT: 'EUR',
  LV: 'EUR', EE: 'EUR', LU: 'EUR', CY: 'EUR', MT: 'EUR',
}

export interface MoneyRange {
  minKrw: number
  maxKrw: number
}

const WON_UNIT = 10_000 // 만 = 10,000

/**
 * 비용 프로즈에서 원화(만 단위) 범위를 파싱. 매칭 실패 시 null.
 * 예: "150-180만원 ($135-165)" → {minKrw:1_500_000, maxKrw:1_800_000}
 *     "20만원" → {minKrw:200_000, maxKrw:200_000}
 *     "40-60万 won" → {minKrw:400_000, maxKrw:600_000}
 *
 * de/fr/vi 번역본은 만(10,000)이 "Millionen/millions/triệu Won"으로 오역돼 있으나,
 * 이것이 만 단위의 오역임을 알므로 동일 단위로 파싱한다 → 7개 언어 전부 정확 환산
 * (표시 계층에서 오역을 우회. 원문 텍스트 자체 수정은 콘텐츠 품질 트랙의 별건).
 */
export function parseKrwRange(text: string | undefined | null): MoneyRange | null {
  if (!text) return null
  // 숫자[-숫자]?  (만 단위 표기: 만원|만|万… 또는 오역된 Millionen/millions de/triệu Won)
  const m = text.match(
    /([\d][\d,]*(?:\.\d+)?)\s*(?:(?:[-–~]|to|bis|à)\s*([\d][\d,]*(?:\.\d+)?)\s*)?(?:만원|만|万韩元|万원|万|Millionen\s+Won|millions?\s+de\s+won|triệu\s+won)/i,
  )
  if (!m) return null
  const toNum = (s: string) => parseFloat(s.replace(/,/g, ''))
  const lo = toNum(m[1])
  const hi = m[2] != null ? toNum(m[2]) : lo
  if (!isFinite(lo) || !isFinite(hi)) return null
  return { minKrw: lo * WON_UNIT, maxKrw: hi * WON_UNIT }
}

/** Intl 로 통화 포맷. KRW/JPY 등 소수 없는 통화는 자동 처리. */
export function formatMoney(amount: number, currency: string, locale: string): string {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      maximumFractionDigits: amount >= 100 ? 0 : 2,
    }).format(amount)
  } catch {
    return `${Math.round(amount).toLocaleString(locale)} ${currency}`
  }
}

/** 원화 범위를 대상 통화로 환산해 "€1,035 – €1,240" 형태로 포맷. */
export function formatConvertedRange(
  range: MoneyRange,
  krwToTarget: number,
  currency: string,
  locale: string,
): string {
  const lo = range.minKrw * krwToTarget
  const hi = range.maxKrw * krwToTarget
  const loStr = formatMoney(lo, currency, locale)
  if (Math.round(lo) === Math.round(hi)) return loStr
  return `${loStr} – ${formatMoney(hi, currency, locale)}`
}
