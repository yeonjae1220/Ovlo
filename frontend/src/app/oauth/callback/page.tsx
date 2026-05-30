'use client'

import { Suspense } from 'react'
import OAuthCallbackPage from '@/views/auth/OAuthCallbackPage'

export default function Page() {
  return (
    <Suspense fallback={null}>
      <OAuthCallbackPage />
    </Suspense>
  )
}
