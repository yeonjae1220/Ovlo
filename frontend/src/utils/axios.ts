import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: attach JWT
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: 401 → refresh → retry
let isRefreshing = false
let pendingQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null) => {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token!)
  })
  pendingQueue = []
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config

    const status = error.response?.status
    // 403 with no authentication entry point configured returns 403 instead of 401;
    // treat it as an auth failure and clear state immediately.
    if (status === 403) {
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
      return Promise.reject(error)
    }
    if (status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    const { refreshToken, setAccessToken, clearAuth } = useAuthStore.getState()
    if (!refreshToken) {
      clearAuth()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return apiClient(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken })
      const newToken: string = data.accessToken
      setAccessToken(newToken)
      processQueue(null, newToken)
      original.headers.Authorization = `Bearer ${newToken}`
      return apiClient(original)
    } catch (err) {
      processQueue(err, null)
      clearAuth()
      window.location.href = '/login'
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  }
)

export default apiClient
