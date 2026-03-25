import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useExchangeUniversitySearch } from '../../hooks/useUniversity'

const STAR = (avg?: number) =>
  avg !== undefined && avg !== null
    ? '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg))
    : '—'

export default function ExchangeUniversitySearchPage() {
  const [keyword, setKeyword] = useState('')
  const [country, setCountry] = useState('')
  const navigate = useNavigate()

  const { data, isLoading } = useExchangeUniversitySearch(keyword, country)
  const universities = data?.content ?? []

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
      <h1 style={{ fontSize: 24, fontWeight: 700, color: '#f1f5f9', marginBottom: 6 }}>교환 대학 검색</h1>
      <p style={{ color: '#94a3b8', fontSize: 14, marginBottom: 24 }}>
        파견 가능한 교환 대학을 검색하고 상세 정보·영상 리뷰를 확인하세요.
      </p>

      {/* 검색 바 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 24 }}>
        <input
          style={{ ...inputStyle, flex: 1 }}
          placeholder="대학 이름 검색 (한국어·영어 모두 가능)"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <input
          style={{ ...inputStyle, width: 160 }}
          placeholder="국가 (예: Japan)"
          value={country}
          onChange={(e) => setCountry(e.target.value)}
        />
      </div>

      {/* 결과 */}
      {isLoading && <p style={{ color: '#94a3b8' }}>검색 중...</p>}

      {!isLoading && universities.length === 0 && (keyword || country) && (
        <p style={{ color: '#64748b', textAlign: 'center', paddingTop: 40 }}>검색 결과가 없습니다.</p>
      )}

      {!isLoading && !keyword && !country && (
        <p style={{ color: '#64748b', textAlign: 'center', paddingTop: 40 }}>대학명 또는 국가를 입력해 검색하세요.</p>
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
