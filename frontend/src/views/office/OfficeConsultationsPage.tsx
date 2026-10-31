'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import { useAssignConsultation, useMyOffice, useOfficeConsultations } from '../../hooks/useOffice'
import type { ConsultationStatus } from '../../api/office'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SelectField, SkeletonLines } from '../../components/ui'
import { ConsultationStatusBadge, formatDateTime } from './consultationUi'

const STATUSES: ConsultationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export default function OfficeConsultationsPage() {
  const { t } = useI18n()
  const router = useRouter()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined
  const [status, setStatus] = useState<ConsultationStatus | ''>('')
  const [page, setPage] = useState(0)
  const list = useOfficeConsultations(officeId, status, page)
  const assign = useAssignConsultation(officeId)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) return <EmptyState title={t('office.staffOnly')} />

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={<Link href="/office/notices">← {t('office.notices.title')}</Link>}
        title={t('office.consultations.title')}
        description={t('office.consultations.subtitle')}
      />
      <SelectField value={status} onChange={(e) => { setStatus(e.target.value as ConsultationStatus | ''); setPage(0) }}>
        <option value="">{t('office.notices.all')}</option>
        {STATUSES.map((s) => (
          <option key={s} value={s}>{t(`consult.status.${s}`)}</option>
        ))}
      </SelectField>

      {list.isLoading && <SkeletonLines count={5} />}
      {list.isError && <QueryErrorNotice onRetry={() => list.refetch()} />}
      {list.data && list.data.content.length === 0 && <EmptyState title={t('office.consultations.empty')} />}
      {list.data?.content.map((c) => (
        <Card key={c.id} interactive onClick={() => router.push(`/office/consultations/${c.id}`)}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6, fontSize: 12, color: 'var(--color-text-muted)' }}>
            <ConsultationStatusBadge status={c.status} />
            <span>{c.lang.toLowerCase()}</span>
            <span>{formatDateTime(c.openedAt)}</span>
            <span>{c.assignedStaffNickname ? t('consult.assigned', { name: c.assignedStaffNickname }) : t('consult.unassigned')}</span>
            {c.status === 'OPEN' && (
              <Button
                variant="primary"
                style={{ marginLeft: 'auto' }}
                disabled={assign.isPending}
                onClick={(e) => { e.stopPropagation(); assign.mutate(c.id) }}
              >
                {t('office.consultations.assign')}
              </Button>
            )}
          </div>
          <div style={{ fontWeight: 600 }}>{c.question}</div>
        </Card>
      ))}

      {list.data && list.data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>‹</Button>
          <span style={{ alignSelf: 'center', fontSize: 13 }}>{page + 1} / {list.data.totalPages}</span>
          <Button variant="ghost" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage((p) => p + 1)}>›</Button>
        </div>
      )}
    </div>
  )
}
