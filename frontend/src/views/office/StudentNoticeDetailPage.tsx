 'use client'
import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { officeApi } from '../../api/office'
import { useMyOffice } from '../../hooks/useOffice'
import { useI18n } from '../../i18n/I18nProvider'
import { Card, EmptyState, QueryErrorNotice, SkeletonLines } from '../../components/ui'
export default function StudentNoticeDetailPage() {
  const params = useParams<{ id: string }>(); const id = Number(params.id)
  const { t, language } = useI18n(); const office = useMyOffice()
  const notice = useQuery({ queryKey: ['office', office.data?.officeId, 'student-notice', id, language],
    queryFn: () => officeApi.getStudentNotice(office.data!.officeId, id, language), enabled: !!office.data?.verifiedStudent && Number.isSafeInteger(id) && id > 0 })
  if (office.isLoading || notice.isLoading) return <SkeletonLines count={4} />
  if (office.isError || notice.isError) return <QueryErrorNotice onRetry={() => { void office.refetch(); void notice.refetch() }} />
  if (!notice.data) return <EmptyState title={t('office.knowledge.outdated')} />
  const n = notice.data
  return <Card><h1>{n.title}</h1><p>{n.term} · {n.effectiveFrom} – {n.expiresAt}</p>
    {n.machineTranslated && <p>{t('office.notice.machine')}</p>}
    <p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{n.body}</p>
    {n.sourceUrl && /^https?:\/\//i.test(n.sourceUrl) && <a href={n.sourceUrl} target="_blank" rel="noreferrer">{t('office.notice.sourceUrl')}</a>}
  </Card>
}
