import { useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { authApi } from '../api/auth'
import { memberApi } from '../api/member'
import { useAuthStore } from '../store/authStore'

export function useLogin() {
  const { setAuth, setAccessToken } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      authApi.login(email, password),
    onSuccess: async (token) => {
      // 토큰을 먼저 store에 저장해야 axios 인터셉터가 Authorization 헤더를 붙일 수 있다
      setAccessToken(token.accessToken)
      const user = await memberApi.getById(String(token.memberId))
      setAuth(token.accessToken, token.refreshToken, user)
      navigate('/')
    },
  })
}

/**
 * Access Token 만료 1분 전에 선제적으로 갱신.
 * WebSocket 전용 세션처럼 REST 호출이 드문 환경에서도 토큰이 유효하게 유지된다.
 * App 루트(또는 인증이 필요한 레이아웃)에서 한 번만 호출하면 된다.
 */
export function useProactiveRefresh() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const clearAuth = useAuthStore((s) => s.clearAuth)

  useEffect(() => {
    if (!accessToken || !refreshToken) return

    let expiryMs: number
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]))
      expiryMs = payload.exp * 1000
    } catch {
      return
    }

    // 만료 1분 전에 갱신 (이미 1분 이내라면 건너뜀 — axios interceptor가 처리)
    const msUntilRefresh = expiryMs - Date.now() - 60_000
    if (msUntilRefresh <= 0) return

    const timer = setTimeout(async () => {
      try {
        // 인터셉터 루프를 피하기 위해 raw axios 사용
        const { data } = await axios.post<{ accessToken: string }>('/api/v1/auth/refresh', { refreshToken })
        setAccessToken(data.accessToken)
      } catch {
        clearAuth()
        window.location.href = '/login'
      }
    }, msUntilRefresh)

    return () => clearTimeout(timer)
  }, [accessToken, refreshToken, setAccessToken, clearAuth])
}

export function useRegister() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: memberApi.register,
    onSuccess: () => navigate('/login'),
  })
}
