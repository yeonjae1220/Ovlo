'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import {
  useApproveNotice,
  useArchiveNotice,
  useMyOffice,
  useOfficeNotice,
  useRetranslateNotice,
  useReviewTranslation,
  useUpdateNotice,
} from '../../hooks/useOffice'
import type { NoticeRequest } from '../../api/office'
import { Badge, Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'
import NoticeForm from './NoticeForm'
import { CategoryBadge, StatusBadge, formatDate } from './noticeUi'

export default function OfficeNoticeDetailPage() {
  const { t } = useI18n()
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const noticeId = Number(params?.id)
  const validId = Number.isInteger(noticeId) && noticeId > 0

  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined
  const notice = useOfficeNotice(officeId, validId ? noticeId : undefined)

  const update = useUpdateNotice(officeId)
  const approve = useApproveNotice(officeId)
  const archive = useArchiveNotice(officeId)
  const review = useReviewTranslation(officeId)
  const retranslate = useRetranslateNotice(officeId)
  const [editing, setEditing] = useState(false)

  if (myOffice.isLoading || notice.isLoading) return <SkeletonLines count={6} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) return <EmptyState title={t('office.staffOnly')} />
  if (notice.isError) return <QueryErrorNotice onRetry={() => notice.refetch()} />
  if (!notice.data) return <EmptyState title={t('office.notices.empty')} />

  const n = notice.data
  const frozen = n.status === 'ARCHIVED'
  const canApprove = n.status === 'DRAFT' || n.status === 'EXPIRED'
  const initial: NoticeRequest = {
    category: n.category,
    title: n.title,
    body: n.body,
    sourceUrl: n.sourceUrl ?? '',
    attachmentMediaIds: n.attachmentMediaIds,
    term: n.term,
    effectiveFrom: n.effectiveFrom,
    expiresAt: n.expiresAt,
  }
  const busy = update.isPending || approve.isPending || archive.isPending || retranslate.isPending

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={<Link href="/office/notices">← {t('office.notice.back')}</Link>}
        title={n.title}
        description={
          <span style={{ display: 'inline-flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <StatusBadge status={n.status} />
            <CategoryBadge category={n.category} />
            {n.activeToday && <Badge tone="success">{t('office.notice.activeToday')}</Badge>}
            <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
              {n.term} · {formatDate(n.effectiveFrom)} – {formatDate(n.expiresAt)} · {t('office.notice.version', { version: n.version })}
            </span>
          </span>
        }
        actions={
          !frozen && (
            <>
              {canApprove && (
                <Button variant="primary" disabled={busy} onClick={() => approve.mutate(n.id)}>{t('office.notice.approve')}</Button>
              )}
              <Button variant="secondary" disabled={busy || editing} onClick={() => setEditing(true)}>{t('office.notice.save')}…</Button>
              <Button variant="danger" disabled={busy} onClick={() => archive.mutate(n.id, { onSuccess: () => router.push('/office/notices') })}>
                {t('office.notice.archive')}
              </Button>
            </>
          )
        }
      />

      {editing ? (
        <Card>
          <NoticeForm
            initial={initial}
            pending={update.isPending}
            onCancel={() => setEditing(false)}
            onSubmit={(req) => update.mutate({ noticeId: n.id, req }, { onSuccess: () => setEditing(false) })}
          />
        </Card>
      ) : (
        <Card>
          <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>{n.body}</p>
          {n.sourceUrl && (
            <p style={{ margin: '10px 0 0', fontSize: 13 }}>
              <a href={n.sourceUrl} target="_blank" rel="noreferrer">{n.sourceUrl}</a>
            </p>
          )}
        </Card>
      )}

      <section style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <h3 style={{ margin: 0, fontSize: 16 }}>{t('office.notice.translations')}</h3>
          {!frozen && n.missingLangs.length > 0 && (
            <Button variant="secondary" disabled={busy} onClick={() => retranslate.mutate(n.id)}>{t('office.notice.retranslate')}</Button>
          )}
        </div>
        {n.missingLangs.length > 0 && (
          <p style={{ margin: 0, fontSize: 13, color: 'var(--color-warm)' }}>
            {t('office.notice.missing', { langs: n.missingLangs.map((l) => l.toLowerCase()).join(', ') })}
          </p>
        )}
        {n.translations.map((tr) => (
          <Card key={tr.lang}>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6 }}>
              <Badge tone="neutral">{tr.lang.toLowerCase()}</Badge>
              {tr.stale ? (
                <Badge tone="danger">{t('office.notice.stale')}</Badge>
              ) : tr.status === 'REVIEWED' ? (
                <Badge tone="success">{t('office.notice.reviewed')}</Badge>
              ) : (
                <Badge tone="warning">{t('office.notice.machine')}</Badge>
              )}
              <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{formatDate(tr.generatedAt)}</span>
              {!frozen && !tr.stale && tr.status === 'MACHINE' && (
                <Button
                  variant="ghost"
                  disabled={review.isPending}
                  onClick={() => review.mutate({ noticeId: n.id, lang: tr.lang })}
                  style={{ marginLeft: 'auto' }}
                >
                  {t('office.notice.review')}
                </Button>
              )}
            </div>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>{tr.title}</div>
            <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: 14 }}>{tr.body}</p>
          </Card>
        ))}
      </section>
    </div>
  )
}
