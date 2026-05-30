'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import AppLayout from '@/components/layout/AppLayout'

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, currentUser } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  // Zustand persist는 클라이언트에서 hydrate되므로, 초기 렌더 시 상태가 없을 수 있음
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    setHydrated(true)
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

  // hydration 전에는 빈 화면 대신 로딩 상태 표시
  if (!hydrated) return null
  if (!accessToken || !currentUser) return null

  return <AppLayout>{children}</AppLayout>
}
