'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import { useExportCsv, useMyOffice, useOfficeDashboard } from '../../hooks/useOffice'
import { PARTNER_CATEGORIES, type ExportKind, type RepeatedQuestion } from '../../api/office'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'
import { formatDate } from './noticeUi'

const EXPORT_KINDS: ExportKind[] = ['inquiries', 'consultations', 'notices']

function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

function percent(rate: number | null, na: string): string {
  return rate === null || !Number.isFinite(rate) ? na : `${Math.round(rate * 100)}%`
}

/** 담당자 첫 화면: §5.4 지표. 비율이 null 이면 "해당 없음" — 0% 로 보여 오독하지 않게. */
export default function OfficeDashboardPage() {
  const { t } = useI18n()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined

  const [from, setFrom] = useState(() => isoDaysAgo(29))
  const [to, setTo] = useState(() => isoDaysAgo(0))
  const dashboard = useOfficeDashboard(officeId, from, to)
  const exportCsv = useExportCsv(officeId)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) {
    return (
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <PageHeader title={t('office.dashboard.title')} />
        <EmptyState title={t('office.staffOnly')} action={<Link href="/my-university">{t('nav.myUniversity')}</Link>} />
      </div>
    )
  }

  const d = dashboard.data
  const na = t('office.dashboard.na')
  const inputStyle = {
    padding: '6px 8px',
    borderRadius: 6,
    border: '1px solid var(--color-border)',
    background: 'var(--color-surface)',
    color: 'var(--color-text)',
  } as const

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={office.officeName}
        title={t('office.dashboard.title')}
        description={t('office.dashboard.subtitle')}
        actions={
          <span style={{ display: 'inline-flex', gap: 12, flexWrap: 'wrap' }}>
            <Link href="/office/notices">{t('office.notices.title')}</Link>
            <Link href="/office/inquiries">{t('office.inquiries.link')}</Link>
            <Link href="/office/consultations">{t('office.consultations.link')}</Link>
            {office.role === 'OFFICE_ADMIN' && <Link href="/office/settings">{t('office.settings.link')}</Link>}
          </span>
        }
      />

      <div style={{ display: 'flex', gap: 12, alignItems: 'end', flexWrap: 'wrap' }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 12 }}>
          {t('office.dashboard.from')}
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 12 }}>
          {t('office.dashboard.to')}
          <input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} style={inputStyle} />
        </label>
        <span style={{ flex: 1 }} />
        <span style={{ display: 'inline-flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.dashboard.export')}</span>
          {EXPORT_KINDS.map((kind) => (
            <Button
              key={kind}
              variant="secondary"
              disabled={exportCsv.isPending}
              onClick={() => exportCsv.mutate({ kind, from: kind === 'inquiries' ? from : undefined, to: kind === 'inquiries' ? to : undefined })}
            >
              {t(`office.dashboard.export.${kind}`)}
            </Button>
          ))}
        </span>
      </div>
      <p style={{ margin: '-8px 0 0', fontSize: 12, color: 'var(--color-text-muted)', textAlign: 'right' }}>
        {t('office.dashboard.exportHint')}
      </p>

      {dashboard.isLoading && <SkeletonLines count={6} />}
      {dashboard.isError && <QueryErrorNotice onRetry={() => dashboard.refetch()} />}

      {d && (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
            <Stat label={t('office.dashboard.inquiries')} value={String(d.inquiries.total)}
              sub={`${t('office.outcome.ANSWERED')} ${d.inquiries.answered} · ${t('office.outcome.NO_GROUNDS')} ${d.inquiries.noGrounds} · ${t('office.outcome.ESCALATED')} ${d.inquiries.escalated}`} />
            <Stat label={t('office.dashboard.answerRate')} value={percent(d.inquiries.answerRate, na)} />
            <Stat label={t('office.dashboard.helpfulRate')} value={percent(d.feedback.helpfulRate, na)}
              sub={`${d.feedback.helpful} / ${d.feedback.helpful + d.feedback.unhelpful}`} />
            <Stat label={t('office.dashboard.consultations')}
              value={String(d.consultations.open + d.consultations.inProgress + d.consultations.resolved + d.consultations.closed)}
              sub={`${t('consult.status.OPEN')} ${d.consultations.open} · ${t('consult.status.IN_PROGRESS')} ${d.consultations.inProgress} · ${t('consult.status.RESOLVED')} ${d.consultations.resolved}`} />
            <Stat label={t('office.dashboard.firstReply')}
              value={d.consultations.avgFirstReplyMinutes === null ? na : t('office.dashboard.minutes', { n: Math.round(d.consultations.avgFirstReplyMinutes) })} />
            <Stat label={t('office.dashboard.resolveHours')}
              value={d.consultations.avgResolveHours === null ? na : t('office.dashboard.hours', { n: Math.round(d.consultations.avgResolveHours * 10) / 10 })} />
            <Stat label={t('office.dashboard.notices')} value={String(d.notices.approved)}
              sub={`${t('office.dashboard.approved')} ${d.notices.approved} · ${t('office.dashboard.draft')} ${d.notices.draft}`} />
          </div>

          <Card>
            <h3 style={{ margin: '0 0 8px', fontSize: 15 }}>{t('office.dashboard.noGrounds')}</h3>
            <p style={{ margin: '0 0 10px', fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.dashboard.noGroundsHint')}</p>
            <QuestionList items={d.noGroundsQuestions} emptyText={t('office.dashboard.repeatedNone')} />
          </Card>

          <Card>
            <h3 style={{ margin: '0 0 10px', fontSize: 15 }}>{t('office.dashboard.repeated')}</h3>
            <QuestionList items={d.repeatedQuestions} emptyText={t('office.dashboard.repeatedNone')} />
          </Card>

          <Card>
            <h3 style={{ margin: '0 0 10px', fontSize: 15 }}>{t('office.dashboard.expiring')}</h3>
            {d.notices.expiringSoon.length === 0 ? (
              <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{t('office.dashboard.expiringNone')}</p>
            ) : (
              <ul style={{ margin: 0, paddingLeft: 18, lineHeight: 1.8 }}>
                {d.notices.expiringSoon.map((n) => (
                  <li key={n.id}>
                    <Link href={`/office/notices/${n.id}`}>{n.title}</Link>
                    <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--color-text-muted)' }}>{formatDate(n.expiresAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <h3 style={{ margin: '0 0 10px', fontSize: 15 }}>{t('office.dashboard.partnerClicks')}</h3>
            {PARTNER_CATEGORIES.every((c) => !d.partnerClicks[c]) ? (
              <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{t('office.dashboard.partnerClicksNone')}</p>
            ) : (
              <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                {PARTNER_CATEGORIES.map((c) => (
                  <span key={c} style={{ fontSize: 14 }}>
                    {t(`partner.category.${c}`)} <strong>{d.partnerClicks[c] ?? 0}</strong>
                  </span>
                ))}
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  )
}

function Stat({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <Card>
      <div style={{ fontSize: 12, color: 'var(--color-text-muted)', marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 26, fontWeight: 700, lineHeight: 1.2 }}>{value}</div>
      {sub && <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 6 }}>{sub}</div>}
    </Card>
  )
}

function QuestionList({ items, emptyText }: { items: RepeatedQuestion[]; emptyText: string }) {
  const { t } = useI18n()
  if (items.length === 0) return <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{emptyText}</p>
  return (
    <ol style={{ margin: 0, paddingLeft: 20, lineHeight: 1.8 }}>
      {items.map((q) => (
        <li key={q.sample}>
          {q.sample}
          <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.dashboard.times', { n: q.count })}</span>
        </li>
      ))}
    </ol>
  )
}
