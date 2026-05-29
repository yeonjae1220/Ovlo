import { Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useBreakpoint } from '../hooks/useBreakpoint'
import { InstallBanner } from '../components/InstallBanner'
import { useI18n } from '../i18n/I18nProvider'

export default function LandingPage() {
  const { t } = useI18n()
  const { currentUser, accessToken } = useAuthStore()
  const { isMobile, isTablet } = useBreakpoint()
  const isLoggedIn = !!accessToken && !!currentUser
  const compactHeader = isMobile || isTablet
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? '?'

  const FEATURES = [
    { icon: '🎓', titleKey: 'landing.feature1.title' as const, descKey: 'landing.feature1.desc' as const },
    { icon: '✈️', titleKey: 'landing.feature2.title' as const, descKey: 'landing.feature2.desc' as const },
    { icon: '💬', titleKey: 'landing.feature3.title' as const, descKey: 'landing.feature3.desc' as const },
    { icon: '🤝', titleKey: 'landing.feature4.title' as const, descKey: 'landing.feature4.desc' as const },
  ]

  return (
    <div style={{ minHeight: '100vh', fontFamily: 'system-ui, -apple-system, sans-serif', color: '#e2e8f0', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <header style={{
        padding: compactHeader ? '16px 16px' : '18px 32px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        gap: 12, flexWrap: 'wrap', borderBottom: '1px solid #2d3748',
        position: 'sticky', top: 0, background: '#242424', zIndex: 10,
      }}>
        <span style={{ fontSize: 22, fontWeight: 800, color: '#a78bfa', letterSpacing: '-0.5px' }}>Ovlo</span>
        <nav style={{
          display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap',
          width: compactHeader ? '100%' : 'auto',
          justifyContent: compactHeader ? 'flex-end' : 'flex-start',
        }}>
          <Link to="/exchange-universities" style={{
            padding: '8px 16px', borderRadius: 8, color: '#94a3b8', fontSize: 14,
            textDecoration: 'none', flex: compactHeader ? '1 1 100%' : undefined, textAlign: 'center',
          }}>
            {t('landing.nav.search')}
          </Link>
          {isLoggedIn ? (
            <Link to="/boards" style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '7px 16px', borderRadius: 8,
              background: '#7c3aed22', border: '1px solid #7c3aed44',
              color: '#a78bfa', fontSize: 14, fontWeight: 600, textDecoration: 'none',
              maxWidth: compactHeader ? '100%' : 260,
            }} title={userLabel}>
              <span style={{
                width: 26, height: 26, borderRadius: '50%', background: '#7c3aed',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 12, color: '#fff', fontWeight: 700,
              }}>
                {userLabel[0].toUpperCase()}
              </span>
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{userLabel}</span>
            </Link>
          ) : (
            <>
              <Link to="/login" style={{
                padding: '8px 16px', borderRadius: 8, color: '#94a3b8', fontSize: 14,
                textDecoration: 'none', flex: compactHeader ? '1 1 calc(50% - 4px)' : undefined, textAlign: 'center',
              }}>
                {t('landing.nav.login')}
              </Link>
              <Link to="/register" style={{
                padding: '8px 20px', borderRadius: 8, background: '#7c3aed',
                color: '#fff', fontSize: 14, fontWeight: 600, textDecoration: 'none',
                flex: compactHeader ? '1 1 calc(50% - 4px)' : undefined, textAlign: 'center',
              }}>
                {t('landing.nav.register')}
              </Link>
            </>
          )}
        </nav>
      </header>

      {/* Hero */}
      <section style={{
        flex: 1, maxWidth: 820, margin: '0 auto',
        padding: compactHeader ? '56px 16px 48px' : '80px 32px 64px', textAlign: 'center',
      }}>
        <div style={{
          display: 'inline-block', padding: '5px 14px', borderRadius: 20,
          background: '#7c3aed22', color: '#a78bfa', fontSize: 13, fontWeight: 600,
          marginBottom: 28, border: '1px solid #7c3aed44',
        }}>
          {t('landing.badge')}
        </div>

        <h1 style={{ fontSize: 'clamp(32px, 6vw, 52px)', fontWeight: 900, lineHeight: 1.18, margin: '0 0 22px', color: '#f1f5f9', letterSpacing: '-1px' }}>
          {t('landing.hero.title1')}
          <br />
          <span style={{ color: '#a78bfa' }}>Ovlo</span>{t('landing.hero.title2').replace('Ovlo', '')}
        </h1>

        <p style={{ fontSize: 17, color: '#94a3b8', lineHeight: 1.75, margin: '0 0 44px', maxWidth: 560, marginLeft: 'auto', marginRight: 'auto' }}>
          {t('landing.hero.desc')}
        </p>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/exchange-universities" style={{
            padding: '14px 30px', borderRadius: 10, background: '#7c3aed',
            color: '#fff', fontSize: 15, fontWeight: 700, textDecoration: 'none',
            boxShadow: '0 4px 20px #7c3aed55',
          }}>
            {t('landing.cta.search')}
          </Link>
          {isLoggedIn ? (
            <Link to="/boards" style={{
              padding: '14px 30px', borderRadius: 10, border: '1.5px solid #7c3aed44',
              color: '#a78bfa', fontSize: 15, fontWeight: 500, textDecoration: 'none',
            }}>
              {t('landing.cta.community')}
            </Link>
          ) : (
            <Link to="/register" style={{
              padding: '14px 30px', borderRadius: 10, border: '1.5px solid #374151',
              color: '#e2e8f0', fontSize: 15, fontWeight: 500, textDecoration: 'none',
            }}>
              {t('landing.cta.start')}
            </Link>
          )}
        </div>

        <div style={{ maxWidth: 400, margin: '0 auto' }}>
          <InstallBanner />
        </div>

        {/* Stats */}
        <div style={{ display: 'flex', gap: 32, justifyContent: 'center', marginTop: 56, flexWrap: 'wrap' }}>
          {([
            { num: '78', labelKey: 'landing.stats.universities' },
            { num: '2,800+', labelKey: 'landing.stats.reviews' },
            { num: t('landing.stats.continentsNum'), labelKey: 'landing.stats.continents' },
          ] as const).map((s) => (
            <div key={s.labelKey} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 26, fontWeight: 800, color: '#a78bfa' }}>{s.num}</div>
              <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>{t(s.labelKey)}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section style={{
        maxWidth: 960, margin: '0 auto',
        padding: compactHeader ? '0 16px 56px' : '0 32px 80px',
        width: '100%', boxSizing: 'border-box',
      }}>
        <h2 style={{ fontSize: compactHeader ? 20 : 22, fontWeight: 700, color: '#f1f5f9', textAlign: 'center', marginBottom: 32 }}>
          {t('landing.features.title')}
        </h2>
        <div style={{
          display: 'grid',
          gridTemplateColumns: `repeat(auto-fit, minmax(${isMobile ? 180 : 210}px, 1fr))`,
          gap: 16,
        }}>
          {FEATURES.map((f) => (
            <div key={f.titleKey} style={{
              padding: compactHeader ? '24px 18px' : '28px 22px',
              borderRadius: 12, border: '1px solid #2d3748', background: '#1e2836',
            }}>
              <div style={{ fontSize: 30, marginBottom: 14 }}>{f.icon}</div>
              <h3 style={{ fontSize: 15, fontWeight: 700, color: '#f1f5f9', margin: '0 0 8px' }}>{t(f.titleKey)}</h3>
              <p style={{ fontSize: 13, color: '#94a3b8', lineHeight: 1.65, margin: 0 }}>{t(f.descKey)}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA Banner (non-logged-in only) */}
      {!isLoggedIn && (
        <section style={{
          background: '#1e1433', borderTop: '1px solid #2d3748', borderBottom: '1px solid #2d3748',
          padding: compactHeader ? '40px 16px' : '48px 32px', textAlign: 'center',
        }}>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: '#f1f5f9', margin: '0 0 12px' }}>
            {t('landing.cta2.title')}
          </h2>
          <p style={{ fontSize: 15, color: '#94a3b8', margin: '0 0 28px' }}>
            {t('landing.cta2.desc')}
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/register" style={{
              padding: '12px 28px', borderRadius: 9, background: '#7c3aed',
              color: '#fff', fontSize: 14, fontWeight: 700, textDecoration: 'none',
            }}>
              {t('landing.cta2.register')}
            </Link>
            <Link to="/login" style={{
              padding: '12px 28px', borderRadius: 9, border: '1.5px solid #374151',
              color: '#cbd5e1', fontSize: 14, textDecoration: 'none',
            }}>
              {t('landing.cta2.login')}
            </Link>
          </div>
        </section>
      )}

      {/* Footer */}
      <footer style={{
        borderTop: '1px solid #1e293b',
        padding: compactHeader ? '18px 16px' : '22px 32px',
        textAlign: 'center', color: '#475569', fontSize: 13,
      }}>
        {t('landing.footer')}
      </footer>
    </div>
  )
}
