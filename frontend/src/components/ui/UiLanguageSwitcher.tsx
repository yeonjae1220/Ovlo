'use client'

import { useI18n } from '@/i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES, LANGUAGE_LABELS } from '@/i18n/messages'

/**
 * 비로그인 사용자도 UI 언어를 바꿀 수 있는 경량 스위처.
 * 네이티브 <select>라 키보드/스크린리더 접근성이 기본 보장된다.
 */
export function UiLanguageSwitcher() {
  const { language, setLanguage } = useI18n()

  return (
    <select
      value={language}
      onChange={(e) => setLanguage(e.target.value)}
      aria-label="Language"
      style={{
        padding: '8px 10px',
        borderRadius: 9,
        fontSize: 13,
        fontWeight: 700,
        color: 'var(--color-text)',
        background: 'var(--color-input-bg)',
        border: '1px solid var(--color-border)',
        cursor: 'pointer',
      }}
    >
      {SUPPORTED_UI_LANGUAGES.map((code) => (
        <option key={code} value={code}>
          {LANGUAGE_LABELS[code]}
        </option>
      ))}
    </select>
  )
}
