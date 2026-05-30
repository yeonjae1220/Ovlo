'use client'

import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

// 전체 앱에서 refresh 호출을 단 하나의 Promise로 묶음
// layout과 axios interceptor가 동시에 호출해도 실제 요청은 1회만 발생
let pending: Promise<string | null> | null = null

export async function refreshAuth(): Promise<string | null> {
  if (pending) return pending

  pending = axios
    .post<{ accessToken: string }>('/api/v1/auth/refresh', undefined, { withCredentials: true })
    .then(({ data }) => {
      useAuthStore.getState().setAccessToken(data.accessToken)
      return data.accessToken
    })
    .catch(() => {
      useAuthStore.getState().clearAuth()
      return null
    })
    .finally(() => {
      pending = null
    })

  return pending
}
