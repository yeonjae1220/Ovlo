export interface ReportSource { url: string; title: string }

/** Source presence and a report's reference date are not proof of factual verification. */
export function reportEvidence(content: unknown, now = new Date()) {
  const value = content && typeof content === 'object' && !Array.isArray(content)
    ? content as Record<string, unknown> : {}
  const sources: ReportSource[] = []
  const seen = new Set<string>()
  if (Array.isArray(value.sources)) {
    for (const item of value.sources) {
      if (!item || typeof item !== 'object' || typeof item.url !== 'string') continue
      try {
        const url = new URL(item.url)
        if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password) continue
        if (seen.has(url.href)) continue
        seen.add(url.href)
        sources.push({ url: url.href, title: typeof item.title === 'string' && item.title.trim() ? item.title.trim() : url.hostname })
      } catch { /* An invalid stored source must not become a clickable link. */ }
    }
  }
  let asOf: string | null = null
  let periodEnd = NaN
  if (typeof value.as_of === 'string' && /^\d{4}-\d{2}(-\d{2})?$/.test(value.as_of)) {
    const [year, month, day = 1] = value.as_of.split('-').map(Number)
    const start = new Date(Date.UTC(year, month - 1, day))
    if (start.getUTCFullYear() === year && start.getUTCMonth() === month - 1 && start.getUTCDate() === day && start.getTime() <= now.getTime()) {
      asOf = value.as_of
      periodEnd = value.as_of.length === 7 ? Date.UTC(year, month, 0) : start.getTime()
    }
  }
  const status = !sources.length ? 'missing' : !asOf ? 'undated'
    : now.getTime() - periodEnd > 180 * 86400000 ? 'old' : 'checkSource'
  return { asOf, sources, status, verified: false as const }
}
