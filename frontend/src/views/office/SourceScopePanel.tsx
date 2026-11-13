 'use client'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { officeApi, type SourceScope } from '../../api/office'
import { useI18n } from '../../i18n/I18nProvider'
import { Button, FieldGroup, TextField } from '../../components/ui'
export default function SourceScopePanel({ officeId }: { officeId: number }) {
  const { t } = useI18n(); const qc = useQueryClient()
  const [department, setDepartment] = useState(''); const [url, setUrl] = useState('')
  const scopes = useQuery({ queryKey: ['office', officeId, 'source-scopes'], queryFn: () => officeApi.sourceScopes(officeId) })
  const save = useMutation({ mutationFn: (s: SourceScope) => officeApi.saveSourceScope(officeId, s), onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId] }) })
  return <details><summary>{t('office.knowledge.manageScopes')}</summary>
    {scopes.data?.map(s => <p key={s.id}>{s.department} · {s.url} <Button disabled={save.isPending} onClick={() => save.mutate({ ...s, enabled: !s.enabled })}>{t(s.enabled ? 'office.knowledge.disable' : 'office.knowledge.enable')}</Button></p>)}
    <FieldGroup label={t('office.knowledge.department')}><TextField value={department} onChange={e => setDepartment(e.target.value)} /></FieldGroup>
    <FieldGroup label={t('office.knowledge.scopeUrl')}><TextField type="url" value={url} onChange={e => setUrl(e.target.value)} /></FieldGroup>
    <Button disabled={save.isPending || !department.trim() || !/^https?:\/\//i.test(url)} onClick={() => save.mutate({ id: null, department, url, enabled: true, revision: 0 })}>{t('office.knowledge.addScope')}</Button>
    {(save.isError || scopes.isError) && <p role="alert">{t('office.knowledge.actionError')}</p>}
  </details>
}
