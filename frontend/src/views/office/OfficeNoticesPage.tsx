'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import { useMyOffice, useOfficeNotices, useRegisterNotice } from '../../hooks/useOffice'
import { NOTICE_CATEGORIES, type NoticeCategory, type NoticeStatus } from '../../api/office'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SelectField, SkeletonLines } from '../../components/ui'
import NoticeForm from './NoticeForm'
import { CategoryBadge, StatusBadge, formatDate } from './noticeUi'

const STATUSES: NoticeStatus[] = ['DRAFT', 'APPROVED', 'EXPIRED', 'ARCHIVED']

/** 담당자 승인 자료 목록. 담당자 클레임이 없으면(role null) 안내만 보여 준다. */
export default function OfficeNoticesPage() {
  const { t } = useI18n()
  const router = useRouter()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined

  const [status, setStatus] = useState<NoticeStatus | ''>('')
  const [category, setCategory] = useState<NoticeCategory | ''>('')
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)

  const notices = useOfficeNotices(officeId, status, category, page)
  const register = useRegisterNotice(officeId)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) {
    return (
      <div style={{ maxWidth: 900, margin: '0 auto' }}>
        <PageHeader title={t('office.notices.title')} />
        <EmptyState title={t('office.staffOnly')} action={<Link href="/my-university">{t('nav.myUniversity')}</Link>} />
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={office.officeName}
        title={t('office.notices.title')}
        description={t('office.notices.subtitle')}
        actions={
          <Button variant="primary" icon="+" onClick={() => setShowForm((v) => !v)}>
            {t('office.notices.new')}
          </Button>
        }
      />

      {showForm && (
        <Card>
          <NoticeForm
            pending={register.isPending}
            onCancel={() => setShowForm(false)}
            onSubmit={(req) =>
              register.mutate(req, {
                onSuccess: (created) => {
                  setShowForm(false)
                  router.push(`/office/notices/${created.id}`)
                },
              })
            }
          />
        </Card>
      )}

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <SelectField value={status} onChange={(e) => { setStatus(e.target.value as NoticeStatus | ''); setPage(0) }}>
          <option value="">{t('office.notices.filter.status')}: {t('office.notices.all')}</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{t(`office.status.${s}`)}</option>
          ))}
        </SelectField>
        <SelectField value={category} onChange={(e) => { setCategory(e.target.value as NoticeCategory | ''); setPage(0) }}>
          <option value="">{t('office.notices.filter.category')}: {t('office.notices.all')}</option>
          {NOTICE_CATEGORIES.map((c) => (
            <option key={c} value={c}>{t(`office.category.${c}`)}</option>
          ))}
        </SelectField>
      </div>

      {notices.isLoading && <SkeletonLines count={5} />}
      {notices.isError && <QueryErrorNotice onRetry={() => notices.refetch()} />}
      {notices.data && notices.data.content.length === 0 && <EmptyState title={t('office.notices.empty')} />}
      {notices.data?.content.map((n) => (
        <Card key={n.id} interactive onClick={() => router.push(`/office/notices/${n.id}`)}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 6 }}>
            <StatusBadge status={n.status} />
            <CategoryBadge category={n.category} />
            <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
              {n.term} · {formatDate(n.effectiveFrom)} – {formatDate(n.expiresAt)} · {t('office.notice.version', { version: n.version })}
            </span>
          </div>
          <div style={{ fontWeight: 600 }}>{n.title}</div>
          <div style={{ fontSize: 12, color: n.missingLangCount > 0 ? 'var(--color-warm)' : 'var(--color-text-muted)', marginTop: 4 }}>
            {t('office.notice.translations')}: {n.translatedLangCount}/{n.translatedLangCount + n.missingLangCount}
          </div>
        </Card>
      ))}

      {notices.data && notices.data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>‹</Button>
          <span style={{ alignSelf: 'center', fontSize: 13 }}>{page + 1} / {notices.data.totalPages}</span>
          <Button variant="ghost" disabled={page + 1 >= notices.data.totalPages} onClick={() => setPage((p) => p + 1)}>›</Button>
        </div>
      )}
    </div>
  )
}
