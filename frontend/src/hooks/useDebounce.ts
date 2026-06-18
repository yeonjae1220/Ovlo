'use client'

import { useEffect, useState } from 'react'

/** 입력값을 delay(ms) 만큼 지연시켜 반환 — 검색어 타이핑 중 과도한 요청을 막는다. */
export function useDebounce<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState<T>(value)

  useEffect(() => {
    const handler = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(handler)
  }, [value, delay])

  return debounced
}
