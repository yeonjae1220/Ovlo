import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { memberApi } from '../api/member'
import { useAuthStore } from '../store/authStore'

export function useLogin() {
  const { setAuth, setAccessToken } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      authApi.login(email, password),
    onSuccess: async (token) => {
      // 토큰을 먼저 store에 저장해야 axios 인터셉터가 Authorization 헤더를 붙일 수 있다
      setAccessToken(token.accessToken)
      const user = await memberApi.getById(String(token.memberId))
      setAuth(token.accessToken, token.refreshToken, user)
      navigate('/')
    },
  })
}

export function useRegister() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: memberApi.register,
    onSuccess: () => navigate('/login'),
  })
}
