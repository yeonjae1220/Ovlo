'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useUniversitySearch } from '../../hooks/useUniversity'
import { useI18n } from '../../i18n/I18nProvider'

export default function UniversitySearchPage() {
  const { t } = useI18n()
  const [keyword, setKeyword] = useState('')
  const [countryCode, setCountryCode] = useState('')
  const { data: universities, isLoading } = useUniversitySearch(keyword, countryCode || undefined)

  return (
    <div>
      <h1>{t('univ.search.title')}</h1>
      <p style={{ marginBottom: 12, fontSize: 13, color: '#6b7280' }}>
        {t('univ.search.info')}{' '}
        <Link href="/exchange-universities" style={{ color: '#2563eb', fontWeight: 600 }}>{t('univ.search.infoLink')}</Link>
        {t('univ.search.infoTrail')}
      </p>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input
          placeholder={t('univ.search.placeholder')}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ flex: 1 }}
        />
        <input
          placeholder={t('univ.search.country')}
          value={countryCode}
          onChange={(e) => setCountryCode(e.target.value.toUpperCase())}
          style={{ width: 140 }}
          maxLength={2}
        />
      </div>

      {isLoading && <p>{t('univ.search.loading')}</p>}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {universities?.map((u) => (
          <li key={u.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
            <strong>{u.name}</strong>
            <span style={{ marginLeft: 8, color: '#888' }}>{u.countryCode} · {u.city}</span>
          </li>
        ))}
      </ul>

      {universities?.length === 0 && keyword && <p>{t('univ.search.notFound')}</p>}
    </div>
  )
}
