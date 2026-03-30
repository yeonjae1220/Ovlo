import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useRegister } from '../../hooks/useAuth'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useMemberSearch } from '../../hooks/useMember'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || null

function redirectToGoogle() {
  if (!GOOGLE_CLIENT_ID) {
    alert('Google 로그인이 현재 설정되지 않았습니다. 관리자에게 문의해주세요.')
    return
  }
  const redirectUri = `${window.location.origin}/oauth/callback`
  const params = new URLSearchParams({
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid email profile',
    access_type: 'offline',
    prompt: 'select_account',
  })
  window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`
}

const DEGREE_TYPES = [
  { value: 'BACHELOR', label: '학사' },
  { value: 'MASTER', label: '석사' },
  { value: 'DOCTOR', label: '박사' },
]

const TOTAL_STEPS = 3
const stepTitles = ['계정 정보', '개인 정보', '학업 정보']

// ── Step indicator (외부 정의: 내부 정의 시 매 렌더마다 unmount/remount 발생) ──
function StepIndicator({ step }: { step: number }) {
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

  const register = useRegister()
  const { data: universities } = useUniversitySearch(uniQuery)
  const { data: nicknameResults } = useMemberSearch(form.nickname)
  const nicknameAvailable = form.nickname.length >= 2 && nicknameResults?.length === 0
  const nicknameTaken = form.nickname.length >= 2 && (nicknameResults?.length ?? 0) > 0

  const set = (key: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [key]: e.target.value }))
      setStepError('')
    }

  const validateStep = (): boolean => {
    if (step === 1) {
      if (form.nickname.length < 2) { setStepError('닉네임은 2자 이상이어야 합니다.'); return false }
      if (!/^[a-zA-Z0-9._]+$/.test(form.nickname)) { setStepError('닉네임은 영문, 숫자, ., _ 만 사용 가능합니다.'); return false }
      if (nicknameTaken) { setStepError('이미 사용 중인 닉네임입니다.'); return false }
      if (!form.email) { setStepError('이메일을 입력해주세요.'); return false }
      if (form.password.length < 8) { setStepError('비밀번호는 8자 이상이어야 합니다.'); return false }
    }
    if (step === 2) {
      if (!form.name.trim()) { setStepError('이름을 입력해주세요.'); return false }
      if (!form.hometown.trim()) { setStepError('고향을 입력해주세요.'); return false }
    }
    if (step === 3) {
      if (!selectedUniId) { setStepError('소속 대학을 검색하여 선택해주세요.'); return false }
      if (!form.majorName.trim()) { setStepError('전공을 입력해주세요.'); return false }
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
      <h2 style={{ margin: '0 0 24px', fontSize: 22, fontWeight: 700, color: '#111827' }}>회원가입</h2>

      <StepIndicator step={step} />

      {/* ── Step 1: 계정 정보 ─────────────────────────────────────────── */}
      {step === 1 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>닉네임 <span style={{ color: '#9ca3af', fontWeight: 400 }}>(영문·숫자·.·_ / 2~30자)</span></label>
            <div style={{ position: 'relative' }}>
              <input
                style={{ ...inputStyle, width: '100%', boxSizing: 'border-box', paddingRight: 96 }}
                value={form.nickname}
                onChange={set('nickname')}
                placeholder="예: john.doe_95"
                required minLength={2} maxLength={30}
              />
              {form.nickname.length >= 2 && (
                <span style={{
                  position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                  fontSize: 12, color: nicknameAvailable ? '#16a34a' : '#dc2626', fontWeight: 600,
                }}>
                  {nicknameAvailable ? '✓ 사용 가능' : '✗ 중복'}
                </span>
              )}
            </div>
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>이메일</label>
            <input style={inputStyle} type="email" value={form.email} onChange={set('email')} placeholder="example@email.com" required />
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>비밀번호 <span style={{ color: '#9ca3af', fontWeight: 400 }}>(8자 이상, 대·소문자·숫자·특수문자 포함)</span></label>
            <input style={inputStyle} type="password" value={form.password} onChange={set('password')} placeholder="••••••••" minLength={8} required />
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}

          <button type="submit" style={primaryBtn}>다음 →</button>
        </form>
      )}

      {/* ── Step 2: 개인 정보 ─────────────────────────────────────────── */}
      {step === 2 && (
        <form onSubmit={handleNext} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>이름</label>
            <input style={inputStyle} value={form.name} onChange={set('name')} placeholder="홍길동" required />
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>고향 (도시, 국가)</label>
            <input style={inputStyle} value={form.hometown} onChange={set('hometown')} placeholder="예: Seoul, Korea" required />
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" onClick={handleBack} style={{ ...secondaryBtn, flex: 1 }}>← 이전</button>
            <button type="submit" style={{ ...primaryBtn, flex: 2 }}>다음 →</button>
          </div>
        </form>
      )}

      {/* ── Step 3: 학업 정보 ─────────────────────────────────────────── */}
      {step === 3 && (
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>소속 대학</label>
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
                title="클릭하여 대학 변경"
              >
                ✓ {selectedUniName} <span style={{ color: '#9ca3af', fontSize: 12, fontWeight: 400 }}>(클릭하여 변경)</span>
              </button>
            ) : (
              <>
                <input
                  style={inputStyle}
                  placeholder="대학명 검색..."
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
                  <p style={{ margin: 0, fontSize: 13, color: '#9ca3af' }}>검색 결과 없음</p>
                )}
              </>
            )}
          </div>

          <div style={fieldStyle}>
            <label style={labelStyle}>전공</label>
            <input style={inputStyle} value={form.majorName} onChange={set('majorName')} placeholder="예: Computer Science" required />
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label style={labelStyle}>학위</label>
              <select style={{ ...inputStyle, background: '#fff' }} value={form.degreeType} onChange={set('degreeType')}>
                {DEGREE_TYPES.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
              </select>
            </div>
            <div style={{ ...fieldStyle, flex: 1 }}>
              <label style={labelStyle}>학년</label>
              <select style={{ ...inputStyle, background: '#fff' }} value={form.gradeLevel} onChange={set('gradeLevel')}>
                {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={String(n)}>{n}학년</option>)}
              </select>
            </div>
          </div>

          {stepError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{stepError}</p>}
          {register.isError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>회원가입 실패. 입력값을 다시 확인해주세요.</p>}

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" onClick={handleBack} style={{ ...secondaryBtn, flex: 1 }}>← 이전</button>
            <button type="submit" disabled={register.isPending} style={{ ...primaryBtn, flex: 2, opacity: register.isPending ? 0.7 : 1 }}>
              {register.isPending ? '가입 중...' : '가입 완료'}
            </button>
          </div>
        </form>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '20px 0 12px' }}>
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid #e5e7eb' }} />
        <span style={{ fontSize: 12, color: '#9ca3af' }}>또는</span>
        <hr style={{ flex: 1, border: 'none', borderTop: '1px solid #e5e7eb' }} />
      </div>

      <button
        type="button"
        onClick={redirectToGoogle}
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
        Google로 계속하기
      </button>

      <p style={{ marginTop: 16, textAlign: 'center', fontSize: 13, color: '#6b7280' }}>
        이미 계정이 있으신가요? <Link to="/login" style={{ color: '#2563eb', fontWeight: 600 }}>로그인</Link>
      </p>
    </div>
  )
}
