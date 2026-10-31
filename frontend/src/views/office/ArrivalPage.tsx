'use client'

import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import { useAuthStore } from '../../store/authStore'
import { useClickPartner, useMyOffice, useStudentPartners } from '../../hooks/useOffice'
import { Badge, Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'

/**
 * 학생용 "입국 전 준비": 국제처가 켠 분류의 정착 제휴 카드.
 * 설계 §6.3 — 카드마다 "제휴 · Ovlo가 수수료를 받을 수 있습니다" 를 항상 붙인다. 공지·답변 안에는 넣지 않는다.
 */
export default function ArrivalPage() {
  const { t } = useI18n()
  const { currentUser } = useAuthStore()
  const profileHref = currentUser ? `/profile/${currentUser.id}` : '/login'
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office && office.verifiedStudent ? office.officeId : undefined
  const partners = useStudentPartners(officeId)
  const click = useClickPartner(officeId)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office) {
    return (
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        <PageHeader title={t('student.arrival.title')} />
        <EmptyState title={t('office.none')} action={<Link href={profileHref}>{t('nav.profile')}</Link>} />
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={office.officeName}
        title={t('student.arrival.title')}
        description={t('student.arrival.subtitle')}
        actions={<Link href="/my-university">{t('student.notices.title')}</Link>}
      />

      {!office.verifiedStudent ? (
        <EmptyState title={t('student.notices.notVerified')} action={<Link href={profileHref}>{t('nav.profile')}</Link>} />
      ) : (
        <>
          {partners.isLoading && <SkeletonLines count={3} />}
          {partners.isError && <QueryErrorNotice onRetry={() => partners.refetch()} />}
          {partners.data && partners.data.length === 0 && <EmptyState title={t('student.arrival.empty')} />}
          {partners.data?.map((p) => (
            <Card key={p.id}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 8 }}>
                <Badge tone="neutral">{t(`partner.category.${p.category}`)}</Badge>
                <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{t('student.arrival.disclosure')}</span>
              </div>
              <h3 style={{ margin: '0 0 6px', fontSize: 17 }}>{p.name}</h3>
              {p.description && <p style={{ margin: '0 0 12px', lineHeight: 1.6 }}>{p.description}</p>}
              <Button variant="secondary" disabled={click.isPending} onClick={() => click.mutate(p.id)}>
                {t('student.arrival.open')}
              </Button>
              {click.isError && <p style={{ margin: '8px 0 0', fontSize: 12, color: 'var(--color-danger)' }}>{t('error.unexpected')}</p>}
            </Card>
          ))}
        </>
      )}
    </div>
  )
}
