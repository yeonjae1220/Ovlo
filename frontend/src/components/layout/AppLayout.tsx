'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState, type ReactNode } from 'react'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'
import { Avatar } from '../ui'
import { Brand } from '../ui/Brand'
import { ThemeToggle } from '../ui/ThemeToggle'
import { UiLanguageSwitcher } from '../ui/UiLanguageSwitcher'

type NavIconName = 'home' | 'globe' | 'chat' | 'profile' | 'calendar' | 'market'

function NavIcon({ name }: { name: NavIconName }) {
  if (name === 'market') return <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none"><path d="M4 8h16l-1 13H5L4 8ZM8 8V6a4 4 0 0 1 8 0v2" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/></svg>
  if (name === 'calendar') return <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none"><rect x="4" y="5" width="16" height="16" rx="2" stroke="currentColor" strokeWidth="1.8"/><path d="M8 3v4M16 3v4M4 10h16M8 14h3M13 14h3M8 17h3" stroke="currentColor" strokeWidth="1.8"/></svg>
  if (name === 'home') {
    return (
      <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none">
        <path d="m4 10.5 8-6.5 8 6.5v8.25a1.25 1.25 0 0 1-1.25 1.25H5.25A1.25 1.25 0 0 1 4 18.75V10.5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M9.5 20v-6h5v6" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      </svg>
    )
  }
  if (name === 'globe') {
    return (
      <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.8" />
        <path d="M3.8 12h16.4M12 3.5c2.3 2.3 3.4 5.1 3.4 8.5S14.3 18.2 12 20.5C9.7 18.2 8.6 15.4 8.6 12S9.7 5.8 12 3.5Z" stroke="currentColor" strokeWidth="1.5" />
      </svg>
    )
  }
  if (name === 'chat') {
    return (
      <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none">
        <path d="M5.2 5.2h13.6v10.1H10l-4.8 4v-14Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M8.5 9h7M8.5 12h4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      </svg>
    )
  }
  return (
    <svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="8" r="3.25" stroke="currentColor" strokeWidth="1.8" />
      <path d="M5.5 20c.5-4 2.7-6 6.5-6s6 2 6.5 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

export default function AppLayout({ children }: { children: ReactNode }) {
  const { t, language } = useI18n()
  const { currentUser } = useAuthStore()
  const pathname = usePathname()
  const [moreOpen, setMoreOpen] = useState(false)
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? t('nav.profile')

  const navItems: Array<{ to: string; label: string; icon: NavIconName }> = [
    { to: '/boards', label: t('nav.home'), icon: 'home' },
    { to: '/marketplace', label: language === 'ko' ? '중고거래' : 'Market', icon: 'market' },
    { to: '/exchange-universities', label: t('nav.exchange'), icon: 'globe' },
    { to: '/chat', label: t('nav.chat'), icon: 'chat' },
    { to: '/calendar', label: language === 'ko' ? '캘린더' : 'Calendar', icon: 'calendar' },
    { to: '/my-university', label: t('nav.myUniversity'), icon: 'globe' },
  ]

  const isActive = (path: string) => pathname !== null && (pathname === path || pathname.startsWith(path + '/'))

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__inner">
          <Brand compact />

          <nav className="app-nav-desktop" aria-label="Primary">
            {navItems.map(({ to, label }) => (
              <Link key={to} href={to} className={isActive(to) ? 'app-nav-link app-nav-link--active' : 'app-nav-link'}>
                {label}
              </Link>
            ))}
            {currentUser && (
              <Link
                href={`/profile/${currentUser.id}`}
                className={isActive(`/profile/${currentUser.id}`) ? 'app-nav-link app-nav-link--active' : 'app-nav-link'}
              >
                {t('nav.profile')}
              </Link>
            )}
          </nav>

          <div className="app-header__tools">
            <span className="app-header__theme"><ThemeToggle /></span>
            <UiLanguageSwitcher />
            {currentUser && (
              <Link className="app-user" href={`/profile/${currentUser.id}`} title={userLabel}>
                <Avatar label={userLabel} size="sm" />
                <span className="app-user-label">{userLabel}</span>
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="app-main">
        <div className="app-main__inner">{children}</div>
      </main>

      <nav className="app-nav-mobile" aria-label="Primary">
        {navItems.filter(({ to }) => ['/boards', '/marketplace', '/chat', '/calendar'].includes(to)).map(({ to, label, icon }) => (
          <Link key={to} href={to} className={isActive(to) ? 'app-mobile-link app-mobile-link--active' : 'app-mobile-link'}>
            <NavIcon name={icon} />
            <span>{label}</span>
          </Link>
        ))}
        <div className="app-mobile-more" onKeyDown={event => { if (event.key === 'Escape') setMoreOpen(false) }}>
          <button type="button" className="app-mobile-link" aria-expanded={moreOpen} aria-controls="mobile-more-links" onClick={() => setMoreOpen(open => !open)}>
            <NavIcon name="profile"/><span>{language === 'ko' ? '더보기' : 'More'}</span>
          </button>
          {moreOpen && <div id="mobile-more-links" className="app-mobile-more__links">
            {navItems.filter(({ to }) => ['/exchange-universities', '/my-university'].includes(to)).map(({to,label}) => <Link key={to} href={to} onClick={() => setMoreOpen(false)}>{label}</Link>)}
            {currentUser && <Link href={`/profile/${currentUser.id}`} onClick={() => setMoreOpen(false)}>{t('nav.profile')}</Link>}
          </div>}
        </div>
      </nav>
    </div>
  )
}
