'use client'

import { useState } from 'react'
import type { FormEvent } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '../store/authStore'
import { useBreakpoint } from '../hooks/useBreakpoint'
import { InstallBanner } from '../components/InstallBanner'
import { useI18n } from '../i18n/I18nProvider'
import { Badge, Card, LinkButton, SearchBox } from '../components/ui'

const C = {
  elevated: 'var(--color-bg-elevated)',
  surface: 'var(--color-surface)',
  accent: 'var(--color-accent)',
  accentStrong: 'var(--color-accent-strong)',
  accentSoft: 'var(--color-accent-subtle)',
  border: 'var(--color-border)',
  text: 'var(--color-text)',
  textSecondary: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  warm: 'var(--color-warm)',
}

export default function LandingPage() {
  const { t } = useI18n()
  const router = useRouter()
  const { currentUser, accessToken } = useAuthStore()
  const { isMobile, isTablet } = useBreakpoint()
  const [query, setQuery] = useState('')
  const isLoggedIn = !!accessToken && !!currentUser
  const compactHeader = isMobile || isTablet
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? ''

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    router.push(trimmed ? `/exchange-universities?q=${encodeURIComponent(trimmed)}` : '/exchange-universities')
  }

  const quickCards = [
    { title: t('landing.feature1.title'), body: t('landing.feature1.desc'), accent: C.accent },
    { title: t('landing.feature3.title'), body: t('landing.feature3.desc'), accent: C.warm },
    { title: t('landing.feature4.title'), body: t('landing.feature4.desc'), accent: 'var(--color-info)' },
  ]

  return (
    <div style={{ minHeight: '100vh', color: C.textSecondary, display: 'flex', flexDirection: 'column' }}>
      <header style={{
        padding: compactHeader ? '14px 16px' : '16px 32px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        borderBottom: `1px solid ${C.border}`,
        position: 'sticky',
        top: 0,
        background: 'color-mix(in srgb, var(--color-bg-elevated) 92%, transparent)',
        backdropFilter: 'blur(14px)',
        zIndex: 10,
      }}>
        <Link href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: 8, fontSize: 21, fontWeight: 900, color: C.text, textDecoration: 'none' }}>
          <span style={{ width: 28, height: 28, borderRadius: 9, background: C.accentStrong, color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>O</span>
          Ovlo
        </Link>
        <nav style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
          <Link href="/exchange-universities" style={{ padding: '9px 12px', color: C.muted, fontSize: 14, fontWeight: 750, textDecoration: 'none' }}>
            {t('landing.nav.search')}
          </Link>
          {isLoggedIn ? (
            <LinkButton href="/boards" variant="primary" icon="⌂">
              {userLabel || t('landing.cta.community')}
            </LinkButton>
          ) : (
            <>
              <LinkButton href="/login" variant="ghost">{t('landing.nav.login')}</LinkButton>
              <LinkButton href="/register" variant="primary">{t('landing.nav.register')}</LinkButton>
            </>
          )}
        </nav>
      </header>

      <main style={{ flex: 1 }}>
        <section style={{
          maxWidth: 1080,
          margin: '0 auto',
          padding: compactHeader ? '42px 16px 28px' : '72px 32px 42px',
          display: 'grid',
          gridTemplateColumns: compactHeader ? '1fr' : 'minmax(0, 1.1fr) minmax(320px, .9fr)',
          gap: compactHeader ? 28 : 42,
          alignItems: 'center',
        }}>
          <div>
            <Badge tone="accent">{t('landing.badge')}</Badge>
            <h1 style={{
              margin: '18px 0 16px',
              color: C.text,
              fontSize: compactHeader ? 38 : 56,
              lineHeight: 1.08,
              fontWeight: 950,
              maxWidth: 680,
            }}>
              {t('landing.hero.title1')}<br />
              <span style={{ color: C.accent }}>{t('landing.hero.title2')}</span>
            </h1>
            <p style={{ maxWidth: 620, margin: '0 0 24px', color: C.muted, fontSize: 17, lineHeight: 1.75 }}>
              {t('landing.hero.desc')}
            </p>

            <form
              action="/exchange-universities"
              method="get"
              onSubmit={handleSearch}
              style={{ display: 'grid', gridTemplateColumns: compactHeader ? '1fr' : '1fr auto', gap: 10, maxWidth: 640, marginBottom: 16 }}
            >
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

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
              <LinkButton href={isLoggedIn ? '/boards' : '/register'} variant="secondary" icon="⌂">
                {isLoggedIn ? t('landing.cta.community') : t('landing.cta.start')}
              </LinkButton>
              <span style={{ color: C.dim, fontSize: 13 }}>
                {t('landing.stats.universities')} 78 · {t('landing.stats.reviews')} 2,800+
              </span>
            </div>
          </div>

          <Card style={{ padding: compactHeader ? 18 : 22, background: 'color-mix(in srgb, var(--color-surface) 96%, transparent)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', marginBottom: 18 }}>
              <div>
                <div style={{ color: C.dim, fontSize: 12, fontWeight: 800, marginBottom: 4 }}>{t('landing.features.title')}</div>
                <div style={{ color: C.text, fontSize: 20, fontWeight: 900 }}>{t('landing.cta2.title')}</div>
              </div>
              <Badge tone="warning">{t('landing.stats.continentsNum')}</Badge>
            </div>
            <div style={{ display: 'grid', gap: 12 }}>
              {quickCards.map((card) => (
                <div key={card.title} style={{ display: 'grid', gridTemplateColumns: '4px 1fr', gap: 12, padding: '12px 0', borderTop: `1px solid ${C.border}` }}>
                  <span style={{ borderRadius: 999, background: card.accent }} />
                  <div>
                    <div style={{ color: C.text, fontWeight: 850, fontSize: 14 }}>{card.title}</div>
                    <p style={{ margin: '4px 0 0', color: C.muted, fontSize: 13, lineHeight: 1.55 }}>{card.body}</p>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </section>

        <section style={{ maxWidth: 1080, margin: '0 auto', padding: compactHeader ? '0 16px 42px' : '0 32px 58px' }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: 12,
            marginBottom: 18,
          }}>
            {([
              { num: '78', labelKey: 'landing.stats.universities' },
              { num: '2,800+', labelKey: 'landing.stats.reviews' },
              { num: t('landing.stats.continentsNum'), labelKey: 'landing.stats.continents' },
            ] as const).map((s) => (
              <Card key={s.labelKey} style={{ padding: '18px 18px' }}>
                <div style={{ fontSize: 28, lineHeight: 1, fontWeight: 950, color: C.accent }}>{s.num}</div>
                <div style={{ fontSize: 13, color: C.muted, marginTop: 7, fontWeight: 750 }}>{t(s.labelKey)}</div>
              </Card>
            ))}
          </div>
          <div style={{ maxWidth: 520, margin: '0 auto' }}>
            <InstallBanner />
          </div>
        </section>
      </main>

      <footer style={{
        borderTop: `1px solid ${C.border}`,
        padding: compactHeader ? '18px 16px' : '22px 32px',
        textAlign: 'center',
        color: C.dim,
        fontSize: 13,
      }}>
        {t('landing.footer')}
      </footer>
    </div>
  )
}
