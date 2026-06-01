'use client'

import { useState } from 'react'
import { usePWAInstall } from '../hooks/usePWAInstall'
import { useI18n } from '../i18n/I18nProvider'

// 앱스토어 출시 후 URL을 채워 넣으세요
const APP_STORE_URL = ''
const PLAY_STORE_URL = ''
const C = {
  surface: 'var(--color-surface)',
  accent: 'var(--color-accent-strong)',
  accentSoft: 'var(--color-accent-subtle)',
  border: 'var(--color-border-strong)',
  text: 'var(--color-text)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
}

export function InstallBanner() {
  const { t } = useI18n()
  const { installPrompt, isInstalled, platform, triggerInstall } = usePWAInstall()
  const [showIOSGuide, setShowIOSGuide] = useState(false)
  const [dismissed, setDismissed] = useState(false)

  if (isInstalled || dismissed) return null

  const hasNativeApp = platform === 'ios' ? !!APP_STORE_URL : !!PLAY_STORE_URL
  const showPWAButton = platform === 'android' ? !!installPrompt : platform === 'ios'

  if (!showPWAButton && !hasNativeApp) return null

  const iosSteps = [
    { step: '1', titleKey: 'install.step1.title' as const, descKey: 'install.step1.desc' as const },
    { step: '2', titleKey: 'install.step2.title' as const, descKey: 'install.step2.desc' as const },
    { step: '3', titleKey: 'install.step3.title' as const, descKey: 'install.step3.desc' as const },
  ]

  return (
    <>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '12px 16px',
        background: C.accentSoft,
        border: `1px solid ${C.border}`,
        borderRadius: 16,
        marginTop: 16,
      }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: C.text }}>{t('install.title')}</p>
          <p style={{ margin: '2px 0 0', fontSize: 11, color: C.muted }}>{t('install.desc')}</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          {showPWAButton && (
            <button
              onClick={platform === 'ios' ? () => setShowIOSGuide(true) : triggerInstall}
              style={{
                padding: '6px 12px',
                background: C.accent,
                color: 'white',
                border: 'none',
                borderRadius: 8,
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {platform === 'ios' ? t('install.ios.btn') : t('install.android.btn')}
            </button>
          )}
          {platform === 'android' && PLAY_STORE_URL && (
            <a href={PLAY_STORE_URL} target="_blank" rel="noopener noreferrer" style={{
              padding: '6px 12px',
              background: C.surface,
              color: C.text,
              border: `1px solid ${C.border}`,
              borderRadius: 8,
              fontSize: 12,
              fontWeight: 600,
              textDecoration: 'none',
            }}>
              Play Store
            </a>
          )}
          {platform === 'ios' && APP_STORE_URL && (
            <a href={APP_STORE_URL} target="_blank" rel="noopener noreferrer" style={{
              padding: '6px 12px',
              background: C.surface,
              color: C.text,
              border: `1px solid ${C.border}`,
              borderRadius: 8,
              fontSize: 12,
              fontWeight: 600,
              textDecoration: 'none',
            }}>
              App Store
            </a>
          )}
          <button
            onClick={() => setDismissed(true)}
            aria-label={t('install.close')}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.dim, padding: 4, lineHeight: 1 }}
          >
            ✕
          </button>
        </div>
      </div>

      {showIOSGuide && (
        <div
          onClick={() => setShowIOSGuide(false)}
          style={{
            position: 'fixed', inset: 0, zIndex: 9999,
            display: 'flex', alignItems: 'flex-end', justifyContent: 'center',
            background: 'rgba(0,0,0,0.6)', padding: 16,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              width: '100%', maxWidth: 400,
              background: C.surface, borderRadius: 24,
              padding: '24px 24px 32px', boxShadow: 'var(--shadow-soft)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
              <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: C.text }}>{t('install.guide.title')}</h2>
              <button onClick={() => setShowIOSGuide(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.dim, fontSize: 18 }}>✕</button>
            </div>
            <ol style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>
              {iosSteps.map(({ step, titleKey, descKey }) => (
                <li key={step} style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <span style={{
                    flexShrink: 0, width: 24, height: 24, borderRadius: '50%',
                    background: C.accent, color: 'white', fontSize: 11, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 2,
                  }}>{step}</span>
                  <div>
                    <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: C.text }}>{t(titleKey)}</p>
                    <p style={{ margin: '2px 0 0', fontSize: 11, color: C.muted }}>{t(descKey)}</p>
                  </div>
                </li>
              ))}
            </ol>
            <button
              onClick={() => setShowIOSGuide(false)}
              style={{
                marginTop: 24, width: '100%', padding: '12px 0',
                background: C.accent, color: 'white', border: 'none',
                borderRadius: 12, fontSize: 14, fontWeight: 600, cursor: 'pointer',
              }}
            >
              {t('install.confirm')}
            </button>
          </div>
        </div>
      )}
    </>
  )
}
