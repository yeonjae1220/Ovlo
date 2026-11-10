'use client'
import { useState, type FormEvent } from 'react'
import type { CalendarEvent, EventCategory, EventInput } from '@/api/calendar'
import { addDays, dateKey, localTime, toInstant } from './calendarDates'

export default function EventEditor({ event, school, curated, ko, busy, onSave, onClose }: {
  event?: CalendarEvent; school: boolean; curated: boolean; ko: boolean; busy: boolean
  onSave: (input: EventInput) => void; onClose: () => void
}) {
  const p = event?.details.period
  const initialZone = p?.timeZone || Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const [zone, setZone] = useState(initialZone)
  const [allDay, setAllDay] = useState(p?.allDay ?? true)
  const [error, setError] = useState('')
  const d = event?.details
  const today = dateKey(new Date())
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setError('')
    const f = new FormData(e.currentTarget)
    const s = (name: string) => String(f.get(name) || '')
    try {
      const input: EventInput = {
        title: s('title'), description: s('description'), category: s('category') as EventCategory,
        allDay, startDate: allDay ? s('startDate') : null, endDate: allDay ? addDays(s('endDate'), 1) : null,
        startsAt: allDay ? null : toInstant(s('startsAt'), zone), endsAt: allDay ? null : toInstant(s('endsAt'), zone), timeZone: zone,
        location: s('location'), actionUrl: s('actionUrl'), audience: s('audience'), deadlineAt: s('deadlineAt') ? toInstant(s('deadlineAt'), zone) : null,
        sourceUrl: school ? s('sourceUrl') || null : null, sourceCheckedOn: school ? s('sourceCheckedOn') || null : null, version: event?.version,
      }
      if (allDay ? input.endDate! <= input.startDate! : input.endsAt! <= input.startsAt!) throw new Error(ko ? '종료는 시작 이후여야 합니다.' : 'End must follow start.')
      onSave(input)
    } catch (e) { setError((e as Error).message) }
  }
  return <section className="cal-panel cal-editor" aria-label={ko ? '일정 편집' : 'Event editor'}>
    <div className="cal-row"><h2>{event ? (ko ? '일정 수정' : 'Edit event') : (ko ? '새 일정' : 'New event')}</h2><button type="button" onClick={onClose} disabled={busy}>{ko ? '닫기' : 'Close'}</button></div>
    {school && <p>{ko ? '초안으로 저장한 뒤 게시하면 학생 캘린더에 표시됩니다.' : 'Save a draft, then publish it to student calendars.'}</p>}
    <form onSubmit={submit} className="cal-form">
      <label className="cal-wide">{ko ? '제목' : 'Title'}<input name="title" required maxLength={200} defaultValue={d?.title} autoFocus /></label>
      <label>{ko ? '분류' : 'Category'}<select name="category" defaultValue={d?.category || 'EVENT'}>
        <option value="ADMINISTRATION">{ko ? '행정 · 비자' : 'Administration · Visa'}</option><option value="ACADEMIC">{ko ? '학사' : 'Academic'}</option><option value="EVENT">{ko ? '행사' : 'Event'}</option><option value="OTHER">{ko ? '기타' : 'Other'}</option>
      </select></label>
      <label>{ko ? '일정 종류' : 'Schedule type'}<select value={String(allDay)} onChange={e => setAllDay(e.target.value === 'true')}><option value="true">{ko ? '종일' : 'All day'}</option><option value="false">{ko ? '시간 지정' : 'Timed'}</option></select></label>
      <label className="cal-wide">{ko ? '기준 시간대 (예: Asia/Seoul, Europe/Paris)' : 'Time zone (e.g. Europe/Paris)'}<input value={zone} onChange={e => setZone(e.target.value)} required maxLength={100} list="calendar-zones" /></label>
      <datalist id="calendar-zones">{['Asia/Seoul','Asia/Tokyo','Europe/Paris','Europe/Berlin','Europe/London','America/New_York','America/Los_Angeles','Australia/Sydney','UTC'].map(z => <option key={z} value={z} />)}</datalist>
      {allDay ? <>
        <label>{ko ? '시작일' : 'First day'}<input type="date" name="startDate" defaultValue={p?.startDate || today} required /></label>
        <label>{ko ? '마지막 날 (포함)' : 'Last day (inclusive)'}<input type="date" name="endDate" defaultValue={p?.endDate ? addDays(p.endDate, -1) : today} required /></label>
      </> : <>
        <label>{ko ? '시작 시각' : 'Starts'}<input type="datetime-local" name="startsAt" defaultValue={p?.startsAt ? localTime(p.startsAt, initialZone) : ''} required /></label>
        <label>{ko ? '종료 시각' : 'Ends'}<input type="datetime-local" name="endsAt" defaultValue={p?.endsAt ? localTime(p.endsAt, initialZone) : ''} required /></label>
      </>}
      <label>{ko ? '장소' : 'Location'}<input name="location" maxLength={300} defaultValue={d?.location || ''} /></label>
      <label>{ko ? '신청 마감 (선택)' : 'Deadline (optional)'}<input type="datetime-local" name="deadlineAt" defaultValue={d?.deadlineAt ? localTime(d.deadlineAt, initialZone) : ''} /></label>
      <label>{ko ? '신청 · 관련 링크' : 'Registration / related link'}<input type="url" name="actionUrl" maxLength={2000} defaultValue={d?.actionUrl || ''} /></label>
      {school && <label>{ko ? '대상 안내' : 'Who this is for'}<input name="audience" maxLength={500} defaultValue={d?.audience || ''} /></label>}
      <label className="cal-wide">{ko ? '설명 · 메모' : 'Description / notes'}<textarea name="description" maxLength={10000} rows={4} defaultValue={d?.description || ''} /></label>
      {school && <>
        <p className="cal-wide">{curated ? (ko ? '운영진 대리 등록에는 공식 출처와 확인일이 필요합니다.' : 'Ovlo-curated events require an official source and check date.') : (ko ? '학교 공식 공지 링크가 있으면 함께 남겨 주세요.' : 'Include the official announcement when available.')}</p>
        <label>{ko ? '공식 출처 URL' : 'Official source URL'}<input type="url" name="sourceUrl" maxLength={2000} defaultValue={event?.source?.url || ''} required={curated} /></label>
        <label>{ko ? '출처 확인일' : 'Source checked on'}<input type="date" name="sourceCheckedOn" max={today} defaultValue={event?.source?.checkedOn || ''} required={curated} /></label>
      </>}
      {error && <p className="cal-error cal-wide" role="alert">{error}</p>}
      <div className="cal-wide cal-actions"><button className="cal-primary" type="submit" disabled={busy}>{busy ? (ko ? '저장 중…' : 'Saving…') : (ko ? '저장' : 'Save')}</button><button type="button" disabled={busy} onClick={onClose}>{ko ? '취소' : 'Cancel'}</button></div>
    </form>
  </section>
}
