import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useExchangeUniversitySearch,
  useExchangeUniversityCountries,
  useGlobalUniversitySearch,
} from '../../hooks/useUniversity'
import { useI18n } from '../../i18n/I18nProvider'

const STAR = (avg?: number | null) =>
  avg !== undefined && avg !== null
    ? '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg))
    : '☆☆☆☆☆'

const PAGE_SIZE = 20

const C = {
  bg:          '#242424',
  card:        '#1e2836',
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  activeBg:    '#1e3a5f',
  activeBorder:'#2563eb',
  activeText:  '#60a5fa',
  purple:      '#a78bfa',
}

const inputStyle: React.CSSProperties = {
  padding: '10px 14px',
  border: `1px solid ${C.borderLight}`,
  borderRadius: 8,
  fontSize: 14,
  outline: 'none',
  background: C.card,
  color: C.textPrimary,
}

export default function ExchangeUniversitySearchPage() {
  const { t } = useI18n()
  const navigate = useNavigate()

  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniName, setSelectedUniName] = useState('')
  const [countryCode, setCountryCode] = useState('')
  const [exchPage, setExchPage] = useState(0)

  const { data: countries = [] } = useExchangeUniversityCountries()
  const { data: globalResults } = useGlobalUniversitySearch(uniQuery)
  const { data: exchData, isLoading: exchLoading } = useExchangeUniversitySearch(
    selectedUniName, countryCode, exchPage, PAGE_SIZE
  )
  const exchUniversities = exchData?.content ?? []
  const exchHasSearched = !!(selectedUniName || countryCode)

  const handleCountryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setCountryCode(e.target.value)
    setExchPage(0)
  }
  const handleUniSelect = (name: string) => { setSelectedUniName(name); setUniQuery(''); setExchPage(0) }
  const handleUniClear = () => { setSelectedUniName(''); setUniQuery(''); setExchPage(0) }

  return (
    <div style={{ padding: '28px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.textMuted, fontSize: 14, marginBottom: 16, padding: 0 }}
      >
        {t('exch.back')}
      </button>

      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, color: C.textPrimary, margin: '0 0 6px' }}>{t('exch.title')}</h1>
        <p style={{ color: C.textMuted, fontSize: 14, margin: 0 }}>{t('exch.subtitle')}</p>
      </div>

      {/* University name search (autocomplete) */}
      <div style={{ marginBottom: 12 }}>
        {selectedUniName ? (
          <button
            type="button"
            onClick={handleUniClear}
            style={{
              width: '100%', padding: '10px 14px', textAlign: 'left',
              border: '1px solid #4ade80', borderRadius: 8, background: '#0d2a1a',
              color: '#4ade80', fontWeight: 500, fontSize: 14, cursor: 'pointer',
            }}
          >
            ✓ {selectedUniName} <span style={{ color: C.textDim, fontSize: 12, fontWeight: 400 }}>{t('exch.change')}</span>
          </button>
        ) : (
          <div style={{ position: 'relative' }}>
            <input
              style={{ ...inputStyle, width: '100%', boxSizing: 'border-box' }}
              placeholder={t('exch.searchPlaceholder')}
              value={uniQuery}
              onChange={(e) => setUniQuery(e.target.value)}
            />
            {uniQuery.length >= 1 && globalResults && globalResults.length > 0 && (
              <ul style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
                listStyle: 'none', padding: 0, margin: 0,
                border: `1px solid ${C.borderLight}`, borderRadius: 8, maxHeight: 200, overflowY: 'auto',
                background: C.card, boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
              }}>
                {globalResults.slice(0, 8).map((u) => (
                  <li
                    key={u.id}
                    onClick={() => handleUniSelect(u.name)}
                    style={{
                      padding: '9px 14px', cursor: 'pointer', fontSize: 14,
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      borderBottom: `1px solid ${C.border}`, color: C.textPrimary,
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = C.border }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
                  >
                    <span>{u.name}</span>
                    <span style={{ color: C.textDim, fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                  </li>
                ))}
              </ul>
            )}
            {uniQuery.length >= 1 && globalResults?.length === 0 && (
              <p style={{ margin: '4px 0 0', fontSize: 13, color: C.textDim }}>{t('chat.noResults')}</p>
            )}
          </div>
        )}
      </div>

      {/* Country dropdown */}
      <div style={{ marginBottom: 24 }}>
        <select
          style={{
            ...inputStyle, width: '100%', boxSizing: 'border-box', cursor: 'pointer',
            appearance: 'none',
            backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E")`,
            backgroundRepeat: 'no-repeat', backgroundPosition: 'right 14px center', paddingRight: 36,
          }}
          value={countryCode}
          onChange={handleCountryChange}
        >
          <option value="">{t('exch.countryAll')}</option>
          {countries.map((c) => (
            <option key={c.countryCode} value={c.countryCode}>
              {c.country} ({c.universityCount})
            </option>
          ))}
        </select>
      </div>

      {exchLoading && <p style={{ color: C.textMuted }}>{t('exch.loading')}</p>}
      {!exchLoading && exchHasSearched && exchUniversities.length === 0 && (
        <p style={{ color: C.textDim, textAlign: 'center', paddingTop: 40 }}>{t('exch.empty')}</p>
      )}
      {!exchHasSearched && !exchLoading && (
        <p style={{ color: C.textDim, textAlign: 'center', paddingTop: 40 }}>{t('exch.searchHint')}</p>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
        {exchUniversities.map((u) => (
          <li
            key={u.id}
            onClick={() => navigate(`/exchange-universities/${u.id}`)}
            style={{
              padding: '16px 20px', border: `1px solid ${C.border}`, borderRadius: 10,
              cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              background: C.card, transition: 'border-color 0.15s, box-shadow 0.15s',
            }}
            onMouseEnter={(e) => {
              const el = e.currentTarget as HTMLLIElement
              el.style.borderColor = '#4c6ef5'
              el.style.boxShadow = '0 4px 16px rgba(0,0,0,0.3)'
            }}
            onMouseLeave={(e) => {
              const el = e.currentTarget as HTMLLIElement
              el.style.borderColor = C.border
              el.style.boxShadow = 'none'
            }}
          >
            <div>
              <div style={{ fontWeight: 700, fontSize: 16, color: C.textPrimary }}>{u.nameKo || u.nameEn}</div>
              {u.nameKo && u.nameEn && <div style={{ fontSize: 13, color: C.textMuted, marginTop: 2 }}>{u.nameEn}</div>}
              <div style={{ fontSize: 13, color: C.textDim, marginTop: 4 }}>{u.country} · {u.city}</div>
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: 16 }}>
              <div style={{ color: '#f59e0b', fontSize: 14, letterSpacing: 1 }}>{STAR(u.avgRating)}</div>
              <div style={{ fontSize: 12, color: C.textDim, marginTop: 2 }}>
                {(u.avgRating ?? 0).toFixed(1)} · {u.reviewCount} {t('exch.reviews')}
              </div>
            </div>
          </li>
        ))}
      </ul>

      {exchData && exchData.totalElements > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, marginTop: 24 }}>
          <button
            onClick={() => setExchPage((p) => p - 1)}
            disabled={exchPage === 0}
            style={{
              padding: '8px 18px', borderRadius: 8, border: `1px solid ${C.borderLight}`,
              background: exchPage === 0 ? '#1a2234' : C.card,
              color: exchPage === 0 ? '#475569' : C.textSec,
              cursor: exchPage === 0 ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            {t('common.prev')}
          </button>
          <span style={{ color: C.textMuted, fontSize: 13 }}>
            {exchPage + 1} / {exchData.totalPages} ({exchData.totalElements})
          </span>
          <button
            onClick={() => setExchPage((p) => p + 1)}
            disabled={!exchData.hasNext}
            style={{
              padding: '8px 18px', borderRadius: 8, border: `1px solid ${C.borderLight}`,
              background: !exchData.hasNext ? '#1a2234' : C.card,
              color: !exchData.hasNext ? '#475569' : C.textSec,
              cursor: !exchData.hasNext ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            {t('common.next')}
          </button>
        </div>
      )}
    </div>
  )
}
