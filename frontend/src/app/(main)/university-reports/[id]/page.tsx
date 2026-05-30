'use client'

import { Suspense } from 'react'
import UniversityReportDetailPage from '@/views/university/UniversityReportDetailPage'

export default function Page() {
  return (
    <Suspense fallback={null}>
      <UniversityReportDetailPage />
    </Suspense>
  )
}
