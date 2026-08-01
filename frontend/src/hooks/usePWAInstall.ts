'use client'

import { useEffect, useState } from 'react'
import { isNativeApp } from '../utils/platform'

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export type Platform = 'android' | 'ios' | 'other'

function detectPlatform(): Platform {
  if (typeof navigator === 'undefined') return 'other'
  const ua = navigator.userAgent
  if (/android/i.test(ua)) return 'android'
  if (/iphone|ipad|ipod/i.test(ua)) return 'ios'
  return 'other'
}

export function usePWAInstall() {
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [isInstalled, setIsInstalled] = useState(false)
  const [platform] = useState<Platform>(() => detectPlatform())

  useEffect(() => {
    // 네이티브 셸 안에서는 이미 설치된 앱이므로 "앱 설치" 안내가 나오면 안 된다.
    // 서비스워커도 등록하지 않는다(WKWebView 지원이 제한적이고 캐시 용도라 이득이 없다).
    if (isNativeApp()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsInstalled(true)
      return
    }

    if (window.matchMedia('(display-mode: standalone)').matches) {
      // 외부 시스템(display-mode 미디어쿼리)과의 동기화 — 의도된 mount-time setState
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsInstalled(true)
      return
    }

    const handler = (e: Event) => {
      e.preventDefault()
      setInstallPrompt(e as BeforeInstallPromptEvent)
    }

    window.addEventListener('beforeinstallprompt', handler)
    window.addEventListener('appinstalled', () => setIsInstalled(true))

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(() => {})
    }

    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  const triggerInstall = async () => {
    if (!installPrompt) return
    await installPrompt.prompt()
    const { outcome } = await installPrompt.userChoice
    if (outcome === 'accepted') setIsInstalled(true)
    setInstallPrompt(null)
  }

  return { installPrompt, isInstalled, platform, triggerInstall }
}
