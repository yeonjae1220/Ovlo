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

// 401 → refreshAuth() 싱글톤 — layout·선제 갱신 타이머와 같은 요청을 공유한다
apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // 403 = 권한 없음 — 토큰 자체는 유효하므로 auth 클리어 안 함
    if (status !== 401 || !original || original._retry) return Promise.reject(error)

    original._retry = true

    let newToken: string | null
    try {
      newToken = await refreshAuth()
    } catch {
      // 일시적 실패 — 로그인 상태는 유지하고 이 요청만 실패시킨다
      return Promise.reject(error)
    }

    if (!newToken) {
      // 세션 종료 — refreshAuth 가 이미 인증 상태를 지웠다
      if (typeof window !== 'undefined') window.location.href = '/login'
      return Promise.reject(error)
    }

    original.headers.Authorization = `Bearer ${newToken}`
    return apiClient(original)
  }
)

export default apiClient
