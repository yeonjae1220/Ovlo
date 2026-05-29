import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useGoogleLogin } from '../../hooks/useAuth'
import { useI18n } from '../../i18n/I18nProvider'

export default function OAuthCallbackPage() {
  const { t } = useI18n()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const googleLogin = useGoogleLogin()

  useEffect(() => {
    const code = searchParams.get('code')
    const error = searchParams.get('error')

    if (error || !code) {
      navigate('/login', { replace: true })
      return
    }

    const sessionKey = `oauth:${code}`
    if (sessionStorage.getItem(sessionKey)) return
    sessionStorage.setItem(sessionKey, '1')

    const redirectUri = `${window.location.origin}/oauth/callback`
    googleLogin.mutate({ code, redirectUri }, {
      onError: () => sessionStorage.removeItem(sessionKey),
    })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (googleLogin.isError) {
    const msg = (googleLogin.error as { response?: { data?: { message?: string } } })
      ?.response?.data?.message
    return (
      <div style={{ maxWidth: 400, margin: '100px auto', padding: 24, textAlign: 'center' }}>
        <p style={{ color: '#dc2626', marginBottom: 20 }}>
          {msg ?? t('oauth.error.default')}
        </p>
        <button
          onClick={() => navigate('/login', { replace: true })}
          style={{
            padding: '10px 20px',
            background: '#2563eb',
            color: '#fff',
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
        width: 36, height: 36, border: '3px solid #e5e7eb',
        borderTopColor: '#2563eb', borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <p style={{ color: '#6b7280', fontSize: 14 }}>{t('oauth.loading')}</p>
    </div>
  )
}
