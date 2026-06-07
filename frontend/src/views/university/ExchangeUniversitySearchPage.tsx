'use client'

import { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import {
  useExchangeUniversitySearch,
  useExchangeUniversityCountries,
  useGlobalUniversitySearch,
} from '../../hooks/useUniversity'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import { useI18n } from '../../i18n/I18nProvider'
import { Badge, Button, Card, EmptyState, PageHeader, SearchBox, SelectField, SkeletonLines } from '../../components/ui'

const STAR = (avg?: number | null) =>
  avg !== undefined && avg !== null
    ? '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg))
    : '☆☆☆☆☆'

const PAGE_SIZE = 20

const C = {
  border: 'var(--color-border)',
  card: 'var(--color-surface)',
  hover: 'var(--color-surface-hover)',
  text: 'var(--color-text)',
  textSec: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent)',
  success: 'var(--color-success)',
  warning: 'var(--color-warning)',
}

export default function ExchangeUniversitySearchPage() {
  const { t } = useI18n()
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isMobile } = useBreakpoint()
  const initialQuery = searchParams?.get('q')?.trim() ?? ''

  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniName, setSelectedUniName] = useState(initialQuery)
  const [countryCode, setCountryCode] = useState('')
  const [exchPage, setExchPage] = useState(0)

  const { data: countries = [] } = useExchangeUniversityCountries()
  const { data: globalResults } = useGlobalUniversitySearch(uniQuery)
  const { data: exchData, isLoading: exchLoading } = useExchangeUniversitySearch(
    selectedUniName,
    countryCode,
    exchPage,
    PAGE_SIZE
  )
  const exchUniversities = exchData?.content ?? []
  const exchHasSearched = !!(selectedUniName || countryCode)

  const handleUniSelect = (name: string) => {
    setSelectedUniName(name)
    setUniQuery('')
    setExchPage(0)
  }

  const handleUniClear = () => {
    setSelectedUniName('')
    setUniQuery('')
    setExchPage(0)
  }

  const handleAllClear = () => {
    setSelectedUniName('')
    setUniQuery('')
    setCountryCode('')
    setExchPage(0)
  }

  return (
    <div>
      <PageHeader
        eyebrow={t('exch.back')}
        title={t('exch.title')}
        description={t('exch.subtitle')}
        actions={
          <Button variant="ghost" onClick={() => router.back()} icon="←">
            {t('exch.back').replace('← ', '')}
          </Button>
        }
      />

      <Card style={{ padding: isMobile ? 14 : 18, marginBottom: 18 }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: isMobile ? '1fr' : 'minmax(0, 1.4fr) minmax(220px, .6fr)',
          gap: 12,
          alignItems: 'start',
        }}>
          <div style={{ position: 'relative' }}>
            {selectedUniName ? (
              <button
                type="button"
                onClick={handleUniClear}
                className="ui-field"
                style={{
                  textAlign: 'left',
                  borderColor: C.success,
                  background: 'var(--color-success-soft)',
                  color: C.success,
                  fontWeight: 800,
                  cursor: 'pointer',
                }}
              >
                ✓ {selectedUniName} <span style={{ color: C.dim, fontSize: 12, fontWeight: 600 }}>{t('exch.change')}</span>
              </button>
            ) : (
              <>
                <SearchBox
                  placeholder={t('exch.searchPlaceholder')}
                  value={uniQuery}
                  onChange={(event) => setUniQuery(event.target.value)}
                  aria-label={t('exch.searchPlaceholder')}
                />
                {uniQuery.length >= 1 && globalResults && globalResults.length > 0 && (
                  <ul style={{
                    position: 'absolute',
                    top: 'calc(100% + 6px)',
                    left: 0,
                    right: 0,
                    zIndex: 10,
                    listStyle: 'none',
                    padding: 0,
                    margin: 0,
                    border: `1px solid ${C.border}`,
                    borderRadius: 10,
                    maxHeight: 260,
                    overflowY: 'auto',
                    background: C.card,
                    boxShadow: 'var(--shadow-soft)',
                  }}>
                    {globalResults.slice(0, 8).map((u) => (
                      <li
                        key={u.id}
                        onClick={() => handleUniSelect(u.name)}
                        style={{
                          padding: '11px 14px',
                          cursor: 'pointer',
                          display: 'flex',
                          justifyContent: 'space-between',
                          gap: 14,
                          alignItems: 'center',
                          borderBottom: `1px solid ${C.border}`,
                          color: C.text,
                        }}
                        onMouseEnter={(event) => { event.currentTarget.style.background = C.hover }}
                        onMouseLeave={(event) => { event.currentTarget.style.background = 'transparent' }}
                      >
                        <span style={{ fontWeight: 800 }}>{u.name}</span>
                        <span style={{ color: C.dim, fontSize: 12, textAlign: 'right' }}>{u.countryCode} · {u.city}</span>
                      </li>
                    ))}
                  </ul>
                )}
                {uniQuery.length >= 1 && globalResults?.length === 0 && (
                  <p style={{ margin: '8px 2px 0', fontSize: 13, color: C.dim }}>{t('chat.noResults')}</p>
                )}
              </>
            )}
          </div>

          <SelectField
            value={countryCode}
            onChange={(event) => {
              setCountryCode(event.target.value)
              setExchPage(0)
            }}
            aria-label={t('exch.countryAll')}
          >
            <option value="">{t('exch.countryAll')}</option>
            {countries.map((c) => (
              <option key={c.countryCode} value={c.countryCode}>
                {c.country} ({c.universityCount})
              </option>
            ))}
          </SelectField>
        </div>

        {(selectedUniName || countryCode) && (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
            {selectedUniName && <Badge tone="success">{selectedUniName}</Badge>}
            {countryCode && <Badge tone="info">{countryCode}</Badge>}
          </div>
        )}
      </Card>

      {exchLoading && <SkeletonLines count={4} />}

      {!exchLoading && exchHasSearched && exchUniversities.length === 0 && (
        <EmptyState
          icon="⌕"
          title={t('exch.empty')}
          description={t('exch.searchHint')}
          action={<Button variant="secondary" onClick={handleAllClear}>{t('exch.countryAll')}</Button>}
        />
      )}

      {!exchHasSearched && !exchLoading && (
        <EmptyState
          icon="⌕"
          title={t('exch.searchHint')}
          description={t('exch.subtitle')}
        />
      )}

      <ul style={{
        listStyle: 'none',
        padding: 0,
        margin: 0,
        display: 'grid',
        gridTemplateColumns: isMobile ? '1fr' : 'repeat(2, minmax(0, 1fr))',
        gap: 12,
      }}>
        {exchUniversities.map((u) => (
          <li key={u.id}>
            <Card
              interactive
              onClick={() => router.push(`/exchange-universities/${u.id}`)}
              style={{ padding: 18, minHeight: 178, display: 'flex', flexDirection: 'column', gap: 14 }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 900, fontSize: 17, lineHeight: 1.35, color: C.text, overflowWrap: 'anywhere' }}>
                    {u.nameKo || u.nameEn}
                  </div>
                  {u.nameKo && u.nameEn && (
                    <div style={{ fontSize: 13, color: C.muted, marginTop: 4, overflowWrap: 'anywhere' }}>{u.nameEn}</div>
                  )}
                </div>
                <Badge tone="accent">{u.reviewCount} {t('exch.reviews')}</Badge>
              </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <Badge tone="neutral">{u.country}</Badge>
                <Badge tone="neutral">{u.city}</Badge>
              </div>

              <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-end' }}>
                <div>
                  <div style={{ color: C.warning, fontSize: 15 }}>{STAR(u.avgRating)}</div>
                  <div style={{ fontSize: 12, color: C.dim, marginTop: 3 }}>
                    {(u.avgRating ?? 0).toFixed(1)} · {u.reviewCount} {t('exch.reviews')}
                  </div>
                </div>
                <span style={{ color: C.accent, fontSize: 13, fontWeight: 900 }}>
                  {t('exch.detail.viewFull')}
                </span>
              </div>
            </Card>
          </li>
        ))}
      </ul>

      {exchData && exchData.totalElements > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, marginTop: 22, flexWrap: 'wrap' }}>
          <Button onClick={() => setExchPage((p) => p - 1)} disabled={exchPage === 0} variant="secondary">
            {t('common.prev')}
          </Button>
          <span style={{ color: C.muted, fontSize: 13, fontWeight: 750 }}>
            {exchPage + 1} / {exchData.totalPages} · {exchData.totalElements}
          </span>
          <Button onClick={() => setExchPage((p) => p + 1)} disabled={!exchData.hasNext} variant="secondary">
            {t('common.next')}
          </Button>
        </div>
      )}
    </div>
  )
}
