import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useRegister } from '../../hooks/useAuth'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useMemberSearch } from '../../hooks/useMember'

const DEGREE_TYPES = ['BACHELOR', 'MASTER', 'DOCTOR']

export default function RegisterPage() {
  const [form, setForm] = useState({
    nickname: '',
    name: '',
    hometown: '',
    email: '',
    password: '',
    majorName: '',
    degreeType: 'BACHELOR',
    gradeLevel: '1',
  })
  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniId, setSelectedUniId] = useState<number | null>(null)
  const [selectedUniName, setSelectedUniName] = useState('')
  const [formError, setFormError] = useState('')

  const register = useRegister()
  const { data: universities } = useUniversitySearch(uniQuery)
  const { data: nicknameResults } = useMemberSearch(form.nickname)
  const nicknameAvailable = form.nickname.length >= 2 && nicknameResults?.length === 0
  const nicknameTaken = form.nickname.length >= 2 && (nicknameResults?.length ?? 0) > 0

  const set = (key: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [key]: e.target.value }))

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setFormError('')

    if (!selectedUniId) {
      setFormError('소속 대학을 검색하여 선택해주세요.')
      return
    }
    if (nicknameTaken) {
      setFormError('이미 사용 중인 닉네임입니다.')
      return
    }
    if (!/^[a-zA-Z0-9._]+$/.test(form.nickname)) {
      setFormError('닉네임은 영문, 숫자, ., _ 만 사용 가능합니다.')
      return
    }

    register.mutate({
      nickname: form.nickname,
      name: form.name,
      hometown: form.hometown,
      email: form.email,
      password: form.password,
      homeUniversityId: selectedUniId,
      majorName: form.majorName,
      degreeType: form.degreeType,
      gradeLevel: Number(form.gradeLevel),
    })
  }

  return (
    <div style={{ maxWidth: 440, margin: '60px auto', padding: 24 }}>
      <h1>회원가입</h1>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>

        <label>닉네임 (영문, 숫자, ., _ 가능 / 2~30자)</label>
        <div style={{ position: 'relative' }}>
          <input
            value={form.nickname}
            onChange={set('nickname')}
            placeholder="예: john.doe_95"
            required
            minLength={2}
            maxLength={30}
            style={{ width: '100%', paddingRight: 90 }}
          />
          {form.nickname.length >= 2 && (
            <span style={{
              position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)',
              fontSize: 12, color: nicknameAvailable ? 'green' : 'red'
            }}>
              {nicknameAvailable ? '✓ 사용 가능' : '✗ 중복'}
            </span>
          )}
        </div>

        <label>이름</label>
        <input value={form.name} onChange={set('name')} required />

        <label>고향 (도시, 국가)</label>
        <input placeholder="예: Seoul, Korea" value={form.hometown} onChange={set('hometown')} required />

        <label>이메일</label>
        <input type="email" value={form.email} onChange={set('email')} required />

        <label>비밀번호 (8자 이상)</label>
        <input type="password" minLength={8} value={form.password} onChange={set('password')} required />

        <label>소속 대학</label>
        {selectedUniId ? (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ flex: 1, padding: '4px 8px', border: '1px solid #ccc', borderRadius: 4 }}>
              ✓ {selectedUniName}
            </span>
            <button type="button" onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}>
              변경
            </button>
          </div>
        ) : (
          <>
            <input
              placeholder="대학명 검색..."
              value={uniQuery}
              onChange={(e) => setUniQuery(e.target.value)}
            />
            {uniQuery.length >= 1 && universities && universities.length > 0 && (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, border: '1px solid #ddd', borderRadius: 4, maxHeight: 160, overflowY: 'auto' }}>
                {universities.slice(0, 8).map((u) => (
                  <li
                    key={u.id}
                    onClick={() => { setSelectedUniId(Number(u.id)); setSelectedUniName(u.name); setUniQuery('') }}
                    style={{ padding: '6px 10px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0' }}
                  >
                    {u.name} <span style={{ color: '#888', fontSize: 12 }}>({u.countryCode})</span>
                  </li>
                ))}
              </ul>
            )}
            {uniQuery.length >= 1 && universities?.length === 0 && (
              <p style={{ margin: 0, fontSize: 13, color: '#888' }}>검색 결과 없음</p>
            )}
          </>
        )}

        <label>전공</label>
        <input placeholder="예: Computer Science" value={form.majorName} onChange={set('majorName')} required />

        <label>학위 유형</label>
        <select value={form.degreeType} onChange={set('degreeType')}>
          {DEGREE_TYPES.map((d) => <option key={d} value={d}>{d}</option>)}
        </select>

        <label>학년 (1–6)</label>
        <select value={form.gradeLevel} onChange={set('gradeLevel')}>
          {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}학년</option>)}
        </select>

        {formError && <p style={{ color: 'red', margin: 0 }}>{formError}</p>}
        {register.isError && <p style={{ color: 'red', margin: 0 }}>회원가입 실패. 입력값을 확인해주세요.</p>}
        {register.isSuccess && <p style={{ color: 'green', margin: 0 }}>완료! 로그인해주세요.</p>}

        <button type="submit" disabled={register.isPending || nicknameTaken}>
          {register.isPending ? '가입 중...' : '회원가입'}
        </button>
      </form>
      <p>이미 계정이 있으신가요? <Link to="/login">로그인</Link></p>
    </div>
  )
}
