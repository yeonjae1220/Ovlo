export function formatDate(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(date)
}

/** UI 언어(9개) → 공지 언어 코드(7개). 없는 언어는 영어로. */
export function noticeLangOf(uiLanguage: string): string {
  const supported = ['ko', 'en', 'ja', 'zh', 'de', 'fr', 'vi']
  return supported.includes(uiLanguage) ? uiLanguage : 'en'
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d)
}
