'use client'

import { useState } from 'react'
import { usePWAInstall } from '../hooks/usePWAInstall'
import { useI18n } from '../i18n/I18nProvider'

// 앱스토어 출시 후 URL을 채워 넣으세요
const APP_STORE_URL = ''
const PLAY_STORE_URL = ''

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
        background: '#2d1f5e',
        border: '1px solid #4c1d95',
        borderRadius: 16,
        marginTop: 16,
      }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: '#e2e8f0' }}>{t('install.title')}</p>
          <p style={{ margin: '2px 0 0', fontSize: 11, color: '#94a3b8' }}>{t('install.desc')}</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          {showPWAButton && (
            <button
              onClick={platform === 'ios' ? () => setShowIOSGuide(true) : triggerInstall}
              style={{
                padding: '6px 12px',
                background: '#7c3aed',
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
              background: '#1e293b',
              color: '#e2e8f0',
              border: '1px solid #334155',
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
              background: '#1e293b',
              color: '#e2e8f0',
              border: '1px solid #334155',
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
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b', padding: 4, lineHeight: 1 }}
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
              background: '#1e1e2e', borderRadius: 24,
              padding: '24px 24px 32px', boxShadow: '0 25px 50px rgba(0,0,0,0.5)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
              <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#e2e8f0' }}>{t('install.guide.title')}</h2>
              <button onClick={() => setShowIOSGuide(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b', fontSize: 18 }}>✕</button>
            </div>
            <ol style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>
              {iosSteps.map(({ step, titleKey, descKey }) => (
                <li key={step} style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <span style={{
                    flexShrink: 0, width: 24, height: 24, borderRadius: '50%',
                    background: '#7c3aed', color: 'white', fontSize: 11, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 2,
                  }}>{step}</span>
                  <div>
                    <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: '#e2e8f0' }}>{t(titleKey)}</p>
                    <p style={{ margin: '2px 0 0', fontSize: 11, color: '#94a3b8' }}>{t(descKey)}</p>
                  </div>
                </li>
              ))}
            </ol>
            <button
              onClick={() => setShowIOSGuide(false)}
              style={{
                marginTop: 24, width: '100%', padding: '12px 0',
                background: '#7c3aed', color: 'white', border: 'none',
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
