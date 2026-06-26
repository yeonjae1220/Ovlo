'use client'

import { useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { useSearchParams } from 'next/navigation'
import { useGoogleLogin } from '../../hooks/useAuth'
import { useI18n } from '../../i18n/I18nProvider'
import { consumeOauthState } from '../../utils/oauthState'

export default function OAuthCallbackPage() {
  const { t } = useI18n()
  const searchParams = useSearchParams()
  const router = useRouter()
  const googleLogin = useGoogleLogin()
  const calledRef = useRef(false)

  useEffect(() => {
    // OAuth authorization code는 1회만 사용 가능 — Strict Mode 이중 실행 및 searchParams 변경 시 재실행 방지
    if (calledRef.current) return
    calledRef.current = true

    const code = searchParams?.get('code')
    const error = searchParams?.get('error')
    const returnedState = searchParams?.get('state')
    const savedState = consumeOauthState()
    

    if (error || !code) {
      router.replace('/login')
      return
    }

    // state 불일치 시 login CSRF 공격 차단
    if (!returnedState || returnedState !== savedState) {
      router.replace('/login')
      return
    }

    const redirectUri = `${window.location.origin}/oauth/callback`
    googleLogin.mutate({ code, redirectUri })
  }, [searchParams, router, googleLogin])

  if (googleLogin.isError) {
    const msg = (googleLogin.error as { response?: { data?: { message?: string } } })
      ?.response?.data?.message
    return (
      <div style={{ maxWidth: 400, margin: '100px auto', padding: 24, textAlign: 'center' }}>
        <p style={{ color: 'var(--color-danger)', marginBottom: 20, fontWeight: 700 }}>
          {msg ?? t('oauth.error.default')}
        </p>
        <button
          onClick={() => router.replace('/login')}
          style={{
            padding: '10px 20px',
            background: 'var(--color-accent-strong)',
            color: 'var(--color-on-accent)',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontSize: 14,
          }}
        >
          {t('oauth.goBack')}
        </button>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: 16 }}>
      <div style={{
        width: 36, height: 36, border: '3px solid var(--color-surface-soft)',
        borderTopColor: 'var(--color-accent)', borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <p style={{ color: 'var(--color-text-muted)', fontSize: 14 }}>{t('oauth.loading')}</p>
    </div>
  )
}
