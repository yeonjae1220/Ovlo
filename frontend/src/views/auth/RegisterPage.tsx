'use client'

import { safeReturnPath } from '@/utils/returnPath'
import { saveOauthState } from '@/utils/oauthState'
import { openOAuthUrl } from '@/utils/nativeOAuth'

import { useState, type FormEvent } from 'react'
import Link from 'next/link'
import { useRegister } from '../../hooks/useAuth'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useNicknameAvailability } from '../../hooks/useMember'
import { useI18n } from '../../i18n/I18nProvider'
import { useDebounce } from '../../hooks/useDebounce'
import { LANGUAGE_LABELS } from '../../i18n/messages'
import { uiLanguageOptions } from '../../config/pilotFocus'
import type { MessageKey } from '../../i18n/messages'
import { QueryErrorNotice } from '../../components/ui'
import { Brand } from '../../components/ui/Brand'
import { AppleSignInButton } from '../../components/auth/AppleSignInButton'

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || null

function redirectToGoogle(notConfiguredMsg: string) {
  if (!GOOGLE_CLIENT_ID) {
    alert(notConfiguredMsg)
    return
  }
  const state = crypto.randomUUID()
  saveOauthState(state)
  sessionStorage.setItem('ovlo-oauth-next', safeReturnPath(new URLSearchParams(window.location.search).get('next')))

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
  void openOAuthUrl(`https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`)
}

const DEGREE_VALUES = ['BACHELOR', 'MASTER', 'DOCTOR'] as const

