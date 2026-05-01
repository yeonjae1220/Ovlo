import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useExchangeUniversitySearch, useExchangeUniversityCountries, useGlobalUniversitySearch } from '../../hooks/useUniversity'

const STAR = (avg?: number | null) =>
  avg !== undefined && avg !== null
    ? '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg))
    : '☆☆☆☆☆'

const PAGE_SIZE = 20

export default function ExchangeUniversitySearchPage() {
  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniName, setSelectedUniName] = useState('')
  const [countryCode, setCountryCode] = useState('')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data: countries = [] } = useExchangeUniversityCountries()
  // global_universities 기반 autocomplete (회원가입과 동일한 패턴)
  const { data: globalResults } = useGlobalUniversitySearch(uniQuery)

  // exchange_universities 결과 조회 (선택된 대학명 또는 국가코드로 검색)
  const { data, isLoading } = useExchangeUniversitySearch(selectedUniName, countryCode, page, PAGE_SIZE)
  const universities = data?.content ?? []
  const hasSearched = !!(selectedUniName || countryCode)

  const handleCountryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setCountryCode(e.target.value)
    setPage(0)
  }

  const handleUniSelect = (name: string) => {
    setSelectedUniName(name)
    setUniQuery('')
    setPage(0)
  }

  const handleUniClear = () => {
    setSelectedUniName('')
    setUniQuery('')
    setPage(0)
  }

  const inputStyle: React.CSSProperties = {
    padding: '10px 14px',
    border: '1px solid #374151',
    borderRadius: 8,
    fontSize: 14,
    outline: 'none',
    background: '#1e2836',
    color: '#e2e8f0',
  }

  return (
    <div style={{ padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로가기 */}
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94a3b8', fontSize: 14, marginBottom: 16, padding: 0 }}
      >
        ← 뒤로
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 10, marginBottom: 6 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, color: '#f1f5f9', margin: 0 }}>교환 대학 검색</h1>
        <button
          onClick={() => navigate('/university-reports')}
          style={{
            padding: '7px 14px', borderRadius: 20, fontSize: 13, cursor: 'pointer',
            border: '1px solid #374151', background: '#1e2836', color: '#60a5fa',
            fontWeight: 600, whiteSpace: 'nowrap',
          }}
        >
          ✦ AI 보고서 →
        </button>
      </div>
      <p style={{ color: '#94a3b8', fontSize: 14, marginBottom: 24 }}>
        파견 가능한 교환 대학을 검색하고 상세 정보·영상 리뷰를 확인하세요.
      </p>

      {/* 대학명 검색 (global_universities autocomplete) */}
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
            ✓ {selectedUniName} <span style={{ color: '#64748b', fontSize: 12, fontWeight: 400 }}>(클릭하여 변경)</span>
          </button>
        ) : (
          <div style={{ position: 'relative' }}>
            <input
              style={{ ...inputStyle, width: '100%', boxSizing: 'border-box' }}
              placeholder="대학 이름 검색 (10,000+ 대학)"
              value={uniQuery}
              onChange={(e) => setUniQuery(e.target.value)}
            />
            {uniQuery.length >= 1 && globalResults && globalResults.length > 0 && (
              <ul style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
                listStyle: 'none', padding: 0, margin: 0,
                border: '1px solid #374151', borderRadius: 8, maxHeight: 200, overflowY: 'auto',
                background: '#1e2836', boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
              }}>
                {globalResults.slice(0, 8).map((u) => (
                  <li
                    key={u.id}
                    onClick={() => handleUniSelect(u.name)}
                    style={{
                      padding: '9px 14px', cursor: 'pointer', fontSize: 14,
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      borderBottom: '1px solid #2d3748', color: '#e2e8f0',
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = '#2d3748' }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
                  >
                    <span>{u.name}</span>
                    <span style={{ color: '#64748b', fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                  </li>
                ))}
              </ul>
            )}
            {uniQuery.length >= 1 && globalResults?.length === 0 && (
              <p style={{ margin: '4px 0 0', fontSize: 13, color: '#64748b' }}>검색 결과 없음</p>
            )}
          </div>
        )}
      </div>

      {/* 국가 드롭다운 */}
      <div style={{ marginBottom: 24 }}>
        <select
          style={{
            ...inputStyle,
            width: '100%',
            boxSizing: 'border-box',
            cursor: 'pointer',
            appearance: 'none',
            backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E")`,
            backgroundRepeat: 'no-repeat',
            backgroundPosition: 'right 14px center',
            paddingRight: 36,
          }}
          value={countryCode}
          onChange={handleCountryChange}
        >
          <option value="">전체 국가</option>
          {countries.map((c) => (
            <option key={c.countryCode} value={c.countryCode}>
              {c.country} ({c.universityCount}개)
            </option>
          ))}
        </select>
      </div>

      {/* 결과 */}
      {isLoading && <p style={{ color: '#94a3b8' }}>검색 중...</p>}

      {!isLoading && hasSearched && universities.length === 0 && (
        <p style={{ color: '#64748b', textAlign: 'center', paddingTop: 40 }}>
          해당하는 교환대학 데이터가 없습니다.
        </p>
      )}

      {!hasSearched && !isLoading && (
        <p style={{ color: '#64748b', textAlign: 'center', paddingTop: 40 }}>
          대학명 또는 국가를 입력해 검색하세요.
        </p>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
        {universities.map((u) => (
          <li
            key={u.id}
            onClick={() => navigate(`/exchange-universities/${u.id}`)}
            style={{
              padding: '16px 20px',
              border: '1px solid #2d3748',
              borderRadius: 10,
              cursor: 'pointer',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: '#1e2836',
              transition: 'border-color 0.15s, box-shadow 0.15s',
            }}
            onMouseEnter={(e) => {
              const el = e.currentTarget as HTMLLIElement
              el.style.borderColor = '#4c6ef5'
              el.style.boxShadow = '0 4px 16px rgba(0,0,0,0.3)'
            }}
            onMouseLeave={(e) => {
              const el = e.currentTarget as HTMLLIElement
              el.style.borderColor = '#2d3748'
              el.style.boxShadow = 'none'
            }}
          >
            <div>
              <div style={{ fontWeight: 700, fontSize: 16, color: '#f1f5f9' }}>{u.nameKo || u.nameEn}</div>
              {u.nameKo && u.nameEn && (
                <div style={{ fontSize: 13, color: '#94a3b8', marginTop: 2 }}>{u.nameEn}</div>
              )}
              <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>{u.country} · {u.city}</div>
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: 16 }}>
              <div style={{ color: '#f59e0b', fontSize: 14, letterSpacing: 1 }}>
                {STAR(u.avgRating)}
              </div>
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                {(u.avgRating ?? 0).toFixed(1)} · 리뷰 {u.reviewCount}개
              </div>
            </div>
          </li>
        ))}
      </ul>

      {data && data.totalElements > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, marginTop: 24 }}>
          <button
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
            style={{
              padding: '8px 18px', borderRadius: 8, border: '1px solid #374151',
              background: page === 0 ? '#1a2234' : '#1e2836', color: page === 0 ? '#475569' : '#e2e8f0',
              cursor: page === 0 ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            ← 이전
          </button>
          <span style={{ color: '#94a3b8', fontSize: 13 }}>
            {page + 1} / {data.totalPages}페이지 ({data.totalElements}개)
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={!data.hasNext}
            style={{
              padding: '8px 18px', borderRadius: 8, border: '1px solid #374151',
              background: !data.hasNext ? '#1a2234' : '#1e2836', color: !data.hasNext ? '#475569' : '#e2e8f0',
              cursor: !data.hasNext ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            다음 →
          </button>
        </div>
      )}
    </div>
  )
}
