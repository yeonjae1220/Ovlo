import { useState, type FormEvent } from 'react'
import { useGlobalUniversitySearch } from '../../hooks/useUniversity'
import { useCompleteOnboarding } from '../../hooks/useAuth'

const DEGREE_TYPES = [
  { value: 'BACHELOR', label: '학사' },
  { value: 'MASTER', label: '석사' },
  { value: 'DOCTOR', label: '박사' },
]

type FormData = {
  hometown: string
  majorName: string
  degreeType: string
  gradeLevel: string
}

export default function OnboardingPage() {
  const [form, setForm] = useState<FormData>({
    hometown: '',
    majorName: '',
    degreeType: 'BACHELOR',
    gradeLevel: '1',
  })
  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniId, setSelectedUniId] = useState<number | null>(null)
  const [selectedUniName, setSelectedUniName] = useState('')
  const [formError, setFormError] = useState('')

  const completeOnboarding = useCompleteOnboarding()
  const { data: universities } = useGlobalUniversitySearch(uniQuery)

  const set = (key: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [key]: e.target.value }))
      setFormError('')
    }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.hometown.trim()) { setFormError('고향을 입력해주세요.'); return }
    if (!selectedUniId) { setFormError('소속 대학을 검색하여 선택해주세요.'); return }
    if (!form.majorName.trim()) { setFormError('전공을 입력해주세요.'); return }

    completeOnboarding.mutate({
      hometown: form.hometown,
      homeUniversityId: selectedUniId,
      majorName: form.majorName,
      degreeType: form.degreeType,
      gradeLevel: Number(form.gradeLevel),
    })
  }

  const containerStyle: React.CSSProperties = {
    maxWidth: 460,
    margin: '60px auto',
    padding: '32px 28px',
    border: '1px solid #e5e7eb',
    borderRadius: 12,
    boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
    fontFamily: 'system-ui, sans-serif',
  }

  const fieldStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 6 }
  const labelStyle: React.CSSProperties = { fontSize: 13, fontWeight: 600, color: '#374151' }
  const inputStyle: React.CSSProperties = {
    padding: '10px 12px',
    border: '1px solid #d1d5db',
    borderRadius: 8,
    fontSize: 14,
    outline: 'none',
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

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: 28 }}>
        <p style={{ fontSize: 12, color: '#7c3aed', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: 8 }}>
          거의 다 됐어요!
        </p>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, color: '#111827' }}>프로필을 완성해주세요</h2>
        <p style={{ margin: '8px 0 0', fontSize: 13, color: '#6b7280' }}>
          Ovlo를 시작하려면 몇 가지 정보가 더 필요해요.
        </p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <div style={fieldStyle}>
          <label style={labelStyle}>고향 (도시, 국가)</label>
          <input
            style={inputStyle}
            value={form.hometown}
            onChange={set('hometown')}
            placeholder="예: Seoul, Korea"
            required
          />
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle}>소속 대학</label>
          {selectedUniId ? (
            <button
              type="button"
              onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}
              style={{
                padding: '10px 12px',
                border: '1px solid #86efac',
                borderRadius: 8,
                background: '#f0fdf4',
                color: '#16a34a',
                fontWeight: 500,
                fontSize: 14,
                cursor: 'pointer',
                textAlign: 'left',
                width: '100%',
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
                onChange={(e) => { setUniQuery(e.target.value); setFormError('') }}
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
            <select style={{ ...inputStyle, background: '#f9fafb', color: '#374151' }} value={form.degreeType} onChange={set('degreeType')}>
              {DEGREE_TYPES.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
          </div>
          <div style={{ ...fieldStyle, flex: 1 }}>
            <label style={labelStyle}>학년</label>
            <select style={{ ...inputStyle, background: '#f9fafb', color: '#374151' }} value={form.gradeLevel} onChange={set('gradeLevel')}>
              {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}학년</option>)}
            </select>
          </div>
        </div>

        {formError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{formError}</p>}
        {completeOnboarding.isError && (
          <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>저장 실패. 다시 시도해주세요.</p>
        )}

        <button type="submit" style={primaryBtn} disabled={completeOnboarding.isPending}>
          {completeOnboarding.isPending ? '저장 중...' : 'Ovlo 시작하기 →'}
        </button>
      </form>
    </div>
  )
}
