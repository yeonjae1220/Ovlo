'use client'

import { useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { authApi } from '../api/auth'
import { memberApi } from '../api/member'
import { useAuthStore } from '../store/authStore'
import { refreshAuth } from '../utils/refreshAuth'
import type { CompleteOnboardingRequest } from '../types'

export function useLogin() {
  const { setAuth, setAccessToken } = useAuthStore()
  const router = useRouter()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      authApi.login(email, password),
    onSuccess: async (token) => {
      setAccessToken(token.accessToken)
      const user = await memberApi.getById(String(token.memberId))
      setAuth(token.accessToken, user)
      router.push('/boards')
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

  useEffect(() => {
    if (!accessToken) return

    let expiryMs: number
    try {
      // base64url → base64 변환 후 디코딩 (JWT는 base64url 인코딩 사용)
      const base64 = accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
      const payload = JSON.parse(atob(base64))
      expiryMs = payload.exp * 1000
    } catch {
      return
    }

    // 만료 1분 전에 갱신 (이미 1분 이내라면 건너뜀 — axios interceptor가 처리)
    const msUntilRefresh = expiryMs - Date.now() - 60_000
    if (msUntilRefresh <= 0) return

    const timer = setTimeout(() => {
      // 다른 재발급 경로(layout·axios interceptor·다른 탭)와 겹치지 않도록 반드시 싱글톤으로 보낸다.
      // 일시적 실패는 refreshAuth 가 재시도한 뒤 throw 한다 — 이때는 로그인 상태를 유지하고,
      // 다음 API 호출의 401 을 interceptor 가 다시 처리한다.
      refreshAuth()
        .then((token) => {
          if (!token) window.location.href = '/login'
        })
        .catch(() => {})
    }, msUntilRefresh)

    return () => clearTimeout(timer)
  }, [accessToken])
}

export function useRegister() {
  const router = useRouter()

  return useMutation({
    mutationFn: memberApi.register,
    onSuccess: () => router.push('/login'),
  })
}

export function useGoogleLogin() {
  const { setAuth, setAccessToken } = useAuthStore()
  const router = useRouter()

  return useMutation({
    mutationFn: ({ code, redirectUri }: { code: string; redirectUri: string }) =>
      authApi.googleLogin(code, redirectUri),
    onSuccess: async (result) => {
      setAccessToken(result.accessToken)
      const user = await memberApi.getById(String(result.memberId))
      setAuth(result.accessToken, user)
      router.push(result.newMember ? '/onboarding' : '/boards')
    },
  })
}

export function useCompleteOnboarding() {
  const { setCurrentUser } = useAuthStore()
  const router = useRouter()

  return useMutation({
    mutationFn: (req: CompleteOnboardingRequest) => memberApi.completeOnboarding(req),
    onSuccess: (updatedUser) => {
      setCurrentUser(updatedUser)
      router.push('/boards')
    },
  })
}
