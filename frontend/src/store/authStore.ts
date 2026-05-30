'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Member } from '../types'

interface AuthState {
  accessToken: string | null
  currentUser: Member | null
  setAuth: (accessToken: string, user: Member) => void
  setAccessToken: (token: string) => void
  setCurrentUser: (user: Member) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      currentUser: null,

      setAuth: (accessToken, user) =>
        set({ accessToken, currentUser: user }),

      setAccessToken: (token) => set({ accessToken: token }),

      setCurrentUser: (user) => set({ currentUser: user }),

      clearAuth: () =>
        set({ accessToken: null, currentUser: null }),
    }),
    {
      name: 'ovlo-auth',
      version: 4,
      // accessToken은 XSS 탈취 위험으로 localStorage 저장 제외 — 메모리에만 유지
      // 페이지 리로드 시 httpOnly 쿠키의 refresh token으로 재발급
      migrate: () => ({ accessToken: null, currentUser: null }),
      partialize: (state) => ({
        currentUser: state.currentUser,
      }),
    }
  )
)
