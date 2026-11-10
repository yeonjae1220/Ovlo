'use client'
import Link from 'next/link'
import { Brand } from '@/components/ui/Brand'
import { useI18n } from '@/i18n/I18nProvider'
import '@/views/calendar/calendar.css'
export default function SignupChoicePage() {
  const { language } = useI18n(); const ko = language === 'ko'
  return <main className="cal-page cal-signup"><Brand /><header className="cal-heading"><div><span className="cal-eyebrow">WELCOME TO OVLO</span><h1>{ko ? '어떻게 함께할까요?' : 'How will you join Ovlo?'}</h1><p>{ko ? '학생의 교환생활과 학교의 소식을 연결합니다.' : 'Connecting exchange students and their universities.'}</p></div></header>
    <div className="cal-signup-choices"><Link href="/register/member" className="cal-signup-choice"><strong>{ko ? '일반 회원 가입' : 'Join as a member'}</strong><span>{ko ? '교환학생 커뮤니티, 학교 일정과 개인 캘린더를 이용하세요.' : 'Join the community and keep university dates and personal plans together.'}</span><b>{ko ? '회원으로 시작 →' : 'Get started →'}</b></Link>
      <Link href="/register/university" className="cal-signup-choice"><strong>{ko ? '학교 담당자 가입' : 'Join as university staff'}</strong><span>{ko ? '학교 공식 일정을 등록하고 학생들에게 소식을 전하세요.' : 'Publish university events and keep students informed.'}</span><b>{ko ? '학교 담당자로 신청 →' : 'Apply as staff →'}</b></Link></div>
    <p className="cal-hint">{ko ? '학교 담당자는 운영진 확인 후 해당 학교의 일정 관리 권한을 받습니다.' : 'University staff receive event management access after administrator review.'}</p>
    <p>{ko ? '이미 계정이 있나요?' : 'Already have an account?'} <Link href="/login">{ko ? '로그인' : 'Sign in'}</Link> · <Link href="/university-account">{ko ? '기존 계정으로 학교 담당자 신청' : 'Apply with an existing account'}</Link></p>
  </main>
}
