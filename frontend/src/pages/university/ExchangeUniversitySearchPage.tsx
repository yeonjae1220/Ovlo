import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useExchangeUniversitySearch, useGlobalUniversitySearch } from '../../hooks/useUniversity'

const STAR = (avg?: number) =>
  avg !== undefined && avg !== null
    ? '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg))
    : '—'

export default function ExchangeUniversitySearchPage() {
  const [uniQuery, setUniQuery] = useState('')
  const [selectedUniName, setSelectedUniName] = useState('')
  const [country, setCountry] = useState('')
  const navigate = useNavigate()

  // global_universities 기반 autocomplete (회원가입과 동일한 패턴)
  const { data: globalResults } = useGlobalUniversitySearch(uniQuery)

  // exchange_universities 결과 조회 (선택된 대학명 또는 국가로 검색)
  const { data, isLoading } = useExchangeUniversitySearch(selectedUniName, country)
  const universities = data?.content ?? []
  const hasSearched = !!(selectedUniName || country)

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
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로가기 */}
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94a3b8', fontSize: 14, marginBottom: 16, padding: 0 }}
      >
        ← 뒤로
      </button>

      <h1 style={{ fontSize: 24, fontWeight: 700, color: '#f1f5f9', marginBottom: 6 }}>교환 대학 검색</h1>
      <p style={{ color: '#94a3b8', fontSize: 14, marginBottom: 24 }}>
        파견 가능한 교환 대학을 검색하고 상세 정보·영상 리뷰를 확인하세요.
      </p>

      {/* 대학명 검색 (global_universities autocomplete) */}
      <div style={{ marginBottom: 12 }}>
        {selectedUniName ? (
          <button
            type="button"
            onClick={() => { setSelectedUniName(''); setUniQuery('') }}
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
                    onClick={() => { setSelectedUniName(u.name); setUniQuery('') }}
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

      {/* 국가 검색 (한국어 텍스트, 기존 유지) */}
      <div style={{ marginBottom: 24 }}>
        <input
          style={{ ...inputStyle, width: '100%', boxSizing: 'border-box' }}
          placeholder="국가 (예: 일본, 미국, 영국)"
          value={country}
          onChange={(e) => setCountry(e.target.value)}
        />
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
                {u.avgRating !== undefined && u.avgRating !== null
                  ? `${u.avgRating.toFixed(1)} · `
                  : ''}
                리뷰 {u.reviewCount}개
              </div>
            </div>
          </li>
        ))}
      </ul>

      {data && data.totalElements > universities.length && (
        <p style={{ textAlign: 'center', marginTop: 20, color: '#64748b', fontSize: 13 }}>
          {data.totalElements}개 중 {universities.length}개 표시 중
        </p>
      )}
    </div>
  )
}
