/** Accept a local destination, including its query, without permitting auth loops or external redirects. */
export function safeReturnPath(value: string | null | undefined): string {
  if (!value || value.length > 2048 || !value.startsWith('/') || value.startsWith('//')) return '/boards'
  try {
    const decoded = decodeURIComponent(value)
    if ([...decoded].some(char => char.charCodeAt(0) <= 32 || char === '\\') || decoded.startsWith('//')) return '/boards'
    const url = new URL(value, 'https://ovlo.invalid')
    if (url.origin !== 'https://ovlo.invalid' || /^\/(login|register|oauth|onboarding)(\/|$)/.test(url.pathname)) return '/boards'
    return url.pathname + url.search + url.hash
  } catch { return '/boards' }
}

export function loginPath(path: string): string {
  return '/login?next=' + encodeURIComponent(safeReturnPath(path))
}
