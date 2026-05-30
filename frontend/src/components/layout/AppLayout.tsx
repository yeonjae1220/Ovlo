'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuthStore } from '../../store/authStore'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import { useI18n } from '../../i18n/I18nProvider'

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { t } = useI18n()
  const { currentUser } = useAuthStore()
  const { isMobile } = useBreakpoint()
  const pathname = usePathname()
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? t('nav.profile')

  const NAV_ITEMS = [
    { to: '/boards', labelKey: 'nav.home' as const },
    { to: '/exchange-universities', labelKey: 'nav.exchange' as const },
    { to: '/chat', labelKey: 'nav.chat' as const },
  ]

  const isActive = (path: string) => pathname !== null && (pathname === path || pathname.startsWith(path + '/'))

  const desktopLinkStyle = (active: boolean): React.CSSProperties => ({
    padding: '6px 12px',
    borderRadius: 6,
    textDecoration: 'none',
    fontSize: 14,
    fontWeight: active ? 600 : 400,
    color: active ? '#a78bfa' : '#94a3b8',
    background: active ? '#7c3aed22' : 'transparent',
    transition: 'color 0.15s, background 0.15s',
  })

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid #2d3748',
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          position: 'sticky',
          top: 0,
          background: '#242424',
          zIndex: 100,
        }}
      >
        <Link href="/" style={{ fontWeight: 'bold', fontSize: 20, textDecoration: 'none', color: '#a78bfa', flexShrink: 0 }}>
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
                color: '#94a3b8',
                fontSize: 14,
                textDecoration: 'none',
                maxWidth: isMobile ? 120 : 220,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={userLabel}
            >
              {userLabel}
            </Link>
          )}
        </div>
      </header>

      <main
        style={{
          flex: 1,
          padding: isMobile ? '16px 16px' : '32px 24px',
          paddingBottom: isMobile ? 'calc(16px + 60px)' : '32px',
        }}
      >
        <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
          {children}
        </div>
      </main>

      {isMobile && (
        <nav
          style={{
            borderTop: '1px solid #2d3748',
            padding: '8px 0',
            display: 'flex',
            justifyContent: 'space-around',
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            background: '#242424',
            zIndex: 100,
          }}
        >
          {NAV_ITEMS.map(({ to, labelKey }) => (
            <Link
              key={to}
              href={to}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textDecoration: 'none',
                fontSize: 12,
                color: isActive(to) ? '#a78bfa' : '#94a3b8',
                padding: '4px 8px',
                minWidth: 0,
                flex: 1,
              }}
            >
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
                textDecoration: 'none',
                fontSize: 12,
                color: isActive(`/profile/${currentUser.id}`) ? '#a78bfa' : '#94a3b8',
                padding: '4px 8px',
                minWidth: 0,
                flex: 1,
              }}
            >
              {t('nav.profile')}
            </Link>
          )}
        </nav>
      )}
    </div>
  )
}
