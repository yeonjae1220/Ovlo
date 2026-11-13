 'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { officeApi, type Notice, type NoticeSource, type NoticeSourceType } from '../../api/office'
import { Button, Card, FieldGroup, TextField, TextAreaField, SelectField, QueryErrorNotice } from '../../components/ui'
import { useI18n } from '../../i18n/I18nProvider'
import SourceScopePanel from './SourceScopePanel'

export default function NoticeSourcePanel({ officeId, notice, admin }: { officeId: number; notice: Notice; admin: boolean }) {
  const { t } = useI18n()
  const qc = useQueryClient()
  const [source, setSource] = useState<NoticeSource>(notice.source ?? {
    type: 'OFFICE_MATERIAL', department: '', reference: '', audience: '', campus: '', acquiredOn: '', publishedOn: null,
    ownerMemberId: 0, critical: false, publicSource: true, scopeId: null, basisNoticeIds: [],
  })
  const [evidence, setEvidence] = useState('')
  const [outcome, setOutcome] = useState('VERIFIED')
  const [checks, setChecks] = useState({ official: false, scopeChecked: false, privacyChecked: false, generalizable: false })
  const scopes = useQuery({ queryKey: ['office', officeId, 'source-scopes'], queryFn: () => officeApi.sourceScopes(officeId) })
  const staff = useQuery({ queryKey: ['office', officeId, 'staff'], queryFn: () => officeApi.listStaff(officeId) })
  const bases = useQuery({ queryKey: ['office', officeId, 'review-queue'], queryFn: () => officeApi.reviewQueue(officeId) })
  const history = useQuery({ queryKey: ['office', officeId, 'notice-history', notice.id], queryFn: () => officeApi.noticeHistory(officeId, notice.id) })
  const refresh = () => qc.invalidateQueries({ queryKey: ['office', officeId] })
  const save = useMutation({ mutationFn: () => officeApi.configureSource(officeId, notice.id, notice.revision,
    source), onSuccess: refresh })
  const review = useMutation({ mutationFn: () => officeApi.reviewSource(officeId, notice.id,
    { revision: notice.revision, outcome, evidence, ...checks }), onSuccess: refresh })
  const frozen = notice.status === 'ARCHIVED'
  const busy = save.isPending || review.isPending
  const set = <K extends keyof NoticeSource>(key: K, value: NoticeSource[K]) => setSource(s => ({ ...s, [key]: value }))
  return <Card>
    <h2 style={{ fontSize: 18 }}>{t('office.knowledge.title')}</h2>
    <p>{t('office.knowledge.lastVerified')}: {notice.review?.verifiedOn ?? '—'} · {t('office.knowledge.nextReview')}: {notice.review?.nextReviewOn ?? '—'}</p>
    {notice.review?.holdReason && <p role="status">{notice.review.holdReason}</p>}
    {admin && <SourceScopePanel officeId={officeId} />}
    {(scopes.isError || staff.isError || history.isError || bases.isError) && <QueryErrorNotice onRetry={() => { void scopes.refetch(); void staff.refetch(); void history.refetch(); void bases.refetch() }} />}
    <fieldset disabled={frozen || busy} style={{ border: 0, padding: 0, display: 'grid', gap: 12 }}>
      <FieldGroup label={t('office.knowledge.type')}><SelectField value={source.type} onChange={e => set('type', e.target.value as NoticeSourceType)}>
        {(['OFFICE_MATERIAL', 'UNIVERSITY_WEB', 'REVIEWED_STAFF_QA'] as const).map(k => <option key={k} value={k}>{t(`office.knowledge.${k}`)}</option>)}
      </SelectField></FieldGroup>
      {(['department', 'reference', 'audience', 'campus'] as const).map(k => <FieldGroup key={k} label={t(`office.knowledge.${k}`)}>
        <TextField value={source[k]} maxLength={k === 'reference' ? 1000 : 200} onChange={e => set(k, e.target.value)} />
      </FieldGroup>)}
      <FieldGroup label={t('office.knowledge.acquiredOn')}><TextField type="date" value={source.acquiredOn} onChange={e => set('acquiredOn', e.target.value)} /></FieldGroup>
      <FieldGroup label={t('office.knowledge.publishedOn')}><TextField type="date" value={source.publishedOn ?? ''} onChange={e => set('publishedOn', e.target.value || null)} /></FieldGroup>
      <FieldGroup label={t('office.knowledge.owner')}><SelectField value={source.ownerMemberId} onChange={e => set('ownerMemberId', Number(e.target.value))}>
        <option value={0}>{t('office.knowledge.select')}</option>
        {staff.data?.filter(s => s.status === 'ACTIVE').map(s => <option key={s.memberId} value={s.memberId}>{s.nickname}</option>)}
      </SelectField></FieldGroup>
      <label><input type="checkbox" checked={source.critical} onChange={e => set('critical', e.target.checked)} /> {t('office.knowledge.critical')}</label>
      <label><input type="checkbox" checked={source.publicSource} onChange={e => set('publicSource', e.target.checked)} /> {t('office.knowledge.publicSource')}</label>
      {source.type === 'UNIVERSITY_WEB' && <FieldGroup label={t('office.knowledge.scope')}><SelectField value={source.scopeId ?? ''} onChange={e => set('scopeId', Number(e.target.value) || null)}>
        <option value="">{t('office.knowledge.select')}</option>
        {scopes.data?.filter(s => s.enabled).map(s => <option key={s.id} value={s.id ?? ''}>{s.department} · {s.url}</option>)}
      </SelectField></FieldGroup>}
      <div role="group" aria-label={t('office.knowledge.basis')}><p>{t('office.knowledge.basis')}</p>
        {bases.data?.filter(n => n.id !== notice.id && (n.canBeBasis || source.basisNoticeIds.includes(n.id))).map(n => <label key={n.id} style={{ display: 'block' }}>
          <input type="checkbox" checked={source.basisNoticeIds.includes(n.id)} onChange={e => set('basisNoticeIds', e.target.checked ? [...source.basisNoticeIds, n.id] : source.basisNoticeIds.filter(id => id !== n.id))} /> {n.title} · {t(`office.knowledge.${n.state}`)}
        </label>)}
      </div>
      <Button onClick={() => save.mutate()} disabled={!source.department || !source.reference || !source.audience || !source.campus || !source.acquiredOn || !source.ownerMemberId}>{t('office.knowledge.saveSource')}</Button>
      <hr />
      <FieldGroup label={t('office.knowledge.outcome')}><SelectField value={outcome} onChange={e => setOutcome(e.target.value)}>
        {['VERIFIED', 'FAILED', 'CHANGED', 'WITHDRAWN', 'CONFLICT'].map(k => <option key={k} value={k}>{t(`office.knowledge.${k}`)}</option>)}
      </SelectField></FieldGroup>
      {outcome === 'VERIFIED' && (Object.keys(checks) as (keyof typeof checks)[]).map(k => <label key={k}>
        <input type="checkbox" checked={checks[k]} onChange={e => setChecks(c => ({ ...c, [k]: e.target.checked }))} /> {t(`office.knowledge.${k}`)}
      </label>)}
      <FieldGroup label={t('office.knowledge.evidence')}><TextAreaField value={evidence} maxLength={2000} onChange={e => setEvidence(e.target.value)} /></FieldGroup>
      <Button onClick={() => review.mutate()} disabled={!evidence.trim() || !notice.source}>{t('office.knowledge.recordReview')}</Button>
    </fieldset>
    {(save.isError || review.isError) && <p role="alert">{t('office.knowledge.actionError')}</p>}
    <details><summary>{t('office.knowledge.history')}</summary>
      {history.data?.map(a => <div key={a.id}><strong>{a.action} · v{a.version}</strong> · {a.createdAt}<p>{a.evidence}</p>
        <details><summary>{t('office.knowledge.snapshot')}</summary><pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{a.snapshot}</pre></details>
      </div>)}
    </details>
  </Card>
}
