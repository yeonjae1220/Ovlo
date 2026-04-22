import { useState, useEffect } from 'react'

const MOBILE_QUERY = '(max-width: 767px)'
const TABLET_QUERY = '(min-width: 768px) and (max-width: 1023px)'
const DESKTOP_QUERY = '(min-width: 1024px)'

function getBreakpoint() {
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
    mq.forEach((m) => m.addEventListener('change', handler))
    return () => mq.forEach((m) => m.removeEventListener('change', handler))
  }, [])

  return bp
}
