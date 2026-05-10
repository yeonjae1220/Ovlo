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
      version: 3,
      migrate: () => ({ accessToken: null, currentUser: null }),
      partialize: (state) => ({
        accessToken: state.accessToken,
        currentUser: state.currentUser,
      }),
    }
  )
)
