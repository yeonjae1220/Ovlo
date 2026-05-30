'use client'

import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import AppLayout from '@/components/layout/AppLayout'

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, currentUser } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (!accessToken || !currentUser) {
      router.replace('/login')
      return
    }
    if (currentUser.status === 'PENDING_ONBOARDING' && pathname !== '/onboarding') {
      router.replace('/onboarding')
    }
  }, [accessToken, currentUser, pathname, router])

  if (!accessToken || !currentUser) return null

  return <AppLayout>{children}</AppLayout>
}
