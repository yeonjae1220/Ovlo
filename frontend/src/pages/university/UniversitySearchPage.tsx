import { useState } from 'react'
import { useUniversitySearch } from '../../hooks/useUniversity'

export default function UniversitySearchPage() {
  const [keyword, setKeyword] = useState('')
  const [countryCode, setCountryCode] = useState('')
  const { data: universities, isLoading } = useUniversitySearch(keyword, countryCode || undefined)

  return (
    <div style={{ maxWidth: 700, margin: '0 auto' }}>
      <h1>대학 검색</h1>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input
          placeholder="대학 이름 검색..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ flex: 1 }}
        />
        <input
          placeholder="국가코드 (예: KR, US)"
          value={countryCode}
          onChange={(e) => setCountryCode(e.target.value.toUpperCase())}
          style={{ width: 120 }}
          maxLength={2}
        />
      </div>

      {isLoading && <p>검색 중...</p>}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {universities?.map((u) => (
          <li key={u.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
            <strong>{u.name}</strong>
            <span style={{ marginLeft: 8, color: '#888' }}>{u.countryCode} · {u.city}</span>
            {u.websiteUrl && (
              <a href={u.websiteUrl} target="_blank" rel="noopener noreferrer" style={{ marginLeft: 8 }}>
                웹사이트
              </a>
            )}
          </li>
        ))}
      </ul>

      {universities?.length === 0 && keyword && <p>검색 결과가 없습니다.</p>}
    </div>
  )
}
