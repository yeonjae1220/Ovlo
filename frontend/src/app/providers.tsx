'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { registerAuthQueryClient } from '@/utils/authQueryCache'
import { watchAuthSessionChanges } from '@/utils/authSession'
import { I18nProvider } from '@/i18n/I18nProvider'
import type { UiLanguage } from '@/i18n/messages'
import { InstallBanner } from '@/components/InstallBanner'
import { NativeShell } from '@/components/NativeShell'
import { ThemeProvider } from '@/theme/ThemeProvider'

export function Providers({
  children,
  initialLanguage,
}: {
  children: React.ReactNode
  initialLanguage?: UiLanguage
}) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 1000 * 60, retry: 1 },
        },
      })
  )

  useEffect(() => registerAuthQueryClient(queryClient), [queryClient])
  useEffect(watchAuthSessionChanges, [])

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <I18nProvider initialLanguage={initialLanguage}>
          {children}
          <InstallBanner />
          <NativeShell />
        </I18nProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
