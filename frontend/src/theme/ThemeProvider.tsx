'use client'

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { DEFAULT_PREFERENCE, THEME_STORAGE_KEY, type ThemePreference } from './themeConfig'
import { ThemeContext, type ThemeMode } from './themeContext'

const THEME_QUERY = '(prefers-color-scheme: dark)'

function normalizePreference(value: string | null): ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system' ? value : DEFAULT_PREFERENCE
}

function systemTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'dark'
  return window.matchMedia(THEME_QUERY).matches ? 'dark' : 'light'
}

function resolve(preference: ThemePreference, sys: ThemeMode = systemTheme()): ThemeMode {
  return preference === 'system' ? sys : preference
}

// themeInitScript가 페인트 전에 <html data-theme>를 이미 적용한다. 그 값을 초기 state로 읽어
// 첫 렌더 불일치를 최소화한다(선호값은 localStorage에서 별도로 읽음).
function getInitialPreference(): ThemePreference {
  if (typeof document === 'undefined') return DEFAULT_PREFERENCE
  return normalizePreference(window.localStorage.getItem(THEME_STORAGE_KEY))
}

function getInitialResolved(): ThemeMode {
  if (typeof document === 'undefined') return 'dark'
  const applied = document.documentElement.dataset.theme
  return applied === 'light' || applied === 'dark' ? applied : 'dark'
}

function applyTheme(resolved: ThemeMode) {
  const root = document.documentElement
  root.dataset.themeSwitch = ''
  root.dataset.theme = resolved
  root.style.colorScheme = resolved
  // transition이 끝난 뒤 switching 플래그 제거 (160ms + 여유)
  setTimeout(() => delete root.dataset.themeSwitch, 200)
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(getInitialPreference)
  const [resolvedTheme, setResolvedTheme] = useState<ThemeMode>(getInitialResolved)
  const preferenceRef = useRef<ThemePreference>(preference)

  // system 선호일 때 OS 테마 변경을 실시간 반영
  useEffect(() => {
    const media = window.matchMedia(THEME_QUERY)
    const onChange = (e: MediaQueryListEvent) => {
      if (preferenceRef.current !== 'system') return
      const next: ThemeMode = e.matches ? 'dark' : 'light'
      setResolvedTheme(next)
      applyTheme(next)
    }
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  const setPreference = useCallback((next: ThemePreference) => {
    preferenceRef.current = next
    setPreferenceState(next)
    window.localStorage.setItem(THEME_STORAGE_KEY, next)
    const resolved = resolve(next)
    setResolvedTheme(resolved)
    applyTheme(resolved)
  }, [])

  const value = useMemo(
    () => ({
      preference,
      resolvedTheme,
      setPreference,
      theme: resolvedTheme,
      setTheme: (t: ThemeMode) => setPreference(t),
    }),
    [preference, resolvedTheme, setPreference],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
