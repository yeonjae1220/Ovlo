'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import { useAuthStore } from '../../store/authStore'
import { useAcceptOfficeInvitation, useMyOffice, useStudentNotices } from '../../hooks/useOffice'
import { NOTICE_CATEGORIES, type NoticeCategory } from '../../api/office'
import { Badge, Button, Card, EmptyState, PageHeader, QueryErrorNotice, SelectField, SkeletonLines } from '../../components/ui'
import { CategoryBadge, formatDate, noticeLangOf } from './noticeUi'

/**
 * 학생용 "내 학교": 소속 국제처의 승인 공지를 내 언어로 보여 준다.
 * 담당자 초대가 있으면 수락 버튼, 담당자면 담당자 화면 링크를 함께 보여 준다.
 */
export default function MyUniversityPage() {
  const { t, language } = useI18n()
  const { currentUser } = useAuthStore()
  const profileHref = currentUser ? `/profile/${currentUser.id}` : '/login'
  const myOffice = useMyOffice()
  const accept = useAcceptOfficeInvitation()
  const [category, setCategory] = useState<NoticeCategory | ''>('')

  const office = myOffice.data ?? null
  const readableOfficeId = office && office.verifiedStudent ? office.officeId : undefined
  const notices = useStudentNotices(readableOfficeId, noticeLangOf(language), category)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />

  if (!office) {
    return (
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        <PageHeader title={t('office.title')} />
        <EmptyState title={t('office.none')} action={<Link href={profileHref}>{t('nav.profile')}</Link>} />
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={office.officeName}
        title={t('student.notices.title')}
        description={t('student.notices.subtitle', { office: office.officeName })}
        actions={
          <span style={{ display: 'inline-flex', gap: 12 }}>
            {office.verifiedStudent && <Link href="/my-university/ask">{t('ask.link')}</Link>}
            {office.verifiedStudent && <Link href="/my-university/consultations">{t('consult.mine')}</Link>}
            {office.role && <Link href="/office/notices">{t('office.staffLink')}</Link>}
          </span>
        }
      />

      {office.pendingInvitation && (
        <Card>
          <p style={{ margin: '0 0 10px' }}>{t('office.pending')}</p>
          {accept.isSuccess ? (
            <p style={{ margin: 0, color: 'var(--color-text-secondary)' }}>{t('office.accepted')}</p>
          ) : (
            <Button variant="primary" onClick={() => accept.mutate()} disabled={accept.isPending}>
              {t('office.accept')}
            </Button>
          )}
        </Card>
      )}

      {!office.verifiedStudent ? (
        <EmptyState title={t('student.notices.notVerified')} action={<Link href={profileHref}>{t('nav.profile')}</Link>} />
      ) : (
        <>
          <SelectField value={category} onChange={(e) => setCategory(e.target.value as NoticeCategory | '')}>
            <option value="">{t('office.notices.all')}</option>
            {NOTICE_CATEGORIES.map((c) => (
              <option key={c} value={c}>{t(`office.category.${c}`)}</option>
            ))}
          </SelectField>

          {notices.isLoading && <SkeletonLines count={4} />}
          {notices.isError && <QueryErrorNotice onRetry={() => notices.refetch()} />}
          {notices.data && notices.data.length === 0 && <EmptyState title={t('student.notices.empty')} />}
          {notices.data?.map((n) => (
            <Card key={n.id}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 8 }}>
                <CategoryBadge category={n.category} />
                <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
                  {t('student.notices.validUntil', { date: formatDate(n.expiresAt) })}
                </span>
                {n.machineTranslated && <Badge tone="warning">{t('office.notice.machine')}</Badge>}
              </div>
              <h3 style={{ margin: '0 0 8px', fontSize: 17 }}>{n.title}</h3>
              <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>{n.body}</p>
              {n.machineTranslated && (
                <p style={{ margin: '10px 0 0', fontSize: 12, color: 'var(--color-text-muted)' }}>{t('student.notices.machine')}</p>
              )}
              {n.sourceUrl && (
                <p style={{ margin: '10px 0 0', fontSize: 13 }}>
                  <a href={n.sourceUrl} target="_blank" rel="noreferrer">{t('student.notices.original')}</a>
                </p>
              )}
            </Card>
          ))}
        </>
      )}
    </div>
  )
}
