'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import axios from 'axios'
import { useAuthStore } from '@/store/authStore'
import { memberApi } from '@/api/member'
import AppLayout from '@/components/layout/AppLayout'

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, currentUser, setAuth, clearAuth } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    // accessToken은 메모리 전용 — 리로드 시 httpOnly 쿠키로 재발급 시도
    const init = async () => {
      if (!accessToken) {
        try {
          const { data } = await axios.post<{ accessToken: string; memberId: number }>(
            '/api/v1/auth/refresh',
            undefined,
            { withCredentials: true }
          )
          const user = await memberApi.getById(String(data.memberId))
          setAuth(data.accessToken, user)
        } catch {
          clearAuth()
        }
      }
      setHydrated(true)
    }
    init()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!hydrated) return
    if (!accessToken || !currentUser) {
      router.replace('/login')
      return
    }
    if (currentUser.status === 'PENDING_ONBOARDING' && pathname !== '/onboarding') {
      router.replace('/onboarding')
    }
  }, [hydrated, accessToken, currentUser, pathname, router])

  if (!hydrated) return null
  if (!accessToken || !currentUser) return null

  return <AppLayout>{children}</AppLayout>
}
