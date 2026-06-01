'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { messages, resolveUiLang } from './messages'
import type { MessageKey, UiLanguage } from './messages'

const STORAGE_KEY = 'ovlo_lang'
const COOKIE_KEY = 'ovlo_lang'

// 쿠키에서 언어를 읽음 — document.cookie는 클라이언트에서 동기적으로 접근 가능
// SSR 초기 HTML과 클라이언트 첫 렌더의 값 불일치(FOUC)를 최소화
function readLangCookie(): string | null {
  if (typeof document === 'undefined') return null
  const m = document.cookie.match(new RegExp(`(?:^|; )${COOKIE_KEY}=([^;]*)`))
  return m ? decodeURIComponent(m[1]) : null
}

function writeLangCookie(lang: string): void {
  document.cookie = `${COOKIE_KEY}=${lang}; path=/; max-age=31536000; SameSite=Lax`
}

type I18nContextValue = {
  language: UiLanguage
  setLanguage: (lang: string) => void
  t: (key: MessageKey, vars?: Record<string, string | number>) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: React.ReactNode }) {
  // 쿠키가 있으면 동기적으로 읽어 FOUC를 최소화, 없으면 'en' (hydration safe)
  const [language, setLanguageState] = useState<UiLanguage>(() => {
    const cookie = readLangCookie()
    return cookie ? resolveUiLang(cookie) : 'en'
  })

  useEffect(() => {
    // localStorage가 쿠키보다 최신이면 동기화 — 외부 저장소(localStorage)와의 의도된 mount-time 동기화
    const stored = resolveUiLang(localStorage.getItem(STORAGE_KEY) ?? navigator.language)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (stored !== language) setLanguageState(stored)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo<I18nContextValue>(() => ({
    language,
    setLanguage: (next) => {
      const normalized = resolveUiLang(next)
      setLanguageState(normalized)
      localStorage.setItem(STORAGE_KEY, normalized)
      writeLangCookie(normalized)
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

// import가 많아 별도 파일 분리 시 churn이 큼 — Fast Refresh(DX) 전용 룰만 타게팅 비활성
// eslint-disable-next-line react-refresh/only-export-components
export function useI18n() {
  const value = useContext(I18nContext)
  if (!value) throw new Error('useI18n must be used within I18nProvider')
  return value
}
