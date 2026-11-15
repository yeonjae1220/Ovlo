'use client'

import { useAuthStore } from '../store/authStore'

const CHANGE_KEY = 'ovlo-auth-session-change'
export class AuthSessionChangedError extends Error {
  constructor() { super('Authentication session changed'); this.name = 'AuthSessionChangedError' }
}
function stamp(): string | null {
  try { return window.localStorage.getItem(CHANGE_KEY) } catch { return null }
}
export function captureAuthSession() {
  return { version: useAuthStore.getState().sessionVersion, stamp: stamp() }
}
export type AuthSession = ReturnType<typeof captureAuthSession>
export function isCurrentAuthSession(session: AuthSession) {
  return session.version === useAuthStore.getState().sessionVersion && session.stamp === stamp()
}
export function isEndedAuthSession(session: AuthSession) {
  return isCurrentAuthSession({ ...session, version: session.version + 1 }) && !useAuthStore.getState().accessToken
}
export function assertAuthSession(session: AuthSession) {
  if (!isCurrentAuthSession(session)) throw new AuthSessionChangedError()
}
export function beginAuthChange() {
  useAuthStore.getState().clearAuth()
  try { window.localStorage.setItem(CHANGE_KEY, crypto.randomUUID()) } catch { /* Private storage can be unavailable. */ }
  return captureAuthSession()
}
export function watchAuthSessionChanges() {
  const listener = (event: StorageEvent) => {
    if (event.key === CHANGE_KEY || event.key === null) useAuthStore.getState().clearAuth()
  }
  window.addEventListener('storage', listener)
  return () => window.removeEventListener('storage', listener)
}

// Never abort an in-flight cookie mutation: Set-Cookie must finish before the next
// login/logout/refresh starts. The epoch guard discards its obsolete application result.
let tail: Promise<unknown> = Promise.resolve()
export function serializeAuthCookie<T>(task: () => Promise<T>): Promise<T> {
  const run = tail.then(async (): Promise<T> => {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined
    return locks ? await locks.request('ovlo-auth-refresh', task) : await task()
  })
  tail = run.catch(() => {})
  return run
}
