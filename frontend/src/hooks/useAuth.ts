import { useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { authApi } from '../api/auth'
import { memberApi } from '../api/member'
import { useAuthStore } from '../store/authStore'
import type { CompleteOnboardingRequest } from '../types'

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
      setAuth(token.accessToken, user)
      navigate('/boards')
    },
  })
}

/**
 * Access Token 만료 1분 전에 선제적으로 갱신.
 * WebSocket처럼 REST 호출이 드문 환경에서도 토큰이 유효하게 유지된다.
 * App 루트(또는 인증이 필요한 레이아웃)에서 한 번만 호출하면 된다.
 */
export function useProactiveRefresh() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const clearAuth = useAuthStore((s) => s.clearAuth)

  useEffect(() => {
    if (!accessToken) return

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

    const timer = setTimeout(() => {
      // 인터셉터 루프를 피하기 위해 raw axios 사용
      // 쿠키(refresh_token)가 자동 전송됨
      let attempts = 0
      const tryRefresh = async () => {
        try {
          const { data } = await axios.post<{ accessToken: string }>(
            '/api/v1/auth/refresh',
            undefined,
            { withCredentials: true }
          )
          setAccessToken(data.accessToken)
        } catch (err: unknown) {
          const status = (err as { response?: { status?: number } })?.response?.status
          if (status && status >= 400 && status < 500) {
            // 쿠키가 만료됨 → 로그아웃
            clearAuth()
            window.location.href = '/login'
            return
          }
          // 일시적 오류 → 재시도 (최대 3회, 2s / 4s / 6s 간격)
          attempts++
          if (attempts < 3) {
            setTimeout(tryRefresh, 2000 * attempts)
          } else {
            clearAuth()
            window.location.href = '/login'
          }
        }
      }
      tryRefresh()
    }, msUntilRefresh)

    return () => clearTimeout(timer)
  }, [accessToken, setAccessToken, clearAuth])
}

export function useRegister() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: memberApi.register,
    onSuccess: () => navigate('/login'),
  })
}

export function useGoogleLogin() {
  const { setAuth, setAccessToken } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: ({ code, redirectUri }: { code: string; redirectUri: string }) =>
      authApi.googleLogin(code, redirectUri),
    onSuccess: async (result) => {
      setAccessToken(result.accessToken)
      const user = await memberApi.getById(String(result.memberId))
      setAuth(result.accessToken, user)
      navigate(result.newMember ? '/onboarding' : '/boards')
    },
  })
}

export function useCompleteOnboarding() {
  const { setCurrentUser } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (req: CompleteOnboardingRequest) => memberApi.completeOnboarding(req),
    onSuccess: (updatedUser) => {
      setCurrentUser(updatedUser)
      navigate('/boards')
    },
  })
}
