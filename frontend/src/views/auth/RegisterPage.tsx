'use client'

import { useState, type FormEvent } from 'react'
import Link from 'next/link'
import { useRegister } from '../../hooks/useAuth'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useMemberSearch } from '../../hooks/useMember'
import { useI18n } from '../../i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES, LANGUAGE_LABELS } from '../../i18n/messages'
import type { MessageKey } from '../../i18n/messages'

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || null

function redirectToGoogle(notConfiguredMsg: string) {
  if (!GOOGLE_CLIENT_ID) {
    alert(notConfiguredMsg)
    return
  }
  const state = crypto.randomUUID()
  sessionStorage.setItem('oauth_state', state)

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

const DEGREE_VALUES = ['BACHELOR', 'MASTER', 'DOCTOR'] as const

const PW_TESTS: Array<{ key: 'register.pw.min8' | 'register.pw.upper' | 'register.pw.lower' | 'register.pw.number' | 'register.pw.special'; test: (p: string) => boolean }> = [
  { key: 'register.pw.min8',     test: (p) => p.length >= 8 },
  { key: 'register.pw.upper',    test: (p) => /[A-Z]/.test(p) },
  { key: 'register.pw.lower',    test: (p) => /[a-z]/.test(p) },
  { key: 'register.pw.number',   test: (p) => /\d/.test(p) },
  { key: 'register.pw.special',  test: (p) => /[@$!%*?&_#^()\-]/.test(p) },
]

const TOTAL_STEPS = 3

type TFn = (key: MessageKey, vars?: Record<string, string | number>) => string

function PasswordConditions({ password, t }: { password: string; t: TFn }) {
  return (
    <ul style={{ listStyle: 'none', padding: '6px 0 0', margin: 0, display: 'flex', flexDirection: 'column', gap: 4 }}>
      {PW_TESTS.map(({ key, test }) => {
        const met = test(password)
        return (
          <li key={key} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
            <span style={{ color: met ? '#16a34a' : '#9ca3af', fontSize: 13, lineHeight: 1 }}>
              {met ? '✓' : '○'}
            </span>
            <span style={{ color: met ? '#16a34a' : '#6b7280' }}>{t(key)}</span>
          </li>
        )
      })}
    </ul>
  )
}

// ── Step indicator (外部定義: 내부 정의 시 매 렌더마다 unmount/remount 발생) ──
function StepIndicator({ step, t }: { step: number; t: TFn }) {
  const stepTitles = [t('register.step.account'), t('register.step.personal'), t('register.step.academic')]
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: 28, gap: 0 }}>
      {stepTitles.map((title, idx) => {
        const num = idx + 1
        const active = num === step
        const done = num < step
        return (
          <div key={num} style={{ display: 'flex', alignItems: 'center', flex: num < TOTAL_STEPS ? 1 : 'none' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
              <div style={{
                width: 28, height: 28, borderRadius: '50%',
                background: done ? '#2563eb' : active ? '#2563eb' : '#e5e7eb',
                color: done || active ? '#fff' : '#9ca3af',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 13, fontWeight: 700,
                transition: 'background 0.2s',
              }}>
                {done ? '✓' : num}
              </div>
              <span style={{ fontSize: 11, color: active ? '#2563eb' : '#9ca3af', fontWeight: active ? 600 : 400, whiteSpace: 'nowrap' }}>
                {title}
              </span>
            </div>
            {num < TOTAL_STEPS && (
              <div style={{
                flex: 1, height: 2, background: done ? '#2563eb' : '#e5e7eb',
                margin: '0 6px', marginBottom: 18,
                transition: 'background 0.2s',
              }} />
            )}
          </div>
        )
      })}
    </div>
  )
}

// ── Styles (outside component to avoid recreation on every render) ─────────
const containerStyle: React.CSSProperties = {
  maxWidth: 460,
  margin: '60px auto',
  padding: '32px 28px',
  border: '1px solid #e5e7eb',
  borderRadius: 12,
  boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
  fontFamily: 'system-ui, sans-serif',
}

const fieldStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 6,
}

const labelStyle: React.CSSProperties = {
  fontSize: 13,
  fontWeight: 600,
  color: '#374151',
}

const inputStyle: React.CSSProperties = {
  padding: '10px 12px',
  border: '1px solid #d1d5db',
  borderRadius: 8,
  fontSize: 14,
  outline: 'none',
  transition: 'border-color 0.15s',
}

const primaryBtn: React.CSSProperties = {
  padding: '11px 0',
  background: '#2563eb',
  color: '#fff',
  border: 'none',
  borderRadius: 8,
  fontSize: 15,
  fontWeight: 600,
  cursor: 'pointer',
  marginTop: 4,
}

const secondaryBtn: React.CSSProperties = {
  padding: '11px 0',
  background: '#f3f4f6',
  color: '#374151',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  fontSize: 14,
  cursor: 'pointer',
}

type FormData = {
  nickname: string
  email: string
  password: string
  name: string
  hometown: string
  majorName: string
  degreeType: string
  gradeLevel: string
}

export default function RegisterPage() {
  const { language, setLanguage, t } = useI18n()
  const [step, setStep] = useState(1)
  const [form, setForm] = useState<FormData>({
    nickname: '',
    email: '',
    password: '',
    name: '',
    hometown: '',
    majorName: '',
    degreeType: 'BACHELOR',
    gradeLevel: '1',
  })
  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniId, setSelectedUniId] = useState<number | null>(null)
  const [selectedUniName, setSelectedUniName] = useState('')
  const [stepError, setStepError] = useState('')
  const [passwordFocused, setPasswordFocused] = useState(false)

  const register = useRegister()
  const { data: universities } = useUniversitySearch(uniQuery)
  const { data: nicknameResults } = useMemberSearch(step === 1 ? form.nickname : '')
  const nicknameAvailable = form.nickname.length >= 2 && nicknameResults?.length === 0
  const nicknameTaken = form.nickname.length >= 2 && (nicknameResults?.length ?? 0) > 0

  const set = (key: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [key]: e.target.value }))
      setStepError('')
    }

  const validateStep = (): boolean => {
    if (step === 1) {
      if (form.nickname.length < 2) { setStepError(t('register.error.nickname.short')); return false }
      if (!/^[a-zA-Z0-9._]+$/.test(form.nickname)) { setStepError(t('register.error.nickname.invalid')); return false }
      if (nicknameTaken) { setStepError(t('register.error.nickname.taken')); return false }
      if (!form.email) { setStepError(t('register.error.email')); return false }
      const unmet = PW_TESTS.filter(c => !c.test(form.password))
      if (unmet.length > 0) {
        setStepError(t('register.error.password').replace('{conditions}', unmet.map(c => t(c.key)).join(', ')))
        return false
      }
    }
    if (step === 2) {
      if (!form.name.trim()) { setStepError(t('register.error.name')); return false }
      if (!form.hometown.trim()) { setStepError(t('register.error.hometown')); return false }
    }
    if (step === 3) {
      if (!selectedUniId) { setStepError(t('register.error.university')); return false }
      if (!form.majorName.trim()) { setStepError(t('register.error.major')); return false }
    }
    return true
  }

  const handleNext = (e: FormEvent) => {
    e.preventDefault()
    if (!validateStep()) return
    setStepError('')
    setStep((s) => s + 1)
  }

  const handleBack = () => {
    setStepError('')
    setStep((s) => s - 1)
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!validateStep()) return
    register.mutate({
      nickname: form.nickname,
      name: form.name,
      hometown: form.hometown,
      email: form.email,
      password: form.password,
      homeUniversityId: selectedUniId!,
      majorName: form.majorName,
      degreeType: form.degreeType,
      gradeLevel: Number(form.gradeLevel),
    })
  }

  return (
    <div style={containerStyle}>
      <h2 style={{ margin: '0 0 24px', fontSize: 22, fontWeight: 700, color: '#111827' }}>{t('register.title')}</h2>

      <StepIndicator step={step} t={t} />

      {/* ── Step 1: 계정 정보 ─────────────────────────────────────────── */}
      {step === 1 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>{t('register.nickname')} <span style={{ color: '#9ca3af', fontWeight: 400 }}>{t('register.nickname.hint')}</span></label>
            <div style={{ position: 'relative' }}>
              <input
                style={{ ...inputStyle, width: '100%', boxSizing: 'border-box', paddingRight: 96 }}
                value={form.nickname}
                onChange={set('nickname')}
                placeholder={t('register.nickname.placeholder')}
                required minLength={2} maxLength={30}
              />
              {form.nickname.length >= 2 && (
                <span style={{
                  position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                  fontSize: 12, color: nicknameAvailable ? '#16a34a' : '#dc2626', fontWeight: 600,
                }}>
                  {nicknameAvailable ? t('register.nickname.available') : t('register.nickname.taken')}
                </span>
              )}
            </div>
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>{t('register.email')}</label>
            <input style={inputStyle} type="email" value={form.email} onChange={set('email')} placeholder="example@email.com" required />
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>{t('register.password')}</label>
            <input
              style={inputStyle}
              type="password"
              value={form.password}
              onChange={set('password')}
              onFocus={() => setPasswordFocused(true)}
              placeholder="••••••••"
              minLength={8}
              required
            />
            {(passwordFocused || form.password.length > 0) && (
              <PasswordConditions password={form.password} t={t} />
            )}
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}

          <button type="submit" style={primaryBtn}>{t('register.next')}</button>
        </form>
      )}

      {/* ── Step 2 ─────────────────────────────────────────── */}
      {step === 2 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>{t('register.name')}</label>
            <input style={inputStyle} value={form.name} onChange={set('name')} placeholder={t('register.name')} required />
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>{t('onboarding.hometown')}</label>
            <input style={inputStyle} value={form.hometown} onChange={set('hometown')} placeholder={t('onboarding.hometown.placeholder')} required />
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" onClick={handleBack} style={{ ...secondaryBtn, flex: 1 }}>{t('register.back')}</button>
            <button type="submit" style={{ ...primaryBtn, flex: 2 }}>{t('register.next')}</button>
          </div>
        </form>
      )}

      {/* ── Step 3: 학업 정보 ─────────────────────────────────────────── */}
      {step === 3 && (
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>{t('onboarding.university')}</label>
            {selectedUniId ? (
              <button
                type="button"
                onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}
                style={{
                  ...inputStyle,
                  background: '#f0fdf4',
                  color: '#16a34a',
                  fontWeight: 500,
                  cursor: 'pointer',
                  textAlign: 'left',
                  border: '1px solid #86efac',
                }}
              >
                ✓ {selectedUniName} <span style={{ color: '#9ca3af', fontSize: 12, fontWeight: 400 }}>{t('onboarding.university.change')}</span>
              </button>
            ) : (
              <>
                <input
                  style={inputStyle}
                  placeholder={t('onboarding.university.placeholder')}
                  value={uniQuery}
                  onChange={(e) => { setUniQuery(e.target.value); setStepError('') }}
                />
                {uniQuery.length >= 1 && universities && universities.length > 0 && (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0, border: '1px solid #e5e7eb', borderRadius: 8, maxHeight: 180, overflowY: 'auto', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
                    {universities.slice(0, 8).map((u) => (
                      <li key={u.id}
                        onClick={() => { setSelectedUniId(Number(u.id)); setSelectedUniName(u.name); setUniQuery('') }}
                        style={{ padding: '9px 12px', cursor: 'pointer', borderBottom: '1px solid #f3f4f6', fontSize: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>{u.name}</span>
                        <span style={{ color: '#9ca3af', fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                      </li>
                    ))}
                  </ul>
                )}
                {uniQuery.length >= 1 && universities?.length === 0 && (
                  <p style={{ margin: 0, fontSize: 13, color: '#9ca3af' }}>{t('onboarding.university.notFound')}</p>
                )}
              </>
            )}
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>{t('onboarding.major')}</label>
            <input style={inputStyle} value={form.majorName} onChange={set('majorName')} placeholder={t('onboarding.major.placeholder')} required />
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>{t('register.uiLanguage')}</label>
            <select
              style={{ ...inputStyle, background: '#f9fafb', color: '#374151' }}
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
            >
              {SUPPORTED_UI_LANGUAGES.map((lang) => (
                <option key={lang} value={lang}>{LANGUAGE_LABELS[lang]}</option>
              ))}
            </select>
            <span style={{ fontSize: 11, color: '#9ca3af', marginTop: 2 }}>
              {t('onboarding.uiLanguage.hint')}
            </span>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label style={labelStyle}>{t('onboarding.degree')}</label>
              <select style={{ ...inputStyle, background: '#f9fafb', color: '#374151' }} value={form.degreeType} onChange={set('degreeType')}>
                {DEGREE_VALUES.map((v) => (
                  <option key={v} value={v}>{t(`register.degree.${v.toLowerCase()}` as 'register.degree.bachelor')}</option>
                ))}
              </select>
            </div>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label style={labelStyle}>{t('onboarding.grade')}</label>
              <select style={{ ...inputStyle, background: '#f9fafb', color: '#374151' }} value={form.gradeLevel} onChange={set('gradeLevel')}>
                {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={String(n)}>{n}</option>)}
              </select>
            </div>
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}
          {register.isError && (
            <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>
              {(register.error as { response?: { data?: { message?: string } } })?.response?.data?.message
                || t('register.error.generic')}
            </p>
          )}

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" onClick={handleBack} style={{ ...secondaryBtn, flex: 1 }}>{t('register.back')}</button>
            <button type="submit" disabled={register.isPending} style={{ ...primaryBtn, flex: 2, opacity: register.isPending ? 0.7 : 1 }}>
              {register.isPending ? t('register.submitting') : t('register.submit')}
            </button>
          </div>
        </form>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '20px 0 12px' }}>
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid #e5e7eb' }} />
        <span style={{ fontSize: 12, color: '#9ca3af' }}>{t('common.or')}</span>
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid #e5e7eb' }} />
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
          padding: '10px 0',
          border: '1px solid #d1d5db',
          borderRadius: 8,
          background: '#fff',
          fontSize: 14,
          fontWeight: 500,
          color: '#374151',
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
        {t('register.google.btn')}
      </button>

      <p style={{ marginTop: 16, textAlign: 'center', fontSize: 13, color: '#6b7280' }}>
        {t('register.hasAccount')} <Link href="/login" style={{ color: '#2563eb', fontWeight: 600 }}>{t('register.login')}</Link>
      </p>
    </div>
  )
}
