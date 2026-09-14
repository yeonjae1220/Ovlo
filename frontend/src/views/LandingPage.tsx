'use client'

import { useState } from 'react'
import type { FormEvent } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '../store/authStore'
import { useI18n } from '../i18n/I18nProvider'
import { Badge, LinkButton, SearchBox } from '../components/ui'
import { Brand } from '../components/ui/Brand'
import { UiLanguageSwitcher } from '../components/ui/UiLanguageSwitcher'
import { ThemeToggle } from '../components/ui/ThemeToggle'

export default function LandingPage() {
  const { t } = useI18n()
  const router = useRouter()
  const { currentUser, accessToken } = useAuthStore()
  const [query, setQuery] = useState('')
  const isLoggedIn = !!accessToken && !!currentUser
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? ''

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    router.push(trimmed ? `/exchange-universities?q=${encodeURIComponent(trimmed)}` : '/exchange-universities')
  }

  const journeyItems = [
    { number: '01', title: t('landing.feature1.title'), body: t('landing.feature1.desc') },
    { number: '02', title: t('landing.feature3.title'), body: t('landing.feature3.desc') },
    { number: '03', title: t('landing.feature4.title'), body: t('landing.feature4.desc') },
  ]

  return (
    <div className="landing-shell">
      <header className="landing-header">
        <div className="landing-header__inner">
          <Brand />
          <nav className="landing-nav" aria-label="Primary">
            <span className="landing-nav__theme"><ThemeToggle /></span>
            <UiLanguageSwitcher />
            <Link className="landing-nav__search" href="/exchange-universities">
              {t('landing.nav.search')}
            </Link>
            {isLoggedIn ? (
              <LinkButton href="/boards" variant="primary">
                {userLabel || t('landing.cta.community')}
              </LinkButton>
            ) : (
              <>
                <Link className="landing-nav__login" href="/login">{t('landing.nav.login')}</Link>
                <LinkButton href="/register" variant="primary">{t('landing.nav.register')}</LinkButton>
              </>
            )}
          </nav>
        </div>
      </header>

      <main>
        <section className="landing-hero">
          <div className="landing-hero__copy">
            <Badge tone="accent">{t('landing.badge')}</Badge>
            <h1 className="landing-hero__title">
              {t('landing.hero.title1')}
              <span>{t('landing.hero.title2')}</span>
            </h1>
            <p className="landing-hero__description">{t('landing.hero.desc')}</p>

            <form className="landing-search" action="/exchange-universities" method="get" onSubmit={handleSearch}>
              <SearchBox
                name="q"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t('univ.search.placeholder')}
                aria-label={t('landing.nav.search')}
              />
              <button className="ui-button ui-button--primary" type="submit">
                {t('landing.cta.search')}
              </button>
            </form>

            <div className="landing-hero__actions">
              <LinkButton href={isLoggedIn ? '/boards' : '/register'} variant="secondary">
                {isLoggedIn ? t('landing.cta.community') : t('landing.cta.start')}
              </LinkButton>
              <div className="landing-proof">
                <span className="landing-proof__rule" aria-hidden="true" />
                <span>{t('landing.feature1.title')} · {t('landing.feature2.title')}</span>
              </div>
            </div>
          </div>

          <aside className="field-note" aria-label={t('landing.feature1.title')}>
            <div className="field-note__topline">
              <span>{t('landing.fieldNote.example')}</span>
              <span>OVLO / 01</span>
            </div>

            <div className="field-note__route" aria-hidden="true">
              <div><strong>ICN</strong><span>HOME</span></div>
              <div className="field-note__route-line"><i /><i /><i /></div>
              <div><strong>BER</strong><span>EXCHANGE</span></div>
            </div>

            <div className="field-note__decision-grid">
              <div><span>{t('exch.detail.cost')}</span><strong>€ 930</strong></div>
              <div><span>{t('exch.detail.dorm')}</span><strong>{t('exch.detail.dormAvailable')}</strong></div>
              <div><span>{t('exch.detail.language')}</span><strong>B2</strong></div>
            </div>

            <div className="field-note__story">
              <p>{t('landing.feature2.desc')}</p>
            </div>

            <div className="field-note__footer">
              <Badge tone="success">✓ {t('verification.badge.exchange')}</Badge>
              <span>{t('landing.feature1.title')}</span>
            </div>
          </aside>
        </section>

        <section className="landing-journey">
          <div className="landing-section-heading">
            <span>{t('landing.features.title')}</span>
            <h2>{t('landing.cta2.title')}</h2>
          </div>
          <ol className="landing-journey__list">
            {journeyItems.map((item) => (
              <li key={item.number}>
                <span className="landing-journey__number">{item.number}</span>
                <h3>{item.title}</h3>
                <p>{item.body}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="landing-end-cta" aria-label={t('landing.cta2.title')}>
          <div>
            <span>{t('landing.cta2.title')}</span>
            <strong>{t('landing.cta2.desc')}</strong>
          </div>
          <LinkButton href={isLoggedIn ? '/boards' : '/register'} variant="primary">
            {isLoggedIn ? t('landing.cta.community') : t('landing.cta2.register')}
          </LinkButton>
        </section>
      </main>

      <footer className="landing-footer">{t('landing.footer')}</footer>
    </div>
  )
}
