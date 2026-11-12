'use client'

import { Badge } from '../../components/ui'
import type { NoticeCategory, NoticeStatus } from '../../api/office'
import { useI18n } from '../../i18n/I18nProvider'

export function StatusBadge({ status }: { status: NoticeStatus }) {
  const { t } = useI18n()
  const tone = status === 'APPROVED' ? 'success' : status === 'DRAFT' ? 'warning' : status === 'EXPIRED' ? 'danger' : 'neutral'
  return <Badge tone={tone}>{t(`office.status.${status}`)}</Badge>
}

export function CategoryBadge({ category }: { category: NoticeCategory }) {
  const { t } = useI18n()
  return <Badge tone="info">{t(`office.category.${category}`)}</Badge>
}
