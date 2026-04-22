import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useBreakpoint } from '../../hooks/useBreakpoint'

const navItems = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/members', label: '회원 관리' },
  { to: '/admin/boards', label: '게시판 관리' },
  { to: '/admin/posts', label: '게시글 관리' },
  { to: '/admin/universities', label: '대학교 관리' },
]

export default function AdminLayout() {
  const { isMobile } = useBreakpoint()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const sidebar = (
    <aside
      style={{
        width: 220,
        minHeight: '100vh',
        background: '#1e293b',
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        padding: '1.5rem 0',
        ...(isMobile
          ? {
              position: 'fixed',
              top: 0,
              left: 0,
              bottom: 0,
              zIndex: 200,
              transform: sidebarOpen ? 'translateX(0)' : 'translateX(-100%)',
              transition: 'transform 0.25s ease',
              minHeight: '100dvh',
            }
          : {}),
      }}
    >
      <div
        style={{
          padding: '0 1.25rem 1.5rem',
          borderBottom: '1px solid #334155',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span style={{ color: '#f1f5f9', fontWeight: 700, fontSize: '1rem', letterSpacing: '-0.01em' }}>
          Ovlo Admin
        </span>
        {isMobile && (
          <button
            onClick={() => setSidebarOpen(false)}
            style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: 20, padding: 0, lineHeight: 1 }}
            aria-label="메뉴 닫기"
          >
            ✕
          </button>
        )}
      </div>
      <nav style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', padding: '0 0.75rem' }}>
        {navItems.map(({ to, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={() => isMobile && setSidebarOpen(false)}
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
  )

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f8fafc', color: '#1e293b' }}>
      {sidebar}

      {isMobile && sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 199 }}
        />
      )}

      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        {isMobile && (
          <header
            style={{
              height: 52,
              background: '#1e293b',
              display: 'flex',
              alignItems: 'center',
              padding: '0 1rem',
              position: 'sticky',
              top: 0,
              zIndex: 100,
              flexShrink: 0,
            }}
          >
            <button
              onClick={() => setSidebarOpen(true)}
              style={{ background: 'none', border: 'none', color: '#f1f5f9', fontSize: 22, cursor: 'pointer', padding: '4px 8px', lineHeight: 1 }}
              aria-label="메뉴 열기"
            >
              ☰
            </button>
            <span style={{ color: '#f1f5f9', fontWeight: 700, marginLeft: 8 }}>Ovlo Admin</span>
          </header>
        )}

        <main style={{ flex: 1, minWidth: 0 }}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
