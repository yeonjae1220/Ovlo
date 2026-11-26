import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import { assertAuthSession, beginAuthChange, captureAuthSession, serializeAuthCookie } from '../utils/authSession'
import type { AuthToken, GoogleLoginResult, Member } from '../types'

function login<T extends AuthToken>(path: string, body: unknown) {
  const session = beginAuthChange()
  return serializeAuthCookie(async () => {
    assertAuthSession(session)
    const { data: token } = await axios.post<T>(`/api/v1/auth/${path}`, body, { withCredentials: true })
    assertAuthSession(session)
    const { data: user } = await axios.get<Member>(`/api/v1/members/${token.memberId}`, {
      headers: { Authorization: `Bearer ${token.accessToken}` }, withCredentials: true,
    })
    assertAuthSession(session)
    if (String(user.id) !== String(token.memberId)) throw new Error('Login member mismatch')
    useAuthStore.getState().setAuth(token.accessToken, user)
    return { ...token, session: captureAuthSession() }
  })
}

export const authApi = {
  login: (email: string, password: string) => login<AuthToken>('login', { email, password }),

  logout: () => {
    const token = useAuthStore.getState().accessToken
    const session = beginAuthChange()
    return serializeAuthCookie(async () => {
      assertAuthSession(session)
      await axios.post('/api/v1/auth/logout', undefined, {
        withCredentials: true, headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      assertAuthSession(session)
      return session
    })
  },

  googleLogin: (code: string, redirectUri: string) => login<GoogleLoginResult>('google', { code, redirectUri }),

  appleLogin: (identityToken: string, nonce: string, name: string | null) =>
    login<GoogleLoginResult>('apple', { identityToken, nonce, name }),
}
