import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/members', label: '회원 관리' },
  { to: '/admin/boards', label: '게시판 관리' },
  { to: '/admin/posts', label: '게시글 관리' },
  { to: '/admin/universities', label: '대학교 관리' },
]

export default function AdminLayout() {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f8fafc', color: '#1e293b' }}>
      {/* 사이드바 */}
      <aside
        style={{
          width: 220,
          minHeight: '100vh',
          background: '#1e293b',
          flexShrink: 0,
          display: 'flex',
          flexDirection: 'column',
          padding: '1.5rem 0',
        }}
      >
        <div style={{ padding: '0 1.25rem 1.5rem', borderBottom: '1px solid #334155' }}>
          <span style={{ color: '#f1f5f9', fontWeight: 700, fontSize: '1rem', letterSpacing: '-0.01em' }}>
            Ovlo Admin
          </span>
        </div>
        <nav style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', padding: '0 0.75rem' }}>
          {navItems.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              style={({ isActive }) => ({
                display: 'block',
                padding: '0.6rem 0.75rem',
                borderRadius: '0.375rem',
                textDecoration: 'none',
                fontSize: '0.9rem',
                fontWeight: isActive ? 600 : 400,
                background: isActive ? '#334155' : 'transparent',
                color: isActive ? '#f1f5f9' : '#94a3b8',
                transition: 'background 0.15s, color 0.15s',
              })}
            >
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* 메인 콘텐츠 */}
      <main style={{ flex: 1, minWidth: 0 }}>
        <Outlet />
      </main>
    </div>
  )
}
