'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { CSSProperties, ReactNode } from 'react'
import { useAuthStore } from '../../store/authStore'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import { useI18n } from '../../i18n/I18nProvider'
import { Avatar } from '../ui'

export default function AppLayout({ children }: { children: ReactNode }) {
  const { t } = useI18n()
  const { currentUser } = useAuthStore()
  const { isMobile } = useBreakpoint()
  const pathname = usePathname()
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? t('nav.profile')

  const NAV_ITEMS = [
    { to: '/boards', labelKey: 'nav.home' as const, icon: '⌂' },
    { to: '/exchange-universities', labelKey: 'nav.exchange' as const, icon: '⌕' },
    { to: '/chat', labelKey: 'nav.chat' as const, icon: '◦' },
  ]

  const isActive = (path: string) => pathname !== null && (pathname === path || pathname.startsWith(path + '/'))

  const desktopLinkStyle = (active: boolean): CSSProperties => ({
    padding: '8px 13px',
    borderRadius: 8,
    textDecoration: 'none',
    fontSize: 14,
    fontWeight: active ? 800 : 650,
    color: active ? 'var(--color-accent)' : 'var(--color-text-muted)',
    background: active ? 'var(--color-accent-subtle)' : 'transparent',
    transition: 'color 0.15s, background 0.15s',
  })

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--color-bg)', color: 'var(--color-text)' }}>
      <header
        style={{
          padding: isMobile ? '10px 14px' : '12px 24px',
          borderBottom: '1px solid var(--color-border)',
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          position: 'sticky',
          top: 0,
          background: 'color-mix(in srgb, var(--color-bg-elevated) 92%, transparent)',
          backdropFilter: 'blur(14px)',
          zIndex: 100,
        }}
      >
        <Link href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: 8, fontWeight: 900, fontSize: 21, textDecoration: 'none', color: 'var(--color-text)', flexShrink: 0 }}>
          <span style={{ width: 28, height: 28, borderRadius: 9, background: 'var(--color-accent-strong)', color: 'var(--color-on-accent)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>O</span>
          Ovlo
        </Link>

        {!isMobile && (
          <nav style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
            {NAV_ITEMS.map(({ to, labelKey }) => (
              <Link key={to} href={to} style={desktopLinkStyle(isActive(to))}>
                {t(labelKey)}
              </Link>
            ))}
            {currentUser && (
              <Link href={`/profile/${currentUser.id}`} style={desktopLinkStyle(isActive(`/profile/${currentUser.id}`))}>
                {t('nav.profile')}
              </Link>
            )}
          </nav>
        )}

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 12, alignItems: 'center' }}>
          {currentUser && (
            <Link
              href={`/profile/${currentUser.id}`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                color: 'var(--color-text-secondary)',
                fontSize: 14,
                fontWeight: 750,
                textDecoration: 'none',
                maxWidth: isMobile ? 120 : 220,
                overflow: 'hidden',
              }}
              title={userLabel}
            >
              <Avatar label={userLabel} size="sm" />
              {!isMobile && <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{userLabel}</span>}
            </Link>
          )}
        </div>
      </header>

      <main
        style={{
          flex: 1,
          padding: isMobile ? '18px 14px' : '34px 24px',
          paddingBottom: isMobile ? 'calc(22px + 72px)' : '34px',
        }}
      >
        <div style={{ maxWidth: 'var(--layout-max)', margin: '0 auto', width: '100%' }}>
          {children}
        </div>
      </main>

      {isMobile && (
        <nav
          style={{
            borderTop: '1px solid var(--color-border)',
            padding: '7px 8px max(7px, env(safe-area-inset-bottom))',
            display: 'flex',
            gap: 6,
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            background: 'color-mix(in srgb, var(--color-bg-elevated) 94%, transparent)',
            backdropFilter: 'blur(14px)',
            zIndex: 100,
          }}
        >
          {NAV_ITEMS.map(({ to, labelKey, icon }) => (
            <Link
              key={to}
              href={to}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 3,
                textDecoration: 'none',
                fontSize: 11,
                fontWeight: 800,
                color: isActive(to) ? 'var(--color-accent)' : 'var(--color-text-muted)',
                background: isActive(to) ? 'var(--color-accent-subtle)' : 'transparent',
                borderRadius: 10,
                padding: '7px 6px',
                minWidth: 0,
                flex: 1,
              }}
            >
              <span aria-hidden="true" style={{ fontSize: 18, lineHeight: 1 }}>{icon}</span>
              {t(labelKey)}
            </Link>
          ))}
          {currentUser && (
            <Link
              href={`/profile/${currentUser.id}`}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 3,
                textDecoration: 'none',
                fontSize: 11,
                fontWeight: 800,
                color: isActive(`/profile/${currentUser.id}`) ? 'var(--color-accent)' : 'var(--color-text-muted)',
                background: isActive(`/profile/${currentUser.id}`) ? 'var(--color-accent-subtle)' : 'transparent',
                borderRadius: 10,
                padding: '7px 6px',
                minWidth: 0,
                flex: 1,
              }}
            >
              <span aria-hidden="true" style={{ fontSize: 18, lineHeight: 1 }}>◎</span>
              {t('nav.profile')}
            </Link>
          )}
        </nav>
      )}
    </div>
  )
}
