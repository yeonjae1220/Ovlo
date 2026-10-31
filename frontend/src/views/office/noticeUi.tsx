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

export function formatDate(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(date)
}

/** UI 언어(9개) → 공지 언어 코드(7개). 없는 언어는 영어로. */
export function noticeLangOf(uiLanguage: string): string {
  const supported = ['ko', 'en', 'ja', 'zh', 'de', 'fr', 'vi']
  return supported.includes(uiLanguage) ? uiLanguage : 'en'
}
