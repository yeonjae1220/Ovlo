'use client'

import { useState } from 'react'
import { useI18n } from '../../i18n/I18nProvider'
import { NOTICE_CATEGORIES, type NoticeCategory, type NoticeRequest } from '../../api/office'
import { Button, FieldGroup, SelectField, TextAreaField, TextField } from '../../components/ui'

const EMPTY: NoticeRequest = {
  category: 'HOUSING',
  title: '',
  body: '',
  sourceUrl: '',
  term: '',
  effectiveFrom: '',
  expiresAt: '',
}

export default function NoticeForm({
  initial,
  pending,
  onSubmit,
  onCancel,
}: {
  initial?: NoticeRequest
  pending: boolean
  onSubmit: (req: NoticeRequest) => void
  onCancel: () => void
}) {
  const { t } = useI18n()
  const [form, setForm] = useState<NoticeRequest>(initial ?? EMPTY)
  const set = <K extends keyof NoticeRequest>(key: K) => (value: NoticeRequest[K]) => setForm((f) => ({ ...f, [key]: value }))
  const valid = form.title.trim() && form.body.trim() && /^\d{4}-[12S]$/.test(form.term) && form.effectiveFrom && form.expiresAt && form.expiresAt > form.effectiveFrom

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (!valid) return
        onSubmit({ ...form, sourceUrl: form.sourceUrl?.trim() || null })
      }}
      style={{ display: 'flex', flexDirection: 'column', gap: 12 }}
    >
      <FieldGroup label={t('office.notice.category')}>
        <SelectField value={form.category} onChange={(e) => set('category')(e.target.value as NoticeCategory)}>
          {NOTICE_CATEGORIES.map((c) => (
            <option key={c} value={c}>{t(`office.category.${c}`)}</option>
          ))}
        </SelectField>
      </FieldGroup>
      <FieldGroup label={t('office.notice.title')}>
        <TextField value={form.title} maxLength={300} onChange={(e) => set('title')(e.target.value)} required />
      </FieldGroup>
      <FieldGroup label={t('office.notice.body')} hint={initial ? t('office.notice.editHint') : undefined}>
        <TextAreaField value={form.body} rows={10} maxLength={20000} onChange={(e) => set('body')(e.target.value)} required />
      </FieldGroup>
      <FieldGroup label={t('office.notice.sourceUrl')}>
        <TextField value={form.sourceUrl ?? ''} type="url" onChange={(e) => set('sourceUrl')(e.target.value)} />
      </FieldGroup>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 12 }}>
        <FieldGroup label={t('office.notice.term')}>
          <TextField value={form.term} placeholder="2027-1" onChange={(e) => set('term')(e.target.value)} required />
        </FieldGroup>
        <FieldGroup label={t('office.notice.effectiveFrom')}>
          <TextField value={form.effectiveFrom} type="date" onChange={(e) => set('effectiveFrom')(e.target.value)} required />
        </FieldGroup>
        <FieldGroup label={t('office.notice.expiresAt')}>
          <TextField value={form.expiresAt} type="date" onChange={(e) => set('expiresAt')(e.target.value)} required />
        </FieldGroup>
      </div>
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button type="button" variant="ghost" onClick={onCancel}>{t('office.notice.cancel')}</Button>
        <Button type="submit" variant="primary" disabled={!valid || pending}>{t('office.notice.save')}</Button>
      </div>
    </form>
  )
}
