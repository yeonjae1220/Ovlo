'use client'

import { saveOauthState } from '@/utils/oauthState'

import { useState, type FormEvent } from 'react'
import Link from 'next/link'
import { useLogin } from '../../hooks/useAuth'
import { useI18n } from '../../i18n/I18nProvider'
import { Button, Card, FieldGroup, TextField } from '../../components/ui'

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || null
const C = {
  border: 'var(--color-border)',
  borderStrong: 'var(--color-border-strong)',
  surface: 'var(--color-surface)',
  accentStrong: 'var(--color-accent-strong)',
  onAccent: 'var(--color-on-accent)',
  text: 'var(--color-text)',
  textSecondary: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  danger: 'var(--color-danger)',
}

function redirectToGoogle(notConfiguredMsg: string) {
  if (!GOOGLE_CLIENT_ID) {
    alert(notConfiguredMsg)
    return
  }
  // state 파라미터로 login CSRF 방지
  const state = crypto.randomUUID()
  saveOauthState(state)

  const redirectUri = `${window.location.origin}/oauth/callback`
  const params = new URLSearchParams({
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid email profile',
    access_type: 'offline',
    prompt: 'select_account',
    state,
  })
  window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
}

export default function LoginPage() {
  const { t } = useI18n()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const login = useLogin()

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)
    login.mutate(
      { email, password },
      { onError: () => setErrorMsg(t('login.error')) },
    )
  }

  return (
    <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: '48px 16px' }}>
      <Card
        style={{
          width: '100%',
          maxWidth: 430,
          padding: '34px 30px 28px',
          borderColor: C.borderStrong,
          boxShadow: 'var(--shadow-soft)',
        }}
      >
        <Link href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: 9, marginBottom: 24, color: C.text, textDecoration: 'none', fontSize: 20, fontWeight: 900 }}>
          <span style={{ width: 30, height: 30, borderRadius: 8, background: C.accentStrong, color: C.onAccent, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>O</span>
          Ovlo
        </Link>

        <h1 style={{ margin: '0 0 22px', color: C.text, fontSize: 28, lineHeight: 1.18, fontWeight: 900 }}>{t('login.title')}</h1>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <FieldGroup label={t('login.email')}>
            <TextField
              type="email"
              placeholder={t('login.email')}
              value={email}
              onChange={(e) => { setEmail(e.target.value); setErrorMsg(null) }}
              required
            />
          </FieldGroup>
          <FieldGroup label={t('login.password')}>
            <TextField
              type="password"
              placeholder={t('login.password')}
              value={password}
              onChange={(e) => { setPassword(e.target.value); setErrorMsg(null) }}
              required
            />
          </FieldGroup>
          {errorMsg && <p style={{ color: C.danger, margin: 0, fontSize: 13, fontWeight: 700 }}>{errorMsg}</p>}
          <Button type="submit" variant="primary" disabled={login.isPending} style={{ width: '100%' }}>
            {login.isPending ? t('login.pending') : t('login.submit')}
          </Button>
        </form>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '16px 0' }}>
          <hr style={{ flex: 1, border: 'none', borderTop: `1px solid ${C.border}` }} />
          <span style={{ fontSize: 12, color: C.muted }}>{t('common.or')}</span>
          <hr style={{ flex: 1, border: 'none', borderTop: `1px solid ${C.border}` }} />
        </div>

        <button
          type="button"
          onClick={() => redirectToGoogle(t('login.google.notConfigured'))}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 10,
            minHeight: 42,
            padding: '10px 0',
            border: `1px solid ${C.borderStrong}`,
            borderRadius: 8,
            background: C.surface,
            fontSize: 14,
            fontWeight: 500,
            color: C.textSecondary,
            cursor: 'pointer',
          }}
        >
          <svg width="18" height="18" viewBox="0 0 48 48">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.18 1.48-4.97 2.36-8.16 2.36-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
            <path fill="none" d="M0 0h48v48H0z"/>
          </svg>
          {t('login.google.btn')}
        </button>

        <p style={{ color: C.muted }}>
          {t('login.noAccount')} <Link href="/register">{t('login.register')}</Link>
        </p>
      </Card>
    </main>
  )
}
