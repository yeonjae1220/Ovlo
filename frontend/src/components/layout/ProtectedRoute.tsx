import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

export default function ProtectedRoute() {
  const { accessToken, currentUser } = useAuthStore()
  const location = useLocation()

  // Both token and user must exist; stale token without valid user data → re-login
  if (!accessToken || !currentUser) return <Navigate to="/login" replace />

  // Google OAuth 신규 유저는 온보딩 완료 전까지 /onboarding 외 접근 차단
  if (currentUser.status === 'PENDING_ONBOARDING' && location.pathname !== '/onboarding') {
    return <Navigate to="/onboarding" replace />
  }

  return <Outlet />
}
