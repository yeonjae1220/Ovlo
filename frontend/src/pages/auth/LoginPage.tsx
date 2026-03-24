import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useLogin } from '../../hooks/useAuth'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const login = useLogin()

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)
    login.mutate(
      { email, password },
      {
        onError: () => setErrorMsg('이메일 또는 비밀번호가 올바르지 않습니다.'),
      },
    )
  }

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24 }}>
      <h1>Ovlo 로그인</h1>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => { setEmail(e.target.value); setErrorMsg(null) }}
          required
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => { setPassword(e.target.value); setErrorMsg(null) }}
          required
        />
        {errorMsg && <p style={{ color: 'red', margin: 0 }}>{errorMsg}</p>}
        <button type="submit" disabled={login.isPending}>
          {login.isPending ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <p>
        계정이 없으신가요? <Link to="/register">회원가입</Link>
      </p>
    </div>
  )
}
