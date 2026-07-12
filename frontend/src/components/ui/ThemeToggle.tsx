'use client'

import { useTheme } from '@/theme/themeContext'
import type { ThemePreference } from '@/theme/themeConfig'
import { useI18n } from '@/i18n/I18nProvider'
import type { MessageKey } from '@/i18n/messages'

const OPTIONS: { value: ThemePreference; icon: string; key: MessageKey }[] = [
  { value: 'system', icon: '🖥️', key: 'profile.theme.system' },
  { value: 'light', icon: '☀️', key: 'profile.theme.light' },
  { value: 'dark', icon: '🌙', key: 'profile.theme.dark' },
]

/**
 * system/light/dark 3-way 토글. 저장된 선호가 없으면 dark가 기본이지만
 * 사용자는 언제든 system(OS 따름)·light로 바꿀 수 있다.
 */
export function ThemeToggle() {
  const { preference, setPreference } = useTheme()
  const { t } = useI18n()

  return (
    <div
      role="group"
      aria-label={t('profile.theme')}
      style={{
        display: 'inline-flex',
        gap: 2,
        padding: 2,
        borderRadius: 9,
        background: 'var(--color-surface-soft)',
        border: '1px solid var(--color-border)',
      }}
    >
      {OPTIONS.map((o) => {
        const selected = preference === o.value
        return (
          <button
            key={o.value}
            type="button"
            onClick={() => setPreference(o.value)}
            aria-pressed={selected}
            title={t(o.key)}
            style={{
              position: 'relative',
              width: 30,
              height: 28,
              borderRadius: 7,
              border: 'none',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 14,
              background: selected ? 'var(--color-bg-elevated)' : 'transparent',
              boxShadow: selected ? 'var(--shadow-soft)' : 'none',
            }}
          >
            <span aria-hidden>{o.icon}</span>
            <span
              style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}
            >
              {t(o.key)}
            </span>
          </button>
        )
      })}
    </div>
  )
}
