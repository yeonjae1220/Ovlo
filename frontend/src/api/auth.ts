import apiClient from '../utils/axios'
import type { AuthToken, GoogleLoginResult } from '../types'

export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<AuthToken>('/auth/login', { email, password }).then((r) => r.data),

  logout: () =>
    apiClient.post('/auth/logout'),

  refresh: () =>
    apiClient.post<{ accessToken: string }>('/auth/refresh').then((r) => r.data),

  googleLogin: (code: string, redirectUri: string) =>
    apiClient.post<GoogleLoginResult>('/auth/google', { code, redirectUri }).then((r) => r.data),
}
