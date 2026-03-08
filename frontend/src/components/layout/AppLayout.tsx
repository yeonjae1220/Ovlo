import { Link, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

export default function AppLayout() {
  const { currentUser } = useAuthStore()

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{ padding: '12px 24px', borderBottom: '1px solid #eee', display: 'flex', alignItems: 'center', gap: '16px' }}>
        <Link to="/" style={{ fontWeight: 'bold', fontSize: '20px', textDecoration: 'none' }}>Ovlo</Link>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '12px', alignItems: 'center' }}>
          {currentUser && (
            <Link to={`/profile/${currentUser.id}`} style={{ color: '#555' }}>{currentUser.nickname}</Link>
          )}
        </div>
      </header>

      <main style={{ flex: 1, padding: '24px' }}>
        <Outlet />
      </main>

      <nav style={{ borderTop: '1px solid #eee', padding: '12px', display: 'flex', justifyContent: 'space-around' }}>
        <Link to="/">홈</Link>
        <Link to="/universities">대학</Link>
        <Link to="/chat">채팅</Link>
        {currentUser && <Link to={`/profile/${currentUser.id}`}>프로필</Link>}
      </nav>
    </div>
  )
}
