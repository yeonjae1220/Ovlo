'use client'
import { useState, type FormEvent } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { calendarApi, calendarError, type School } from '@/api/calendar'
import { useI18n } from '@/i18n/I18nProvider'
import { useAuthStore } from '@/store/authStore'
import { Brand } from '@/components/ui/Brand'
import SchoolPicker from './SchoolPicker'
import './calendar.css'

export default function UniversityAccountPage({ registration = false }: { registration?: boolean }) {
  const { language } = useI18n(); const ko = language === 'ko'
  const user = useAuthStore(s => s.currentUser)
  const [school, setSchool] = useState<School | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [formError, setFormError] = useState('')
  const qc = useQueryClient()
  const applications = useQuery({ queryKey: ['calendar', user?.id, 'applications'], queryFn: calendarApi.applications, enabled: !registration && !!user })
  const mutation = useMutation({ mutationFn: (task: () => Promise<unknown>) => task(), onSuccess: async () => {
    setSubmitted(true); await qc.invalidateQueries({ queryKey: ['calendar', user?.id] })
  } })
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setFormError('')
    if (!school) { setFormError(ko ? '학교를 선택해 주세요.' : 'Choose a university.'); return }
    const f = new FormData(e.currentTarget); const s = (key: string) => String(f.get(key) || '').trim()
    if (registration) mutation.mutate(() => calendarApi.registerSchool({ universityId: school.id, department: s('department'), message: s('message'), nickname: s('nickname'), name: s('name'), email: s('email'), password: String(f.get('password') || '') }))
    else mutation.mutate(() => calendarApi.apply({ universityId: school.id, department: s('department'), message: s('message'), contactEmail: s('email') }))
  }
  const statusNames = ko ? { PENDING: '검토 대기', APPROVED: '승인됨', REJECTED: '반려됨', REVOKED: '권한 해제됨' } : { PENDING: 'Pending review', APPROVED: 'Approved', REJECTED: 'Rejected', REVOKED: 'Access revoked' }
  return <main className={`cal-page ${registration ? 'cal-signup' : ''}`}>
    {registration && <Brand />}
    <header className="cal-heading"><div><span className="cal-eyebrow">OVLO FOR UNIVERSITIES</span><h1>{ko ? '학교 담당자 신청' : 'University staff application'}</h1><p>{ko ? '국제처·교류 담당 부서의 소식을 학생들에게 전달하세요.' : 'Share news from your international office with students.'}</p></div></header>
    <section className="cal-panel"><p>{ko ? '계정 생성 → 담당자 확인 → 학교 일정 관리 권한 부여' : 'Create an account → Staff verification → University event access'}</p><p className="cal-hint">{ko ? '학교 공식 이메일과 담당 업무를 남겨 주세요. 운영진이 학교 소속·권한을 확인한 후 승인합니다. 신청만으로 학교의 공식 대표 권한이 부여되지는 않습니다.' : 'Provide your university work email and responsibilities. An administrator verifies your affiliation and authority before approval.'}</p>
      {registration && <p><Link href="/login?next=/university-account">{ko ? '이미 계정이 있다면 로그인 후 신청' : 'Already registered? Sign in to apply'}</Link></p>}
      {submitted ? <div role="status"><h2>{ko ? '신청을 접수했습니다.' : 'Application received.'}</h2><p>{ko ? '승인 전에는 일반 회원 권한으로 이용할 수 있습니다. 로그인 후 이 화면에서 검토 결과를 확인하세요.' : 'You can use member features while your application is reviewed. Check this page after signing in for the result.'}</p>
        {registration ? <Link href="/login?next=/university-account">{ko ? '로그인하고 신청 확인' : 'Sign in to view application'}</Link> : <button onClick={() => { setSubmitted(false); setSchool(null); mutation.reset() }}>{ko ? '다른 학교 신청' : 'Apply for another university'}</button>}</div>
      : <form onSubmit={submit} className="cal-form">
        {registration && <><label>{ko ? '담당자 이름' : 'Your name'}<input name="name" required maxLength={100} autoComplete="name" /></label><label>{ko ? '닉네임' : 'Username'}<input name="nickname" required minLength={2} maxLength={30} pattern="[a-zA-Z0-9._]+" autoComplete="username" /><small>{ko ? '영문·숫자·점·밑줄, 2~30자' : 'Letters, numbers, . or _, 2–30 characters'}</small></label></>}
        <label>{ko ? '학교 업무 이메일' : 'University work email'}<input name="email" type="email" required maxLength={254} defaultValue={!registration ? user?.email : ''} autoComplete="email" /></label>
        {registration && <label>{ko ? '비밀번호' : 'Password'}<input name="password" type="password" required minLength={8} maxLength={100} autoComplete="new-password" /><small>{ko ? '8자 이상, 대문자·소문자·숫자·특수문자(@$!%*?&_#^()-) 포함' : '8+ characters with upper/lowercase, a number and a symbol (@$!%*?&_#^()-)'}</small></label>}
        <div className="cal-wide"><SchoolPicker value={school} onChange={setSchool} ko={ko} /></div>
        <label className="cal-wide">{ko ? '담당 부서' : 'Department'}<input name="department" required maxLength={200} placeholder={ko ? '예: 국제교류처' : 'e.g. International Office'} /></label>
        <label className="cal-wide">{ko ? '담당 업무와 확인 가능한 공식 연락처·안내' : 'Responsibilities and official contact / verification details'}<textarea name="message" required maxLength={2000} rows={4} placeholder={ko ? '학교 직원 안내 페이지, 담당 업무 등을 적어 주세요. 비밀번호나 신분증은 보내지 마세요.' : 'Include your staff directory page and responsibilities. Do not submit passwords or identity documents.'} /></label>
        {formError && <p className="cal-error cal-wide" role="alert">{formError}</p>}{mutation.isError && <p className="cal-error cal-wide" role="alert">{calendarError(mutation.error)}</p>}
        <div className="cal-actions cal-wide"><button type="submit" className="cal-primary" disabled={mutation.isPending}>{mutation.isPending ? (ko ? '신청 중…' : 'Submitting…') : registration ? (ko ? '계정 만들고 담당자 신청' : 'Create account and apply') : (ko ? '담당자 신청' : 'Submit application')}</button>{registration && <Link href="/register">{ko ? '가입 유형 선택' : 'Choose account type'}</Link>}</div>
      </form>}
    </section>
    {!registration && <section className="cal-panel"><div className="cal-row"><h2>{ko ? '내 신청 내역' : 'My applications'}</h2><button onClick={() => applications.refetch()}>{ko ? '새로고침' : 'Refresh'}</button></div>
      {applications.isPending && <p role="status">{ko ? '불러오는 중…' : 'Loading…'}</p>}{applications.isError && <p role="alert" className="cal-error">{calendarError(applications.error)}</p>}
      {applications.isSuccess && applications.data.length === 0 && <p>{ko ? '아직 신청한 내역이 없습니다.' : 'No applications yet.'}</p>}
      {applications.data?.map(a => <article key={a.id} className="cal-panel"><div className="cal-row"><strong>{a.universityName}</strong><span className="cal-badge">{statusNames[a.status]}</span></div><p>{a.department} · {a.contactEmail}</p>{a.reviewNote && <p>{a.reviewNote}</p>}{a.status === 'APPROVED' && <Link href="/calendar/manage">{ko ? '학교 일정 관리하기' : 'Manage university events'}</Link>}{(a.status === 'REJECTED' || a.status === 'REVOKED') && <p className="cal-hint">{ko ? '안내 내용을 확인하고 위 양식에서 다시 신청할 수 있습니다.' : 'Review the note and reapply using the form above.'}</p>}</article>)}
    </section>}
  </main>
}
