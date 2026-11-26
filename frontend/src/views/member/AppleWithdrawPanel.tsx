'use client'

import { useEffect, useSyncExternalStore } from 'react'
import { useI18n } from '../../i18n/I18nProvider'
import {
  isAppleSignInAvailable,
  prepareAppleSignIn,
  requestAppleCredential,
  type AppleCredential,
} from '../../utils/appleSignIn'

const C = {
  danger: 'var(--color-danger)',
  dangerSoft: 'var(--color-danger-soft)',
  textDim: 'var(--color-text-dim)',
}

const subscribeNever = () => () => {}

/**
 * Apple 로그인 계정의 탈퇴 확인.
 *
 * 탈퇴하려면 서버가 Apple 토큰을 폐기해야 하고(App Store 5.1.1(v)), 그 토큰은 이 자리에서 받는
 * authorization code 로만 얻는다. 웹 팝업이 막히지 않도록 확인 버튼의 클릭 안에서 곧바로
 * requestAppleCredential() 을 부른다 — window.confirm 뒤에 열면 Chrome 의 사용자 활성화가 끝나 있을 수 있다.
 * Android 앱은 Apple 인증을 띄울 수 없어 웹·iOS 앱에서 탈퇴하도록 안내한다.
 */
export function AppleWithdrawPanel({
  pending,
  error,
  onConfirm,
  onCancel,
}: {
  pending: boolean
  /** 화면 위쪽 알림은 스크롤에 가려질 수 있어, 실패 사유를 패널 안에도 보인다. */
  error: string
  onConfirm: (credential: Promise<AppleCredential>) => void
  onCancel: () => void
}) {
  const { t } = useI18n()
  const available = useSyncExternalStore(subscribeNever, isAppleSignInAvailable, () => false)

  useEffect(() => {
    if (available) prepareAppleSignIn().catch(() => {})
  }, [available])

  return (
    <div
      role="alertdialog"
      aria-label={t('profile.withdraw')}
      style={{ marginTop: 12, padding: 16, borderRadius: 8, border: `1px solid ${C.danger}`, background: C.dangerSoft }}
    >
      <p style={{ margin: '0 0 12px', fontSize: 14 }}>
        {available ? t('profile.withdraw.apple.description') : t('profile.withdraw.apple.unavailable')}
      </p>
      {error && <p role="alert" style={{ margin: '0 0 12px', fontSize: 13, color: C.danger }}>{error}</p>}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {available && (
          <button
            type="button"
            onClick={() => onConfirm(requestAppleCredential())}
            disabled={pending}
            style={{ background: '#000', color: '#fff', border: '1px solid #000', cursor: pending ? 'wait' : 'pointer' }}
          >
            {t('profile.withdraw.apple.btn')}
          </button>
        )}
        <button type="button" onClick={onCancel} disabled={pending} style={{ color: C.textDim }}>
          {t('common.cancel')}
        </button>
      </div>
    </div>
  )
}
