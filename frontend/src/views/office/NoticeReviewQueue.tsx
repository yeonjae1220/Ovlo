 'use client'
import { useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { officeApi } from '../../api/office'
import { useI18n } from '../../i18n/I18nProvider'
import { Card, SelectField, QueryErrorNotice } from '../../components/ui'
export default function NoticeReviewQueue({ officeId }: { officeId: number }) {
  const { t } = useI18n(); const [state, setState] = useState('ALL')
  const q = useQuery({ queryKey: ['office', officeId, 'review-queue'], queryFn: () => officeApi.reviewQueue(officeId) })
  const rows = q.data?.filter(r => state === 'ALL' || r.state === state)
  return <Card><h2 style={{ fontSize: 18 }}>{t('office.knowledge.queue')}</h2>
    <SelectField value={state} onChange={e => setState(e.target.value)} aria-label={t('office.knowledge.queue')}>
      {['ALL', 'PENDING', 'HELD', 'OVERDUE', 'DUE_SOON', 'CURRENT'].map(k => <option key={k} value={k}>{t(`office.knowledge.${k}`)} ({k === 'ALL' ? q.data?.length ?? 0 : q.data?.filter(r => r.state === k).length ?? 0})</option>)}
    </SelectField>
    {q.isError && <QueryErrorNotice onRetry={() => q.refetch()} />}
    {rows?.length === 0 && <p>{t('office.knowledge.noTasks')}</p>}
    <ul>{rows?.map(r => <li key={r.id}><Link href={`/office/notices/${r.id}`}>{r.title}</Link> · {t(`office.knowledge.${r.state}`)} · {r.nextReviewOn ?? '—'}</li>)}</ul>
  </Card>
}
