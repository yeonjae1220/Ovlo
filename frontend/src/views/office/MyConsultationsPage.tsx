'use client'

import { formatDateTime } from './officeFormatting'
import Link from 'next/link'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import { useOwnConsultations } from '../../hooks/useOffice'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'
import { ConsultationStatusBadge } from './consultationUi'

export default function MyConsultationsPage() {
  const { t } = useI18n()
  const router = useRouter()
  const [page, setPage] = useState(0)
  const list = useOwnConsultations(page)
  if (list.isLoading) return <SkeletonLines count={4} />
  if (list.isError) return <QueryErrorNotice onRetry={() => list.refetch()} />

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <PageHeader eyebrow={<Link href="/my-university">← {t('office.title')}</Link>} title={t('consult.mine')} />
      {list.isLoading && <SkeletonLines count={3} />}
      {list.data && list.data.content.length === 0 && <EmptyState title={t('consult.none')} action={<Link href="/my-university/ask">{t('ask.link')}</Link>} />}
      {list.data?.content.map((c) => (
        <Card key={c.id} interactive onClick={() => router.push(`/my-university/consultations/${c.id}`)}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6, fontSize: 12, color: 'var(--color-text-muted)' }}>
            <ConsultationStatusBadge status={c.status} />
            <span>{formatDateTime(c.openedAt)}</span>
            <span>{c.assignedStaffNickname ? t('consult.assigned', { name: c.assignedStaffNickname }) : t('consult.unassigned')}</span>
          </div>
          <div style={{ fontWeight: 600 }}>{c.question}</div>
        </Card>
      ))}
      {list.data && list.data.totalPages > 1 && <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
        <Button variant="ghost" disabled={page === 0} onClick={() => setPage(p => p - 1)}>‹</Button>
        <span>{page + 1} / {list.data.totalPages}</span>
        <Button variant="ghost" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(p => p + 1)}>›</Button>
      </div>}
    </div>
  )
}
