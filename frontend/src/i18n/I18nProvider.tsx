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

export function I18nProvider({
  children,
  initialLanguage,
}: {
  children: React.ReactNode
  initialLanguage?: UiLanguage
}) {
  // 서버가 쿠키로 결정한 언어를 초기값으로 사용(SSR/hydration 일치).
  // 서버 prop이 없으면 클라이언트 쿠키 → 'en' 순으로 폴백.
  const [language, setLanguageState] = useState<UiLanguage>(() => {
    if (initialLanguage) return initialLanguage
    const cookie = readLangCookie()
    return cookie ? resolveUiLang(cookie) : 'en'
  })

  useEffect(() => {
    // 쿠키 > localStorage > 브라우저 순으로 해석하고 쿠키에 백필해
    // 기존(쿠키 없는) 사용자도 다음 로드부터 서버 SSR <html lang>이 정확해진다.
    const resolved = resolveUiLang(readLangCookie() ?? localStorage.getItem(STORAGE_KEY) ?? navigator.language)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (resolved !== language) setLanguageState(resolved)
    localStorage.setItem(STORAGE_KEY, resolved)
    writeLangCookie(resolved)
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