const PW_TESTS: Array<{ key: 'register.pw.min8' | 'register.pw.upper' | 'register.pw.lower' | 'register.pw.number' | 'register.pw.special'; test: (p: string) => boolean }> = [
  { key: 'register.pw.min8',     test: (p) => p.length >= 8 },
  { key: 'register.pw.upper',    test: (p) => /[A-Z]/.test(p) },
  { key: 'register.pw.lower',    test: (p) => /[a-z]/.test(p) },
  { key: 'register.pw.number',   test: (p) => /\d/.test(p) },
  { key: 'register.pw.special',  test: (p) => /[@$!%*?&_#^()-]/.test(p) },
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
            <span style={{ color: met ? 'var(--color-success)' : 'var(--color-text-dim)', fontSize: 13, lineHeight: 1 }}>
              {met ? '✓' : '○'}
            </span>
            <span style={{ color: met ? 'var(--color-success)' : 'var(--color-text-muted)' }}>{t(key)}</span>
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
                background: done || active ? 'var(--color-accent-strong)' : 'var(--color-surface-soft)',
                color: done || active ? 'var(--color-on-accent)' : 'var(--color-text-muted)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 13, fontWeight: 700,
                transition: 'background 0.2s',
              }}>
                {done ? '✓' : num}
              </div>
              <span style={{ fontSize: 11, color: active ? 'var(--color-accent)' : 'var(--color-text-muted)', fontWeight: active ? 700 : 500, whiteSpace: 'nowrap' }}>
                {title}
              </span>
            </div>
            {num < TOTAL_STEPS && (
              <div style={{
                flex: 1, height: 2, background: done ? 'var(--color-accent-strong)' : 'var(--color-surface-soft)',
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
  border: '1px solid var(--color-border-strong)',
  borderRadius: 12,
  boxShadow: 'var(--shadow-soft)',
  fontFamily: 'system-ui, sans-serif',
  background: 'var(--color-surface)',
}

const fieldStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 6,
}

const labelStyle: React.CSSProperties = {
  fontSize: 13,
  fontWeight: 600,
  color: 'var(--color-text-secondary)',
}

const inputStyle: React.CSSProperties = {
  padding: '10px 12px',
  border: '1px solid var(--color-border-strong)',
  borderRadius: 8,
  fontSize: 14,
  outlineOffset: 2,
  transition: 'border-color 0.15s',
  background: 'var(--color-input-bg)',
  color: 'var(--color-text)',
}

const primaryBtn: React.CSSProperties = {
  padding: '11px 0',
  background: 'var(--color-accent-strong)',
  color: 'var(--color-on-accent)',
  border: 'none',
  borderRadius: 8,
  fontSize: 15,
  fontWeight: 600,
  cursor: 'pointer',
  marginTop: 4,
}

const secondaryBtn: React.CSSProperties = {
  padding: '11px 0',
  background: 'var(--color-surface-soft)',
  color: 'var(--color-text-secondary)',
  border: '1px solid var(--color-border)',
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
  const [invalidField, setInvalidField] = useState('')
  const [passwordFocused, setPasswordFocused] = useState(false)

  const register = useRegister()
  const debouncedUniQuery = useDebounce(uniQuery.trim(), 300)
  const { data: universities, isError: universityError, isFetching: universityLoading, refetch: retryUniversities } = useUniversitySearch(debouncedUniQuery, undefined, language)
  const { data: nicknameCheck } = useNicknameAvailability(step === 1 ? form.nickname : '')
  const nicknameAvailable = form.nickname.length >= 2 && nicknameCheck?.available === true
  const nicknameTaken = form.nickname.length >= 2 && nicknameCheck?.available === false

  const set = (key: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [key]: e.target.value }))
      setStepError('')
    }

  const fail = (field: string, message: string): false => {
    setInvalidField(field)
    setStepError(message)
    document.getElementById('register-' + field)?.focus()
    return false
  }

  const validateStep = (): boolean => {
    if (step === 1) {
      if (form.nickname.length < 2) { return fail('nickname', t('register.error.nickname.short')) }
      if (!/^[a-zA-Z0-9._]+$/.test(form.nickname)) { return fail('nickname', t('register.error.nickname.invalid')) }
      if (nicknameTaken) { return fail('nickname', t('register.error.nickname.taken')) }
      if (!form.email) { return fail('email', t('register.error.email')) }
      const unmet = PW_TESTS.filter(c => !c.test(form.password))
      if (unmet.length > 0) {
        return fail('password', t('register.error.password').replace('{conditions}', unmet.map(c => t(c.key)).join(', ')))
      }
    }
    if (step === 2) {
      if (!form.name.trim()) { return fail('name', t('register.error.name')) }
      if (!form.hometown.trim()) { return fail('hometown', t('register.error.hometown')) }
    }
    if (step === 3) {
      if (!selectedUniId) { return fail('university', t('register.error.university')) }
      if (!form.majorName.trim()) { return fail('majorName', t('register.error.major')) }
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
      <div style={{ marginBottom: 22 }}><Brand /></div>
      <h2 style={{ margin: '0 0 24px', fontSize: 22, fontWeight: 800, color: 'var(--color-text)' }}>{t('register.title')}</h2>

      <StepIndicator step={step} t={t} />

      <p style={{ margin: '0 0 18px', fontSize: 12, lineHeight: 1.6, color: 'var(--color-text-dim)' }}>
        {t('register.legal.prefix')}
        <Link href="/terms" style={{ color: 'var(--color-primary)' }}>{t('legal.terms')}</Link>
        {t('register.legal.between')}
        <Link href="/privacy" style={{ color: 'var(--color-primary)' }}>{t('legal.privacy')}</Link>
        {t('register.legal.suffix')}
      </p>
      <p
        role="note"
        style={{ margin: '-8px 0 18px', padding: '10px 12px', fontSize: 12, lineHeight: 1.6, color: 'var(--color-text-secondary)', border: '1px solid var(--color-border)', borderRadius: 10, background: 'var(--color-surface-soft)' }}
      >
        {t('register.legal.ugc')}
      </p>

      {/* ── Step 1: 계정 정보 ─────────────────────────────────────────── */}
      {step === 1 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label htmlFor="register-nickname" style={labelStyle}>{t('register.nickname')} <span style={{ color: 'var(--color-text-dim)', fontWeight: 500 }}>{t('register.nickname.hint')}</span></label>
            <div style={{ position: 'relative' }}>
              <input
                style={{ ...inputStyle, width: '100%', boxSizing: 'border-box', paddingRight: 96 }}
                id="register-nickname" aria-invalid={!!stepError && invalidField === "nickname"} autoComplete="username" aria-describedby={stepError ? "register-error" : undefined} value={form.nickname}
                onChange={set('nickname')}
                placeholder={t('register.nickname.placeholder')}
                required minLength={2} maxLength={30}
              />
              {(nicknameAvailable || nicknameTaken) && (
                <span style={{
                  position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                  fontSize: 12, color: nicknameAvailable ? 'var(--color-success)' : 'var(--color-danger)', fontWeight: 700,
                }}>
                  {nicknameAvailable ? t('register.nickname.available') : t('register.nickname.taken')}
                </span>
              )}
            </div>
          </div>

          <div style={fieldStyle}>
            <label htmlFor="register-email" style={labelStyle}>{t('register.email')}</label>
            <input style={inputStyle} type="email" id="register-email" aria-invalid={!!stepError && invalidField === "email"} autoComplete="email" aria-describedby={stepError ? "register-error" : undefined} value={form.email} onChange={set('email')} placeholder="example@email.com" required />
          </div>

          <div style={fieldStyle}>
            <label htmlFor="register-password" style={labelStyle}>{t('register.password')}</label>
            <input
              style={inputStyle}
              type="password"
              id="register-password" aria-invalid={!!stepError && invalidField === "password"} autoComplete="new-password" aria-describedby={stepError ? "register-error" : undefined} value={form.password}
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

          {stepError && <p id="register-error" role="alert" style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>{stepError}</p>}

          <button type="submit" style={primaryBtn}>{t('register.next')}</button>
        </form>
      )}

      {/* ── Step 2 ─────────────────────────────────────────── */}
      {step === 2 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label htmlFor="register-name" style={labelStyle}>{t('register.name')}</label>
            <input style={inputStyle} id="register-name" aria-invalid={!!stepError && invalidField === "name"} autoComplete="name" aria-describedby={stepError ? "register-error" : undefined} value={form.name} onChange={set('name')} placeholder={t('register.name')} required />
          </div>

          <div style={fieldStyle}>
            <label htmlFor="register-hometown" style={labelStyle}>{t('onboarding.hometown')}</label>
            <input style={inputStyle} id="register-hometown" aria-invalid={!!stepError && invalidField === "hometown"} autoComplete="address-level2" aria-describedby={stepError ? "register-error" : undefined} value={form.hometown} onChange={set('hometown')} placeholder={t('onboarding.hometown.placeholder')} required />
          </div>

          {stepError && <p id="register-error" role="alert" style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>{stepError}</p>}

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
            <label htmlFor="register-university" style={labelStyle}>{t('onboarding.university')}</label>
            {selectedUniId ? (
              <button id="register-university"
                type="button"
                onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}
                style={{
                  ...inputStyle,
                  background: 'var(--color-success-soft)',
                  color: 'var(--color-success)',
                  fontWeight: 500,
                  cursor: 'pointer',
                  textAlign: 'left',
                  border: '1px solid color-mix(in srgb, var(--color-success) 48%, var(--color-border-strong))',
                }}
              >
                ✓ {selectedUniName} <span style={{ color: 'var(--color-text-dim)', fontSize: 12, fontWeight: 500 }}>{t('onboarding.university.change')}</span>
              </button>
            ) : (
              <>
                <input
                  style={inputStyle}
                  placeholder={t('onboarding.university.placeholder')}
                  id="register-university" value={uniQuery}
                  onChange={(e) => { setUniQuery(e.target.value); setStepError('') }}
                />
                {universityError && <QueryErrorNotice onRetry={() => void retryUniversities()} />}
                {universityLoading && <p role="status">{t('common.loading')}</p>}
                {!universityError && uniQuery.length >= 1 && universities && universities.length > 0 && (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0, border: '1px solid var(--color-border-strong)', borderRadius: 8, maxHeight: 180, overflowY: 'auto', boxShadow: 'var(--shadow-soft)', background: 'var(--color-surface)' }}>
                    {universities.slice(0, 8).map((u) => (
                      <li key={u.id}><button type="button"
                        onClick={() => { setSelectedUniId(Number(u.id)); setSelectedUniName(u.name); setUniQuery('') }}
                        style={{ width: '100%', background: 'none', border: 0, textAlign: 'left', padding: '9px 12px', cursor: 'pointer', borderBottom: '1px solid var(--color-border)', fontSize: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text)' }}>
                        <span style={{ minWidth: 0 }}>
                          {u.name}
                          {u.localName && u.localName !== u.name && (
                            <span style={{ display: 'block', color: 'var(--color-text-muted)', fontSize: 12 }}>{u.localName}</span>
                          )}
                        </span>
                        <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                      </button></li>
                    ))}
                  </ul>
                )}
                {!universityError && !universityLoading && uniQuery.length >= 1 && universities?.length === 0 && (
                  <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{t('onboarding.university.notFound')}</p>
                )}
              </>
            )}
          </div>

          <div style={fieldStyle}>
            <label htmlFor="register-majorName" style={labelStyle}>{t('onboarding.major')}</label>
            <input style={inputStyle} id="register-majorName" aria-invalid={!!stepError && invalidField === "majorName"} autoComplete="off" aria-describedby={stepError ? "register-error" : undefined} value={form.majorName} onChange={set('majorName')} placeholder={t('onboarding.major.placeholder')} required />
          </div>

          <div style={fieldStyle}>
            <label htmlFor="register-language" style={labelStyle}>{t('register.uiLanguage')}</label>
            <select
              style={inputStyle}
              id="register-language" value={language}
              onChange={(e) => setLanguage(e.target.value)}
            >
              {uiLanguageOptions(language).map((lang) => (
                <option key={lang} value={lang}>{LANGUAGE_LABELS[lang]}</option>
              ))}
            </select>
            <span style={{ fontSize: 11, color: 'var(--color-text-dim)', marginTop: 2 }}>
              {t('onboarding.uiLanguage.hint')}
            </span>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label htmlFor="register-degreeType" style={labelStyle}>{t('onboarding.degree')}</label>
              <select style={inputStyle} id="register-degreeType" aria-invalid={!!stepError && invalidField === "degreeType"} autoComplete="off" aria-describedby={stepError ? "register-error" : undefined} value={form.degreeType} onChange={set('degreeType')}>
                {DEGREE_VALUES.map((v) => (
                  <option key={v} value={v}>{t(`register.degree.${v.toLowerCase()}` as 'register.degree.bachelor')}</option>
                ))}
              </select>
            </div>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label htmlFor="register-gradeLevel" style={labelStyle}>{t('onboarding.grade')}</label>
              <select style={inputStyle} id="register-gradeLevel" aria-invalid={!!stepError && invalidField === "gradeLevel"} autoComplete="off" aria-describedby={stepError ? "register-error" : undefined} value={form.gradeLevel} onChange={set('gradeLevel')}>
                {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={String(n)}>{n}</option>)}
              </select>
            </div>
          </div>

          {stepError && <p id="register-error" role="alert" style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>{stepError}</p>}
          {register.isError && (
            <p style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>
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
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--color-border)' }} />
        <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{t('common.or')}</span>
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--color-border)' }} />
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
          border: '1px solid var(--color-border-strong)',
          borderRadius: 8,
          background: 'var(--color-surface)',
          fontSize: 14,
          fontWeight: 500,
          color: 'var(--color-text-secondary)',
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
      <AppleSignInButton labelKey="register.apple.btn" />

      <p style={{ marginTop: 16, textAlign: 'center', fontSize: 13, color: 'var(--color-text-muted)' }}>
        {t('register.hasAccount')} <Link href="/login" style={{ color: 'var(--color-accent)', fontWeight: 700 }}>{t('register.login')}</Link>
      </p>
    </div>
  )
}
