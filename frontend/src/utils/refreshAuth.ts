'use client'

import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

/**
 * refresh 토큰은 1회용(rotation)이라 같은 쿠키로 동시에 두 번 보내면 하나만 성공한다.
 * 그래서 모든 재발급은 이 함수 하나로만 보낸다.
 *
 * - 탭 안: 진행 중인 Promise 를 공유해 layout·axios interceptor·선제 갱신 타이머가 동시에 불러도 1회.
 * - 탭 사이: Web Locks 로 직렬화한다. 다음 탭은 앞 탭이 받은 새 쿠키로 보내므로 충돌하지 않는다.
 * - 그래도 겹치면 서버가 진 요청에 409 를 준다. 이긴 요청의 Set-Cookie 가 이미 반영됐으므로
 *   잠시 뒤 한 번 더 보내면 된다.
 *
 * 반환: 새 access token, 또는 세션이 끝났으면(400·401·403) null — 이때 인증 상태를 지운다.
 * 네트워크 오류·5xx·429 처럼 일시적인 실패는 재시도 후에도 실패하면 throw 한다.
 * 일시적 실패를 로그아웃으로 처리하지 않기 위해서다(GLOBAL-PIT-012).
 */

const REFRESH_URL = '/api/v1/auth/refresh'
const LOCK_NAME = 'ovlo-auth-refresh'
const CONFLICT_RETRY_DELAY_MS = 300
const TRANSIENT_RETRY_DELAYS_MS = [1000, 3000]
const SESSION_ENDED_STATUSES = new Set([400, 401, 403])

export class TransientRefreshError extends Error {
  constructor() {
    super('토큰 재발급이 일시적으로 실패했습니다')
    this.name = 'TransientRefreshError'
  }
}

let pending: Promise<string | null> | null = null

export function refreshAuth(): Promise<string | null> {
  if (pending) return pending

  pending = withCrossTabLock(requestWithRetry).finally(() => {
    pending = null
  })
  return pending
}

async function withCrossTabLock(task: () => Promise<string | null>): Promise<string | null> {
  const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined
  if (!locks) return task()
  return await locks.request(LOCK_NAME, task)
}

async function requestWithRetry(): Promise<string | null> {
  let conflictRetried = false
  let transientAttempt = 0

  for (;;) {
    try {
      const { data } = await axios.post<{ accessToken: string }>(REFRESH_URL, undefined, {
        withCredentials: true,
      })
      useAuthStore.getState().setAccessToken(data.accessToken)
      return data.accessToken
    } catch (err) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined

      if (status !== undefined && SESSION_ENDED_STATUSES.has(status)) {
        useAuthStore.getState().clearAuth()
        return null
      }
      if (status === 409 && !conflictRetried) {
        conflictRetried = true
        await sleep(CONFLICT_RETRY_DELAY_MS)
        continue
      }
      if (transientAttempt < TRANSIENT_RETRY_DELAYS_MS.length) {
        await sleep(TRANSIENT_RETRY_DELAYS_MS[transientAttempt])
        transientAttempt++
        continue
      }
      throw new TransientRefreshError()
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
