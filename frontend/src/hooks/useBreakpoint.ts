'use client'

import { useState, useEffect } from 'react'

const MOBILE_QUERY = '(max-width: 767px)'
const TABLET_QUERY = '(min-width: 768px) and (max-width: 1023px)'
const DESKTOP_QUERY = '(min-width: 1024px)'

function getBreakpoint() {
  if (typeof window === 'undefined') {
    return { isMobile: false, isTablet: false, isDesktop: true }
  }
  return {
    isMobile: window.matchMedia(MOBILE_QUERY).matches,
    isTablet: window.matchMedia(TABLET_QUERY).matches,
    isDesktop: window.matchMedia(DESKTOP_QUERY).matches,
  }
}

export function useBreakpoint() {
  const [bp, setBp] = useState(getBreakpoint)

  useEffect(() => {
    const mq = [MOBILE_QUERY, TABLET_QUERY, DESKTOP_QUERY].map((q) => window.matchMedia(q))
    const handler = () => setBp(getBreakpoint())
    // SSR에서는 desktop 스냅샷을 사용하므로, 리스너 등록 시점에 현재 viewport와
    // 반드시 한 번 동기화한다. 이 호출이 없으면 모바일 최초 로드가 다음 resize까지
    // desktop 레이아웃으로 고정된다.
    handler()
    mq.forEach((m) => m.addEventListener('change', handler))
    return () => mq.forEach((m) => m.removeEventListener('change', handler))
  }, [])

  return bp
}
