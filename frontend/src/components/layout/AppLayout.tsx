import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import { useBreakpoint } from '../../hooks/useBreakpoint'

const NAV_ITEMS = [
  { to: '/boards', label: '홈' },
  { to: '/exchange-universities', label: '교환대학' },
  { to: '/chat', label: '채팅' },
]

export default function AppLayout() {
  const { currentUser } = useAuthStore()
  const { isMobile } = useBreakpoint()
  const userLabel = currentUser?.nickname ?? currentUser?.name ?? '프로필'

  const desktopLinkStyle = (isActive: boolean): React.CSSProperties => ({
    padding: '6px 12px',
    borderRadius: 6,
    textDecoration: 'none',
    fontSize: 14,
    fontWeight: isActive ? 600 : 400,
    color: isActive ? '#a78bfa' : '#94a3b8',
    background: isActive ? '#7c3aed22' : 'transparent',
    transition: 'color 0.15s, background 0.15s',
  })

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid #2d3748',
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          position: 'sticky',
          top: 0,
          background: '#242424',
          zIndex: 100,
        }}
      >
        <Link to="/" style={{ fontWeight: 'bold', fontSize: 20, textDecoration: 'none', color: '#a78bfa', flexShrink: 0 }}>
          Ovlo
        </Link>

        {!isMobile && (
          <nav style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
            {NAV_ITEMS.map(({ to, label }) => (
              <NavLink key={to} to={to} style={({ isActive }) => desktopLinkStyle(isActive)}>
                {label}
              </NavLink>
            ))}
            {currentUser && (
              <NavLink to={`/profile/${currentUser.id}`} style={({ isActive }) => desktopLinkStyle(isActive)}>
                프로필
              </NavLink>
            )}
          </nav>
        )}

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 12, alignItems: 'center' }}>
          {currentUser && (
            <Link
              to={`/profile/${currentUser.id}`}
              style={{
                color: '#94a3b8',
                fontSize: 14,
                textDecoration: 'none',
                maxWidth: isMobile ? 120 : 220,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={userLabel}
            >
              {userLabel}
            </Link>
          )}
        </div>
      </header>

      <main
        style={{
          flex: 1,
          padding: isMobile ? '16px 16px' : '32px 24px',
          paddingBottom: isMobile ? 'calc(16px + 60px)' : '32px',
        }}
      >
        <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
          <Outlet />
        </div>
      </main>

      {isMobile && (
        <nav
          style={{
            borderTop: '1px solid #2d3748',
            padding: '8px 0',
            display: 'flex',
            justifyContent: 'space-around',
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            background: '#242424',
            zIndex: 100,
          }}
        >
          {NAV_ITEMS.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              style={({ isActive }) => ({
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textDecoration: 'none',
                fontSize: 12,
                color: isActive ? '#a78bfa' : '#94a3b8',
                padding: '4px 8px',
                minWidth: 0,
                flex: 1,
              })}
            >
              {label}
            </NavLink>
          ))}
          {currentUser && (
            <NavLink
              to={`/profile/${currentUser.id}`}
              style={({ isActive }) => ({
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textDecoration: 'none',
                fontSize: 12,
                color: isActive ? '#a78bfa' : '#94a3b8',
                padding: '4px 8px',
                minWidth: 0,
                flex: 1,
              })}
            >
              프로필
            </NavLink>
          )}
        </nav>
      )}
    </div>
  )
}
