'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import { useMyConsultations, useMyOffice } from '../../hooks/useOffice'
import { Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'
import { ConsultationStatusBadge, formatDateTime } from './consultationUi'

export default function MyConsultationsPage() {
  const { t } = useI18n()
  const router = useRouter()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office && office.verifiedStudent ? office.officeId : undefined
  const list = useMyConsultations(officeId)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office) return <EmptyState title={t('office.none')} />
  if (!office.verifiedStudent) return <EmptyState title={t('student.notices.notVerified')} />

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <PageHeader eyebrow={<Link href="/my-university">← {office.officeName}</Link>} title={t('consult.mine')} />
      {list.isLoading && <SkeletonLines count={3} />}
      {list.isError && <QueryErrorNotice onRetry={() => list.refetch()} />}
      {list.data && list.data.length === 0 && <EmptyState title={t('consult.none')} action={<Link href="/my-university/ask">{t('ask.link')}</Link>} />}
      {list.data?.map((c) => (
        <Card key={c.id} interactive onClick={() => router.push(`/my-university/consultations/${c.id}`)}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6, fontSize: 12, color: 'var(--color-text-muted)' }}>
            <ConsultationStatusBadge status={c.status} />
            <span>{formatDateTime(c.openedAt)}</span>
            <span>{c.assignedStaffNickname ? t('consult.assigned', { name: c.assignedStaffNickname }) : t('consult.unassigned')}</span>
          </div>
          <div style={{ fontWeight: 600 }}>{c.question}</div>
        </Card>
      ))}
    </div>
  )
}
