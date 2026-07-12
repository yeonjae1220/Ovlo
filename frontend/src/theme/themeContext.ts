import { createContext, useContext } from 'react'
import type { ThemePreference } from './themeConfig'

export type ThemeMode = 'light' | 'dark'
export type { ThemePreference }

export interface ThemeContextValue {
  preference: ThemePreference
  resolvedTheme: ThemeMode
  setPreference: (preference: ThemePreference) => void
  // 하위호환: 기존 소비처(ProfilePage)가 theme/setTheme(2-way)를 사용한다.
  theme: ThemeMode
  setTheme: (theme: ThemeMode) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('useTheme must be used within ThemeProvider')
  return value
}
