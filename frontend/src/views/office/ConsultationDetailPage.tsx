'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useI18n } from '../../i18n/I18nProvider'
import { useAuthStore } from '../../store/authStore'
import {
  useAssignConsultation,
  useCloseConsultation,
  useMyConsultation,
  useMyOffice,
  useResolveConsultation,
  useStaffConsultation,
  useTranscript,
} from '../../hooks/useOffice'
import { Button, Card, EmptyState, LinkButton, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'
import { ConsultationStatusBadge, Transcript, formatDateTime } from './consultationUi'
import { noticeLangOf } from './noticeUi'

/**
 * 상담 상세. staff=true 면 담당자 받은함에서 연 것(배정·해결 버튼, 한국어 번역), 아니면 학생 화면(종료 버튼, 내 언어 번역).
 * 실시간 대화는 기존 채팅방(/chat/[id])에서 하고, 여기서는 번역이 붙은 대화를 15초마다 다시 읽는다.
 */
export default function ConsultationDetailPage({ staff }: { staff: boolean }) {
  const { t, language } = useI18n()
  const { currentUser } = useAuthStore()
  const params = useParams<{ id: string }>()
  const id = Number(params?.id)
  const validId = Number.isInteger(id) && id > 0

  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = staff ? (office?.role ? office.officeId : undefined) : office && office.verifiedStudent ? office.officeId : undefined

  const staffQuery = useStaffConsultation(staff ? officeId : undefined, validId ? id : undefined)
  const mineQuery = useMyConsultation(staff ? undefined : officeId, validId ? id : undefined)
  const consultation = staff ? staffQuery.data : mineQuery.data
  const detailLoading = staff ? staffQuery.isLoading : mineQuery.isLoading
  const detailError = staff ? staffQuery.isError : mineQuery.isError
  const refetchDetail = staff ? staffQuery.refetch : mineQuery.refetch

  const lang = staff ? 'ko' : noticeLangOf(language)
  const active = !!consultation && consultation.status !== 'CLOSED'
  const transcript = useTranscript(officeId, validId ? id : undefined, lang, staff, active)

  const assign = useAssignConsultation(officeId)
  const resolve = useResolveConsultation(officeId)
  const close = useCloseConsultation(officeId)

  if (myOffice.isLoading || detailLoading) return <SkeletonLines count={6} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || officeId === undefined) return <EmptyState title={staff ? t('office.staffOnly') : t('office.none')} />
  if (detailError) return <QueryErrorNotice onRetry={() => refetchDetail()} />
  if (!consultation) return <EmptyState title={t('consult.none')} />

  const c = consultation
  const busy = assign.isPending || resolve.isPending || close.isPending
  const backHref = staff ? '/office/consultations' : '/my-university/consultations'

  return (
    <div style={{ maxWidth: 820, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={<Link href={backHref}>← {staff ? t('office.consultations.title') : t('consult.mine')}</Link>}
        title={t('consult.detail')}
        description={
          <span style={{ display: 'inline-flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <ConsultationStatusBadge status={c.status} />
            <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{formatDateTime(c.openedAt)}</span>
            <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
              {c.assignedStaffNickname ? t('consult.assigned', { name: c.assignedStaffNickname }) : t('consult.unassigned')}
            </span>
            {staff && (
              <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
                {t('office.consultations.firstReply')}: {c.firstStaffReplyAt ? formatDateTime(c.firstStaffReplyAt) : t('office.consultations.noReply')}
              </span>
            )}
          </span>
        }
        actions={
          <>
            {active && <LinkButton href={`/chat/${c.chatRoomId}`} variant="primary">{t('consult.chatRoom')}</LinkButton>}
            {staff && active && c.status === 'OPEN' && (
              <Button variant="secondary" disabled={busy} onClick={() => assign.mutate(c.id)}>{t('office.consultations.assign')}</Button>
            )}
            {staff && active && c.status !== 'RESOLVED' && (
              <Button variant="secondary" disabled={busy} onClick={() => resolve.mutate(c.id)}>{t('office.consultations.resolve')}</Button>
            )}
            {!staff && active && (
              <Button variant="danger" disabled={busy} onClick={() => close.mutate(c.id)}>{t('consult.close')}</Button>
            )}
          </>
        }
      />

      <Card>
        <div style={{ fontSize: 12, color: 'var(--color-text-muted)', marginBottom: 4 }}>{t('ask.title')} · {c.lang.toLowerCase()}</div>
        <div style={{ fontWeight: 600, marginBottom: c.answerText ? 8 : 0 }}>{c.question}</div>
        {c.answerText && (
          <p style={{ margin: 0, fontSize: 14, whiteSpace: 'pre-wrap', lineHeight: 1.6, color: 'var(--color-text-secondary)' }}>{c.answerText}</p>
        )}
      </Card>

      {!active && <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{t('consult.closed')}</p>}

      <section style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <h3 style={{ margin: 0, fontSize: 16 }}>{t('consult.transcript')}</h3>
        {transcript.isLoading && <SkeletonLines count={3} />}
        {transcript.isError && <QueryErrorNotice onRetry={() => transcript.refetch()} />}
        {transcript.data && <Transcript messages={transcript.data} viewerId={currentUser ? Number(currentUser.id) : null} />}
      </section>
    </div>
  )
}
