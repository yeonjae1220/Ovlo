'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import { useMyOffice, useOfficeInquiries } from '../../hooks/useOffice'
import type { InquiryOutcome } from '../../api/office'
import { Badge, Button, Card, EmptyState, PageHeader, QueryErrorNotice, SelectField, SkeletonLines } from '../../components/ui'

const OUTCOMES: InquiryOutcome[] = ['ANSWERED', 'NO_GROUNDS', 'ESCALATED']

function OutcomeBadge({ outcome }: { outcome: InquiryOutcome }) {
  const { t } = useI18n()
  const tone = outcome === 'ANSWERED' ? 'success' : outcome === 'NO_GROUNDS' ? 'warning' : 'info'
  return <Badge tone={tone}>{t(`office.outcome.${outcome}`)}</Badge>
}

function formatDateTime(value: string): string {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d)
}

/** 담당자 문의 로그. 학생 식별 정보는 서버가 보내지 않는다. */
export default function OfficeInquiriesPage() {
  const { t } = useI18n()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined
  const [outcome, setOutcome] = useState<InquiryOutcome | ''>('')
  const [page, setPage] = useState(0)
  const inquiries = useOfficeInquiries(officeId, outcome, page)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) return <EmptyState title={t('office.staffOnly')} />

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={<Link href="/office/notices">← {t('office.notices.title')}</Link>}
        title={t('office.inquiries.title')}
        description={t('office.inquiries.subtitle')}
      />

      <SelectField value={outcome} onChange={(e) => { setOutcome(e.target.value as InquiryOutcome | ''); setPage(0) }}>
        <option value="">{t('office.inquiries.filter')}: {t('office.notices.all')}</option>
        {OUTCOMES.map((o) => (
          <option key={o} value={o}>{t(`office.outcome.${o}`)}</option>
        ))}
      </SelectField>

      {inquiries.isLoading && <SkeletonLines count={5} />}
      {inquiries.isError && <QueryErrorNotice onRetry={() => inquiries.refetch()} />}
      {inquiries.data && inquiries.data.content.length === 0 && <EmptyState title={t('office.inquiries.empty')} />}
      {inquiries.data?.content.map((q) => (
        <Card key={q.id}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6, fontSize: 12, color: 'var(--color-text-muted)' }}>
            <OutcomeBadge outcome={q.outcome} />
            <Badge tone="neutral">{q.lang.toLowerCase()}</Badge>
            <span>{formatDateTime(q.askedAt)}</span>
            <span>{q.latencyMs} ms</span>
            <span>{t('office.inquiries.cited')} {q.citedNoticeIds.length} / {t('office.inquiries.candidates')} {q.candidateCount}</span>
            {q.helpful !== null && <span>{q.helpful ? '👍' : '👎'}{q.feedbackReason ? ` ${q.feedbackReason}` : ''}</span>}
          </div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>{q.question}</div>
          {q.answerText && (
            <p style={{ margin: 0, fontSize: 14, whiteSpace: 'pre-wrap', lineHeight: 1.6, color: 'var(--color-text-secondary)' }}>{q.answerText}</p>
          )}
        </Card>
      ))}

      {inquiries.data && inquiries.data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>‹</Button>
          <span style={{ alignSelf: 'center', fontSize: 13 }}>{page + 1} / {inquiries.data.totalPages}</span>
          <Button variant="ghost" disabled={page + 1 >= inquiries.data.totalPages} onClick={() => setPage((p) => p + 1)}>›</Button>
        </div>
      )}
    </div>
  )
}
