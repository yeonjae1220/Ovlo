'use client'

import { useCallback, useMemo, useState } from 'react'
import { DEFAULT_THEME, THEME_STORAGE_KEY } from './themeConfig'
import { ThemeContext, type ThemeMode } from './themeContext'

// themeInitScript가 페인트 전에 이미 <html data-theme>를 localStorage 기준으로 적용한다.
// 그 값을 그대로 초기 state로 읽어, effect 내 setState 없이 동기화한다.
function getInitialTheme(): ThemeMode {
  if (typeof document === 'undefined') return DEFAULT_THEME
  const applied = document.documentElement.dataset.theme
  if (applied === 'light' || applied === 'dark') return applied
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
  return stored === 'light' || stored === 'dark' ? stored : DEFAULT_THEME
}

function applyTheme(theme: ThemeMode) {
  const root = document.documentElement
  root.dataset.themeSwitch = ''
  root.dataset.theme = theme
  root.style.colorScheme = theme
  // transition이 끝난 뒤 switching 플래그 제거 (160ms + 여유)
  setTimeout(() => delete root.dataset.themeSwitch, 200)
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(getInitialTheme)

  const setTheme = useCallback((nextTheme: ThemeMode) => {
    setThemeState(nextTheme)
    window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
    applyTheme(nextTheme)
  }, [])

  const value = useMemo(() => ({ theme, setTheme }), [theme, setTheme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
