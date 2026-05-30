'use client'

import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import { refreshAuth } from './refreshAuth'

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 → refreshAuth() 싱글톤 사용 — layout과 중복 호출 없음
let pendingQueue: Array<{ resolve: (t: string) => void; reject: (e: unknown) => void }> = []

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // 403 = 권한 없음 — 토큰 자체는 유효하므로 auth 클리어 안 함
    if (status !== 401 || original._retry) return Promise.reject(error)

    original._retry = true

    // refreshAuth가 이미 진행 중이면 같은 Promise를 기다림
    const newToken = await refreshAuth()

    if (!newToken) {
      // refresh 실패 — pendingQueue 거절
      pendingQueue.forEach(({ reject }) => reject(error))
      pendingQueue = []
      if (typeof window !== 'undefined') window.location.href = '/login'
      return Promise.reject(error)
    }

    // 대기 중이던 요청들 재시도
    pendingQueue.forEach(({ resolve }) => resolve(newToken))
    pendingQueue = []
    original.headers.Authorization = `Bearer ${newToken}`
    return apiClient(original)
  }
)

export default apiClient
