import apiClient from '../utils/axios'
import type { AuthToken } from '../types'

export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<AuthToken>('/auth/login', { email, password }).then((r) => r.data),

  logout: () => apiClient.post('/auth/logout'),

  refresh: (refreshToken: string) =>
    apiClient.post<{ accessToken: string }>('/auth/refresh', { refreshToken }).then((r) => r.data),
}
