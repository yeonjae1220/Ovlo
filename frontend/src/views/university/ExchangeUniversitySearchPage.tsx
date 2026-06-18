'use client'

import { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import {
  useUniversityCatalogSearch,
  useUniversityCatalogCountries,
} from '../../hooks/useUniversity'
import { useDebounce } from '../../hooks/useDebounce'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import { useI18n } from '../../i18n/I18nProvider'
import type { UniversityCatalogItem } from '../../types'
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
  const { t, language } = useI18n()
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isMobile } = useBreakpoint()
  const initialQuery = searchParams?.get('q')?.trim() ?? ''

  const [query, setQuery] = useState(initialQuery)
  const [countryCode, setCountryCode] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebounce(query.trim(), 300)

  const { data: countries = [] } = useUniversityCatalogCountries()
  const { data: pageData, isLoading } = useUniversityCatalogSearch(
    debouncedQuery,
    countryCode,
    page,
    PAGE_SIZE
  )
  const universities = pageData?.content ?? []
  const hasSearched = !!(debouncedQuery || countryCode)

  const resetPage = () => setPage(0)

  const handleQueryChange = (value: string) => {
    setQuery(value)
    resetPage()
  }

  const handleAllClear = () => {
    setQuery('')
    setCountryCode('')
    resetPage()
  }

  // 콘텐츠 보유 우선순위: 리포트(집계 가이드)가 있으면 리포트, 없으면 후기 상세로 라우팅
  const openUniversity = (u: UniversityCatalogItem) => {
    if (u.hasReport && u.reportId) {
      router.push(`/university-reports/${u.reportId}?lang=${language}`)
    } else if (u.hasReviews && u.exchangeUnivId) {
      router.push(`/exchange-universities/${u.exchangeUnivId}`)
    }
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
          <SearchBox
            placeholder={t('exch.searchPlaceholder')}
            value={query}
            onChange={(event) => handleQueryChange(event.target.value)}
            aria-label={t('exch.searchPlaceholder')}
          />

          <SelectField
            value={countryCode}
            onChange={(event) => {
              setCountryCode(event.target.value)
              resetPage()
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

        {countryCode && (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
            <Badge tone="info">{countryCode}</Badge>
          </div>
        )}
      </Card>

      {isLoading && <SkeletonLines count={4} />}

      {!isLoading && hasSearched && universities.length === 0 && (
        <EmptyState
          icon="⌕"
          title={t('exch.empty')}
          description={t('exch.searchHint')}
          action={<Button variant="secondary" onClick={handleAllClear}>{t('exch.countryAll')}</Button>}
        />
      )}

      {!hasSearched && !isLoading && (
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
        {universities.map((u) => (
          <li key={`${u.globalUnivId ?? 'g'}-${u.exchangeUnivId ?? 'e'}-${u.reportId ?? 'r'}`}>
            <Card
              interactive
              onClick={() => openUniversity(u)}
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
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end', flexShrink: 0 }}>
                  {u.hasReport && <Badge tone="accent">{t('catalog.badge.report')}</Badge>}
                  {u.hasReviews && (
                    <Badge tone="info">{u.reviewCount} {t('catalog.badge.reviews')}</Badge>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {u.country && <Badge tone="neutral">{u.country}</Badge>}
                {u.city && <Badge tone="neutral">{u.city}</Badge>}
              </div>

              <div style={{ marginTop: 'auto', display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-end' }}>
                {u.hasReviews ? (
                  <div>
                    <div style={{ color: C.warning, fontSize: 15 }}>{STAR(u.avgRating)}</div>
                    <div style={{ fontSize: 12, color: C.dim, marginTop: 3 }}>
                      {(u.avgRating ?? 0).toFixed(1)} · {u.reviewCount} {t('exch.reviews')}
                    </div>
                  </div>
                ) : <span />}
                <span style={{ color: C.accent, fontSize: 13, fontWeight: 900 }}>
                  {t('exch.detail.viewFull')}
                </span>
              </div>
            </Card>
          </li>
        ))}
      </ul>

      {pageData && pageData.totalElements > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, marginTop: 22, flexWrap: 'wrap' }}>
          <Button onClick={() => setPage((p) => p - 1)} disabled={page === 0} variant="secondary">
            {t('common.prev')}
          </Button>
          <span style={{ color: C.muted, fontSize: 13, fontWeight: 750 }}>
            {page + 1} / {pageData.totalPages} · {pageData.totalElements}
          </span>
          <Button onClick={() => setPage((p) => p + 1)} disabled={!pageData.hasNext} variant="secondary">
            {t('common.next')}
          </Button>
        </div>
      )}
    </div>
  )
}
