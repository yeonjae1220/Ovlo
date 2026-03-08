import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

export default function ProtectedRoute() {
  const { accessToken, currentUser } = useAuthStore()
  // Both token and user must exist; stale token without valid user data → re-login
  if (!accessToken || !currentUser) return <Navigate to="/login" replace />
  return <Outlet />
}
