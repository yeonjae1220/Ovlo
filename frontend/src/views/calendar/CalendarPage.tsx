'use client'
import { useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { calendarApi, calendarError, type CalendarEvent, type School, type EventInput } from '@/api/calendar'
import { useAuthStore } from '@/store/authStore'
import { useI18n } from '@/i18n/I18nProvider'
import SchoolPicker from './SchoolPicker'
import EventEditor from './EventEditor'
import { addDays, dateKey, localTime, occursOn, eventDate } from './calendarDates'
import './calendar.css'

export default function CalendarPage({ management = false }: { management?: boolean }) {
  const { language } = useI18n(); const ko = language === 'ko'
  const memberId = useAuthStore(s => s.currentUser?.id)
  const qc = useQueryClient()
  const [month, setMonth] = useState(() => dateKey(new Date()).slice(0, 7))
  const [school, setSchool] = useState<School | null>(null)
  const [subscriptionSchool, setSubscriptionSchool] = useState<School | null>(null)
  const [subscriptionsOpen, setSubscriptionsOpen] = useState(false)
  const [filter, setFilter] = useState('ALL')
  const [day, setDay] = useState<string | null>(null)
  const [view, setView] = useState<'month' | 'list'>('month')
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [editor, setEditor] = useState<{ event?: CalendarEvent } | null>(null)
  const [notice, setNotice] = useState('')
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const options = useQuery({ queryKey: ['calendar', memberId, 'options'], queryFn: calendarApi.options, enabled: !!memberId })
  const managedSchool = school || options.data?.managedSchools[0] || null
  const canManage = options.data?.role === 'UNIV' || options.data?.role === 'ADMIN'
  const [year, monthNumber] = month.split('-').map(Number)
  const first = new Date(year, monthNumber - 1, 1, 12)
  const gridStart = addDays(dateKey(first), -(first.getDay() + 6) % 7)
  const gridEnd = addDays(gridStart, 42)
  const days = Array.from({ length: 42 }, (_, i) => addDays(gridStart, i))
  const events = useQuery({
    queryKey: ['calendar', memberId, management ? 'management' : 'events', management ? managedSchool?.id : null, gridStart, gridEnd, zone],
    queryFn: () => management ? calendarApi.manage(managedSchool!.id, gridStart, gridEnd, zone) : calendarApi.list(gridStart, gridEnd, zone),
    enabled: !!memberId && (!management || (!!canManage && !!managedSchool)),
  })
  const mutation = useMutation({
    mutationFn: (task: () => Promise<unknown>) => task(),
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['calendar', memberId] }); setNotice(ko ? '반영했습니다.' : 'Saved.'); },
  })
  const allEvents = [...(events.data || [])].sort((a, b) => eventDate(a, zone).localeCompare(eventDate(b, zone)) || a.details.title.localeCompare(b.details.title))
  const visible = allEvents.filter(e => filter === 'ALL' || filter === e.kind || filter === e.details.category)
  const agenda = visible.filter(e => day ? occursOn(e, day, zone) : days.some(d => d.startsWith(month) && occursOn(e, d, zone)))
  const selected = allEvents.find(e => `${e.kind}-${e.id}` === selectedKey)
  function save(input: EventInput) {
    mutation.mutate(() => management ? calendarApi.saveSchool(managedSchool!.id, input, editor?.event?.id) : calendarApi.savePersonal(input, editor?.event?.id),
      { onSuccess: () => setEditor(null) })
  }
  function moveMonth(delta: number) { setMonth(dateKey(new Date(year, monthNumber - 1 + delta, 1, 12)).slice(0, 7)); setDay(null); setSelectedKey(null); setEditor(null) }
  function provenance(e: CalendarEvent) {
    if (e.kind === 'PERSONAL') return ko ? '개인 · 나만 보기' : 'Personal · Only you'
    return e.origin === 'UNIVERSITY' ? (ko ? '학교 담당자 등록' : 'Posted by university') : (ko ? 'Ovlo 등록 · 학교 공식 공지 참고' : 'Ovlo curated · Official source')
  }
  function when(e: CalendarEvent) {
    const p = e.details.period
    return p.allDay ? `${p.startDate} ~ ${addDays(p.endDate!, -1)} · ${ko ? '종일' : 'All day'}` : `${localTime(p.startsAt!, p.timeZone).replace('T', ' ')} ~ ${localTime(p.endsAt!, p.timeZone).replace('T', ' ')} · ${p.timeZone}`
  }
  function remove(e: CalendarEvent) {
    if (window.confirm(ko ? '이 일정을 삭제할까요?' : 'Delete this event?')) mutation.mutate(() => calendarApi.remove(e), { onSuccess: () => setSelectedKey(null) })
  }
  return <div className="cal-page">
    <header className="cal-heading"><div><span className="cal-eyebrow">OVLO CALENDAR</span><h1>{management ? (ko ? '학교 일정 관리' : 'University events') : (ko ? '내 캘린더' : 'My calendar')}</h1><p>{management ? (ko ? '학교의 중요한 소식을 학생의 일상으로.' : 'Keep students connected to university life.') : (ko ? '학교의 중요한 일정과 나만의 계획을 한곳에서.' : 'University dates and personal plans, together.')}</p></div>
      <div className="cal-actions">{management ? <Link href="/calendar">{ko ? '학생 캘린더' : 'My calendar'}</Link> : <><button onClick={() => setSubscriptionsOpen(!subscriptionsOpen)}>{ko ? '학교 구독' : 'Universities'}</button>{canManage && <Link href="/calendar/manage">{ko ? '학교 일정 관리' : 'Manage events'}</Link>}</>}
        {(!management || (canManage && managedSchool)) && <button className="cal-primary" onClick={() => { setEditor({}); mutation.reset(); setNotice('') }}>{management ? (ko ? '+ 학교 일정' : '+ University event') : (ko ? '+ 개인 일정' : '+ Personal event')}</button>}</div>
    </header>
    <div className="cal-status" aria-live="polite">{notice}</div>
    {options.isError && <p role="alert" className="cal-error">{calendarError(options.error)} <button onClick={() => options.refetch()}>{ko ? '재시도' : 'Retry'}</button></p>}
    {mutation.isError && <p role="alert" className="cal-error">{calendarError(mutation.error)}</p>}
    {management && options.isSuccess && !canManage && <section className="cal-panel"><p>{ko ? '학교 담당자 승인이 필요합니다.' : 'University staff approval is required.'}</p><Link href="/university-account">{ko ? '학교 담당자 신청·상태 확인' : 'Apply / view application'}</Link></section>}
    {management && canManage && <section className="cal-panel">
      {options.data?.role === 'ADMIN' ? <SchoolPicker value={school} onChange={s => { setSchool(s); setEditor(null); setSelectedKey(null) }} ko={ko} /> :
        options.data?.managedSchools.length ? <label>{ko ? '관리 학교' : 'University'}<select value={managedSchool?.id} onChange={e => { setSchool(options.data!.managedSchools.find(s => s.id === Number(e.target.value)) || null); setEditor(null) }}>{options.data.managedSchools.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label> :
        <p>{ko ? '배정된 학교가 없습니다. 운영진에게 학교 배정을 요청해 주세요.' : 'No university assigned. Contact an administrator.'}</p>}
      <Link href="/university-account">{ko ? '담당자 신청·상태 확인' : 'Staff applications'}</Link>
    </section>}
    {!management && subscriptionsOpen && <section className="cal-panel"><h2>{ko ? '학교 캘린더 구독' : 'University calendars'}</h2><p>{ko ? '구독한 학교의 게시된 일정이 자동으로 표시됩니다. 학교 소속 인증과는 별개입니다.' : 'Published events from subscribed universities appear automatically. Subscriptions do not verify affiliation.'}</p>
      <div className="cal-chips">{options.data?.subscriptions.map(s => <button key={s.id} disabled={mutation.isPending} onClick={() => mutation.mutate(() => calendarApi.unsubscribe(s.id))}>{s.name} ×</button>)}</div>
      <div className="cal-chips">{options.data?.suggestions.filter(s => !options.data.subscriptions.some(v => v.id === s.id)).map(s => <button key={s.id} disabled={mutation.isPending} onClick={() => mutation.mutate(() => calendarApi.subscribe(s.id))}>+ {s.name}</button>)}</div>
      <SchoolPicker value={subscriptionSchool} onChange={setSubscriptionSchool} ko={ko} />
      {subscriptionSchool && <button className="cal-primary" disabled={mutation.isPending || options.data?.subscriptions.some(s => s.id === subscriptionSchool.id)} onClick={() => mutation.mutate(() => calendarApi.subscribe(subscriptionSchool.id), { onSuccess: () => setSubscriptionSchool(null) })}>{ko ? '이 학교 구독' : 'Subscribe'}</button>}
    </section>}
    {editor && <EventEditor key={editor.event ? `${editor.event.kind}-${editor.event.id}` : 'new'} event={editor.event} school={management}
      curated={editor.event ? editor.event.origin === 'OVLO_CURATED' : options.data?.role === 'ADMIN'} ko={ko} busy={mutation.isPending} onSave={save} onClose={() => setEditor(null)} />}
    {(!management || (canManage && managedSchool)) && <>
      <section className="cal-panel">
        <div className="cal-toolbar"><div className="cal-actions"><button aria-label={ko ? '이전 달' : 'Previous month'} onClick={() => moveMonth(-1)}>←</button><label><span className="cal-sr">{ko ? '조회 월' : 'Month'}</span><input type="month" value={month} onChange={e => { if (e.target.value) { setMonth(e.target.value); setDay(null) } }} /></label><button aria-label={ko ? '다음 달' : 'Next month'} onClick={() => moveMonth(1)}>→</button><button onClick={() => { setMonth(dateKey(new Date()).slice(0, 7)); setDay(dateKey(new Date())) }}>{ko ? '오늘' : 'Today'}</button></div>
          <div className="cal-actions"><select aria-label={ko ? '일정 필터' : 'Event filter'} value={filter} onChange={e => setFilter(e.target.value)}><option value="ALL">{ko ? '모든 일정' : 'All events'}</option><option value="SCHOOL">{ko ? '학교 일정' : 'University'}</option>{!management && <option value="PERSONAL">{ko ? '개인 일정' : 'Personal'}</option>}<option value="ADMINISTRATION">{ko ? '행정 · 비자' : 'Administration'}</option><option value="ACADEMIC">{ko ? '학사' : 'Academic'}</option><option value="EVENT">{ko ? '행사' : 'Events'}</option></select><button className="cal-view-toggle" onClick={() => setView(view === 'month' ? 'list' : 'month')}>{view === 'month' ? (ko ? '목록 보기' : 'List') : (ko ? '달력 보기' : 'Month')}</button></div>
        </div>
        <p className="cal-hint">{ko ? '표시 시간대' : 'Display time zone'}: {zone} · {ko ? '종일 일정은 학교가 지정한 날짜를 유지합니다.' : 'All-day dates remain as published.'}</p>
        {events.isError && <p role="alert" className="cal-error">{calendarError(events.error)} <button onClick={() => events.refetch()}>{ko ? '재시도' : 'Retry'}</button></p>}
        {events.isFetching && <p role="status">{ko ? '일정을 불러오는 중…' : 'Loading events…'}</p>}
        {view === 'month' && <div className="cal-grid-wrap"><div className="cal-grid cal-weekdays">{(ko ? ['월','화','수','목','금','토','일'] : ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']).map(d => <span key={d}>{d}</span>)}</div>
          <div className="cal-grid">{days.map(d => <button key={d} className={`cal-day ${d.startsWith(month) ? '' : 'cal-day-out'} ${day === d ? 'cal-day-selected' : ''} ${d === dateKey(new Date()) ? 'cal-day-today' : ''}`} aria-label={d} aria-pressed={day === d} onClick={() => setDay(day === d ? null : d)}><span>{Number(d.slice(-2))}</span>{visible.filter(e => occursOn(e, d, zone)).slice(0, 3).map(e => <span key={`${e.kind}-${e.id}`} className={`cal-dot-event ${e.kind === 'PERSONAL' ? 'cal-personal' : ''} ${e.status === 'CANCELLED' ? 'cal-cancelled' : ''}`}>{e.details.title}</span>)}{visible.filter(e => occursOn(e, d, zone)).length > 3 && <small>+{visible.filter(e => occursOn(e, d, zone)).length - 3}</small>}</button>)}</div></div>}
      </section>
      <div className="cal-columns"><section className="cal-panel cal-agenda"><div className="cal-row"><h2>{day || (ko ? '이달의 일정' : 'This month')}</h2>{day && <button onClick={() => setDay(null)}>{ko ? '월 전체' : 'Whole month'}</button>}</div>
        {events.isSuccess && agenda.length === 0 && <div className="cal-empty"><p>{ko ? '표시할 일정이 없습니다.' : 'No events to display.'}</p>{!management && options.data?.subscriptions.length === 0 && <button onClick={() => setSubscriptionsOpen(true)}>{ko ? '학교 캘린더 구독하기' : 'Subscribe to a university'}</button>}</div>}
        {agenda.map(e => <button key={`${e.kind}-${e.id}`} className={`cal-agenda-item ${e.kind === 'PERSONAL' ? 'cal-agenda-personal' : ''}`} onClick={() => setSelectedKey(`${e.kind}-${e.id}`)}><span className="cal-hint">{eventDate(e, zone)} · {e.universityName || (ko ? '개인' : 'Personal')}</span><strong className={e.status === 'CANCELLED' ? 'cal-cancelled' : ''}>{e.details.title}</strong><small>{provenance(e)}{e.status === 'CANCELLED' ? (ko ? ' · 취소됨' : ' · Cancelled') : e.status === 'DRAFT' ? (ko ? ' · 초안' : ' · Draft') : ''}</small></button>)}
      </section>
      {selected && <section className="cal-panel cal-detail"><div className="cal-row"><span className="cal-badge">{provenance(selected)}</span><button onClick={() => setSelectedKey(null)}>{ko ? '닫기' : 'Close'}</button></div><h2>{selected.details.title}</h2>{selected.status === 'CANCELLED' && <p role="status" className="cal-error">{ko ? '취소된 일정입니다.' : 'This event was cancelled.'}</p>}
        <p>{selected.universityName}</p><p>{when(selected)}</p>{selected.details.deadlineAt && <p><strong>{ko ? '신청 마감' : 'Deadline'}: </strong>{localTime(selected.details.deadlineAt, selected.details.period.timeZone).replace('T', ' ')} · {selected.details.period.timeZone}</p>}
        {selected.details.location && <p>{ko ? '장소' : 'Location'}: {selected.details.location}</p>}{selected.details.audience && <p>{ko ? '대상' : 'Audience'}: {selected.details.audience}</p>}<p className="cal-description">{selected.details.description}</p>
        {selected.details.actionUrl && <p><a href={selected.details.actionUrl} target="_blank" rel="noopener noreferrer">{ko ? '신청 · 관련 페이지 ↗' : 'Registration / details ↗'}</a></p>}
        {selected.source && <p><a href={selected.source.url} target="_blank" rel="noopener noreferrer">{ko ? '학교 공식 출처 ↗' : 'Official source ↗'}</a><br/><small>{ko ? '출처 확인일' : 'Source checked'}: {selected.source.checkedOn}</small></p>}
        <div className="cal-actions">{(selected.kind === 'PERSONAL' || (management && selected.status !== 'CANCELLED')) && <button onClick={() => { setEditor({ event: selected }); mutation.reset() }}>{ko ? '수정' : 'Edit'}</button>}
          {(selected.kind === 'PERSONAL' || (management && selected.status === 'DRAFT')) && <button disabled={mutation.isPending} onClick={() => remove(selected)}>{ko ? '삭제' : 'Delete'}</button>}
          {management && selected.status === 'DRAFT' && <button className="cal-primary" disabled={mutation.isPending} onClick={() => mutation.mutate(() => calendarApi.status(selected, 'publish'))}>{ko ? '게시' : 'Publish'}</button>}
          {management && selected.status === 'PUBLISHED' && <button disabled={mutation.isPending} onClick={() => { if (window.confirm(ko ? '학생들에게 취소된 일정으로 표시할까요?' : 'Mark this event as cancelled for students?')) mutation.mutate(() => calendarApi.status(selected, 'cancel')) }}>{ko ? '행사 취소' : 'Cancel event'}</button>}
        </div>
      </section>}
      </div>
    </>}
    <footer className="cal-hint"><Link href="/university-account">{ko ? '학교 담당자이신가요? 신청·승인 상태 확인' : 'University staff? Apply / check approval'}</Link></footer>
  </div>
}
