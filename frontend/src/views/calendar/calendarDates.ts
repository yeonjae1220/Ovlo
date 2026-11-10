import type { CalendarEvent } from '@/api/calendar'

export function dateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
export function addDays(key: string, days: number) {
  const [y, m, d] = key.split('-').map(Number)
  return dateKey(new Date(y, m - 1, d + days, 12))
}
export function localTime(instant: string, timeZone: string) {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date(instant))
  const p = Object.fromEntries(parts.map(p => [p.type, p.value]))
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`
}
/** Reject nonexistent/ambiguous DST wall times instead of silently shifting a deadline. */
export function toInstant(wallTime: string, timeZone: string) {
  const base = Date.parse(wallTime + ':00Z')
  if (!Number.isFinite(base)) throw new Error('올바른 날짜와 시간을 입력해 주세요 / Invalid date and time')
  const offsets = new Set<number>()
  for (const hours of [-36, -12, 0, 12, 36]) {
    const probe = base + hours * 3600000
    offsets.add(Date.parse(localTime(new Date(probe).toISOString(), timeZone) + ':00Z') - probe)
  }
  const candidates = [...offsets].map(offset => new Date(base - offset).toISOString()).filter(instant => localTime(instant, timeZone) === wallTime)
  if (candidates.length !== 1) throw new Error('서머타임 전환 시각입니다. 다른 시각을 선택해 주세요 / Ambiguous or nonexistent daylight saving time')
  return candidates[0]
}
export function occursOn(event: CalendarEvent, day: string, timeZone: string) {
  const p = event.details.period
  if (p.allDay) return p.startDate! <= day && p.endDate! > day
  // Midnight can itself be a DST transition; use formatted occupied dates, end exclusive.
  const first = localTime(p.startsAt!, timeZone).slice(0, 10)
  const last = localTime(new Date(Date.parse(p.endsAt!) - 1).toISOString(), timeZone).slice(0, 10)
  return first <= day && last >= day
}
export function eventDate(event: CalendarEvent, timeZone: string) {
  const p = event.details.period
  return p.allDay ? p.startDate! : localTime(p.startsAt!, timeZone).slice(0, 10)
}
