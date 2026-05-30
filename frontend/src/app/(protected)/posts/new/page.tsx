'use client'

import { Suspense } from 'react'
import CreatePostPage from '@/views/post/CreatePostPage'

export default function Page() {
  return (
    <Suspense fallback={null}>
      <CreatePostPage />
    </Suspense>
  )
}
