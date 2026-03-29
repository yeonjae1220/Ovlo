import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Member } from '../types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  currentUser: Member | null
  setAuth: (accessToken: string, refreshToken: string, user: Member) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  setAccessToken: (token: string) => void
  setCurrentUser: (user: Member) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      currentUser: null,

      setAuth: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, currentUser: user }),

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),

      setAccessToken: (token) => set({ accessToken: token }),

      setCurrentUser: (user) => set({ currentUser: user }),

      clearAuth: () =>
        set({ accessToken: null, refreshToken: null, currentUser: null }),
    }),
    {
      name: 'ovlo-auth',
      version: 2, // bump when Member type changes to clear stale localStorage
      migrate: () => ({ accessToken: null, refreshToken: null, currentUser: null }),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        currentUser: state.currentUser,
      }),
    }
  )
)
