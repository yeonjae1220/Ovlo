'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import { refreshAuth } from '@/utils/refreshAuth'
import AppLayout from '@/components/layout/AppLayout'

function AuthSkeleton() {
  return (
    <div style={{ minHeight: '100vh', background: '#0c0c0c', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, width: 300 }}>
        {[80, 60, 60, 40].map((w, i) => (
          <div key={i} style={{
            height: 14, borderRadius: 7, background: '#1e1e1e',
            width: `${w}%`, animation: 'pulse 1.4s ease infinite',
            animationDelay: `${i * 0.1}s`,
          }} />
        ))}
      </div>
      <style>{`@keyframes pulse { 0%,100%{opacity:.4} 50%{opacity:.8} }`}</style>
    </div>
  )
}

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, currentUser, clearAuth } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    const init = async () => {
      if (!accessToken) {
        // currentUser는 localStorage에서 복원됨 — token만 재발급
        // refreshAuth() 싱글톤: layout과 axios interceptor가 동시 호출해도 1회만 실행
        const token = await refreshAuth()
        if (!token) clearAuth()
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

  if (!hydrated) return <AuthSkeleton />
  if (!accessToken || !currentUser) return null

  return <AppLayout>{children}</AppLayout>
}
