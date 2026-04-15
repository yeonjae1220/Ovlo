import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

export default function AdminRoute() {
  const { accessToken, currentUser } = useAuthStore()

  if (!accessToken || !currentUser) return <Navigate to="/login" replace />
  if (currentUser.role !== 'ADMIN') return <Navigate to="/boards" replace />

  return <Outlet />
}
