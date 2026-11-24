'use client'

import { useId, useState } from 'react'
import { useUniversitySearch } from '../hooks/useUniversity'
import { useDebounce } from '../hooks/useDebounce'
import { useI18n } from '../i18n/I18nProvider'
import { Button, QueryErrorNotice, TextField } from './ui'

export function UniversitySelector({ value, onChange }: { value: number | null; onChange: (id: number | null) => void }) {
  const { t, language } = useI18n()
  const id = useId()
  const [keyword, setKeyword] = useState('')
  const query = useDebounce(keyword.trim(), 300)
  const schools = useUniversitySearch(query, undefined, language)
  return <div>
    <label htmlFor={id}>{t('onboarding.university')}</label>
    <TextField id={id} value={keyword} onChange={e => { setKeyword(e.target.value); onChange(null) }} autoComplete="off" />
    {schools.isError && <QueryErrorNotice onRetry={() => void schools.refetch()} />}
    {schools.isFetching && <p role="status">{t('common.loading')}</p>}
    {!schools.isError && value === null && <ul style={{ maxHeight: 220, overflow: 'auto', listStyle: 'none', padding: 0 }}>
      {schools.data?.map(u => <li key={u.id}><Button onClick={() => { onChange(u.id); setKeyword(u.localName || u.name) }}>{u.localName || u.name} · {u.countryCode}</Button></li>)}
    </ul>}
    {value !== null && <p role="status">✓ {keyword}</p>}
  </div>
}
