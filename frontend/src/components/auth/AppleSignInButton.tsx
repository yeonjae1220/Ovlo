'use client'

import { useEffect, useSyncExternalStore } from 'react'
import { useAppleLogin } from '../../hooks/useAuth'
import { useI18n } from '../../i18n/I18nProvider'
import type { MessageKey } from '../../i18n/messages'
import {
  AppleSignInCanceledError,
  isAppleSignInAvailable,
  prepareAppleSignIn,
  requestAppleCredential,
} from '../../utils/appleSignIn'

/**
 * Apple 로그인 버튼(Apple HIG: 검은 바탕·흰 로고, 다른 소셜 버튼과 같은 크기).
 * 설정이 없거나 Android 앱이면 렌더하지 않는다. 판정은 브라우저 전역(Capacitor 브리지)에 달려 있어
 * 서버 렌더(항상 숨김)와 어긋나지 않도록 hydrate 뒤에 판정한다.
 */
export function AppleSignInButton({ labelKey }: { labelKey: MessageKey }) {
  const { t } = useI18n()
  const appleLogin = useAppleLogin()
  const available = useSyncExternalStore(subscribeNever, isAppleSignInAvailable, () => false)

  // 클릭 때 바로 팝업을 열 수 있게 SDK·nonce 를 미리 준비한다. 실패하면 클릭 때 다시 시도하고 그때 오류를 보인다.
  useEffect(() => {
    if (available) prepareAppleSignIn().catch(() => {})
  }, [available])

  if (!available) return null

  const error = appleLogin.error
  const errorMessage = !error || error instanceof AppleSignInCanceledError
    ? null
    : (error as { response?: { data?: { error?: string } } }).response?.data?.error ?? t('login.apple.error')

  return (
    <div style={{ marginTop: 10 }}>
      <button
        type="button"
        onClick={() => appleLogin.mutate(requestAppleCredential())}
        disabled={appleLogin.isPending}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          minHeight: 42,
          padding: '10px 0',
          border: '1px solid #000',
          borderRadius: 8,
          background: '#000',
          color: '#fff',
          fontSize: 14,
          fontWeight: 500,
          cursor: appleLogin.isPending ? 'wait' : 'pointer',
          opacity: appleLogin.isPending ? 0.7 : 1,
        }}
      >
        <svg width="16" height="18" viewBox="0 0 814 1000" aria-hidden="true" fill="currentColor">
          <path d="M788.1 340.9c-5.8 4.5-108.2 62.2-108.2 190.5 0 148.4 130.3 200.9 134.2 202.2-.6 3.2-20.7 71.9-68.7 141.9-42.8 61.6-87.5 123.1-155.5 123.1s-85.5-39.5-164-39.5c-76.5 0-103.7 40.8-165.9 40.8s-105.6-57-155.5-127C46.7 790.7 0 663 0 541.8c0-194.4 126.4-297.5 250.8-297.5 66.1 0 121.2 43.4 162.7 43.4 39.5 0 101.1-46 176.3-46 28.5 0 130.9 2.6 198.3 99.2zm-234-181.5c31.1-36.9 53.1-88.1 53.1-139.3 0-7.1-.6-14.3-1.9-20.1-50.6 1.9-110.8 33.7-147.1 75.8-28.5 32.4-55.1 83.6-55.1 135.5 0 7.8 1.3 15.6 1.9 18.1 3.2.6 8.4 1.3 13.6 1.3 45.4 0 102.5-30.4 135.5-71.3z" />
        </svg>
        {t(labelKey)}
      </button>
      {errorMessage && (
        <p role="alert" style={{ margin: '8px 0 0', color: 'var(--color-danger)', fontSize: 13, fontWeight: 700 }}>
          {errorMessage}
        </p>
      )}
    </div>
  )
}

// 설정과 플랫폼은 페이지가 떠 있는 동안 바뀌지 않는다.
function subscribeNever(): () => void {
  return () => {}
}
