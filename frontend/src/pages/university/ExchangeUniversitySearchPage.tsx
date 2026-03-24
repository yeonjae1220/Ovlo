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

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, color: '#111827', marginBottom: 6 }}>교환 대학 검색</h1>
      <p style={{ color: '#6b7280', fontSize: 14, marginBottom: 24 }}>
        파견 가능한 교환 대학을 검색하고 상세 정보·영상 리뷰를 확인하세요.
      </p>

      {/* 검색 바 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 24 }}>
        <input
          style={{ flex: 1, padding: '10px 14px', border: '1px solid #d1d5db', borderRadius: 8, fontSize: 14, outline: 'none' }}
          placeholder="대학 이름 검색 (한국어·영어 모두 가능)"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <input
          style={{ width: 160, padding: '10px 14px', border: '1px solid #d1d5db', borderRadius: 8, fontSize: 14, outline: 'none' }}
          placeholder="국가 (예: Japan)"
          value={country}
          onChange={(e) => setCountry(e.target.value)}
        />
      </div>

      {/* 결과 */}
      {isLoading && <p style={{ color: '#9ca3af' }}>검색 중...</p>}

      {!isLoading && universities.length === 0 && (keyword || country) && (
        <p style={{ color: '#9ca3af', textAlign: 'center', paddingTop: 40 }}>검색 결과가 없습니다.</p>
      )}

      {!isLoading && !keyword && !country && (
        <p style={{ color: '#9ca3af', textAlign: 'center', paddingTop: 40 }}>대학명 또는 국가를 입력해 검색하세요.</p>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {universities.map((u) => (
          <li
            key={u.id}
            onClick={() => navigate(`/exchange-universities/${u.id}`)}
            style={{
              padding: '16px 20px',
              border: '1px solid #e5e7eb',
              borderRadius: 10,
              cursor: 'pointer',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              transition: 'box-shadow 0.15s, border-color 0.15s',
            }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.boxShadow = '0 4px 16px rgba(0,0,0,0.08)'; (e.currentTarget as HTMLLIElement).style.borderColor = '#bfdbfe' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.boxShadow = 'none'; (e.currentTarget as HTMLLIElement).style.borderColor = '#e5e7eb' }}
          >
            <div>
              <div style={{ fontWeight: 700, fontSize: 16, color: '#111827' }}>{u.nameKo || u.nameEn}</div>
              {u.nameKo && u.nameEn && (
                <div style={{ fontSize: 13, color: '#6b7280', marginTop: 2 }}>{u.nameEn}</div>
              )}
              <div style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>{u.country} · {u.city}</div>
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: 16 }}>
              <div style={{ color: '#f59e0b', fontSize: 14, letterSpacing: 1 }}>
                {STAR(u.avgRating)}
              </div>
              <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>
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
        <p style={{ textAlign: 'center', marginTop: 20, color: '#9ca3af', fontSize: 13 }}>
          {data.totalElements}개 중 {universities.length}개 표시 중
        </p>
      )}
    </div>
  )
}
