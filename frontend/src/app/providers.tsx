'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { I18nProvider } from '@/i18n/I18nProvider'
import { InstallBanner } from '@/components/InstallBanner'
import { ThemeProvider } from '@/theme/ThemeProvider'

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 1000 * 60, retry: 1 },
        },
      })
  )

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <I18nProvider>
          {children}
          <InstallBanner />
        </I18nProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
