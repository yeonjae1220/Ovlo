import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useRegister } from '../../hooks/useAuth'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useMemberSearch } from '../../hooks/useMember'

const DEGREE_TYPES = [
  { value: 'BACHELOR', label: '학사' },
  { value: 'MASTER', label: '석사' },
  { value: 'DOCTOR', label: '박사' },
]

const TOTAL_STEPS = 3

const stepTitles = ['계정 정보', '개인 정보', '학업 정보']

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

  // ── Styles ────────────────────────────────────────────────────────────────
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

  // ── Step indicator ────────────────────────────────────────────────────────
  const StepIndicator = () => (
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

  // ── Step 1: 계정 정보 ──────────────────────────────────────────────────────
  const Step1 = () => (
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
  )

  // ── Step 2: 개인 정보 ──────────────────────────────────────────────────────
  const Step2 = () => (
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
  )

  // ── Step 3: 학업 정보 ──────────────────────────────────────────────────────
  const Step3 = () => (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={fieldStyle}>
        <label style={labelStyle}>소속 대학</label>
        {selectedUniId ? (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ flex: 1, padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 8, fontSize: 14, color: '#16a34a', fontWeight: 500 }}>
              ✓ {selectedUniName}
            </span>
            <button type="button"
              style={{ padding: '10px 14px', border: '1px solid #d1d5db', borderRadius: 8, background: '#f9fafb', cursor: 'pointer', fontSize: 13 }}
              onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}>
              변경
            </button>
          </div>
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
            {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}학년</option>)}
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
  )

  return (
    <div style={containerStyle}>
      <h2 style={{ margin: '0 0 24px', fontSize: 22, fontWeight: 700, color: '#111827' }}>회원가입</h2>

      <StepIndicator />

      {step === 1 && <Step1 />}
      {step === 2 && <Step2 />}
      {step === 3 && <Step3 />}

      <p style={{ marginTop: 20, textAlign: 'center', fontSize: 13, color: '#6b7280' }}>
        이미 계정이 있으신가요? <Link to="/login" style={{ color: '#2563eb', fontWeight: 600 }}>로그인</Link>
      </p>
    </div>
  )
}
