'use client'

import { Badge } from '../../components/ui'
import { useI18n } from '../../i18n/I18nProvider'
import type { TrustLevel } from '../../api/verification'

/** 신뢰 등급 뱃지. UNVERIFIED는 기본적으로 렌더하지 않는다(showUnverified로 노출 가능). */
export function TrustBadge({
  level,
  showUnverified = false,
}: {
  level: TrustLevel
  showUnverified?: boolean
}) {
  const { t } = useI18n()

  if (level === 'UNVERIFIED' && !showUnverified) return null

  if (level === 'STUDENT') {
    return (
      <Badge tone="success" title={t('verification.badge.student.hint')}>
        ✓ {t('verification.badge.student')}
      </Badge>
    )
  }

  return <Badge tone="neutral">{t('verification.badge.unverified')}</Badge>
}
