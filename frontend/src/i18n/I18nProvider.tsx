'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { messages, resolveUiLang } from './messages'
import type { MessageKey, UiLanguage } from './messages'

const STORAGE_KEY = 'ovlo_lang'

type I18nContextValue = {
  language: UiLanguage
  setLanguage: (lang: string) => void
  t: (key: MessageKey, vars?: Record<string, string | number>) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<UiLanguage>(() => {
    if (typeof window === 'undefined') return 'en'
    return resolveUiLang(localStorage.getItem(STORAGE_KEY) ?? navigator.language)
  })

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo<I18nContextValue>(() => ({
    language,
    setLanguage: (next) => {
      const normalized = resolveUiLang(next)
      setLanguageState(normalized)
      localStorage.setItem(STORAGE_KEY, normalized)
    },
    t: (key, vars) => {
      const raw = messages[language][key] ?? messages.en[key] ?? key
      if (!vars) return raw
      return Object.entries(vars).reduce<string>(
        (acc, [k, v]) => acc.replace(`{${k}}`, String(v)),
        raw
      )
    },
  }), [language])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const value = useContext(I18nContext)
  if (!value) throw new Error('useI18n must be used within I18nProvider')
  return value
}
