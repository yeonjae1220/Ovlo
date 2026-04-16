import { useEffect, useState } from 'react'
import { adminApi, type AdminUniversityResponse } from '../../api/admin'

export default function AdminUniversitiesPage() {
  const [universities, setUniversities] = useState<AdminUniversityResponse[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const PAGE_SIZE = 20

  useEffect(() => {
    setLoading(true)
    adminApi
      .getUniversities(page, PAGE_SIZE)
      .then((data) => {
        setUniversities(data.content)
        setTotalElements(data.totalElements)
      })
      .finally(() => setLoading(false))
  }, [page])

  const totalPages = Math.ceil(totalElements / PAGE_SIZE)

  return (
    <div style={{ padding: '2rem', maxWidth: 1000, margin: '0 auto', background: '#fff', minHeight: '100vh', color: '#1e293b' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem', color: '#0f172a' }}>대학교 관리</h1>
      <p style={{ color: '#475569', marginBottom: '1.5rem' }}>전체 {totalElements}개</p>

      {loading ? (
        <p style={{ color: '#475569' }}>로딩 중...</p>
      ) : (
        <>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ background: '#f1f5f9', textAlign: 'left' }}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>이름</th>
                <th style={thStyle}>현지명</th>
                <th style={thStyle}>국가 코드</th>
                <th style={thStyle}>도시</th>
              </tr>
            </thead>
            <tbody>
              {universities.map((univ) => (
                <tr key={univ.id} style={{ borderBottom: '1px solid #e2e8f0', background: '#fff' }}>
                  <td style={tdStyle}>{univ.id}</td>
                  <td style={tdStyle}>{univ.name}</td>
                  <td style={{ ...tdStyle, color: '#475569' }}>{univ.localName ?? '-'}</td>
                  <td style={tdStyle}>
                    <span style={{ padding: '0.2rem 0.5rem', borderRadius: '0.375rem', background: '#e0f2fe', color: '#0369a1', fontSize: '0.8rem', fontWeight: 600 }}>
                      {univ.countryCode}
                    </span>
                  </td>
                  <td style={tdStyle}>{univ.city}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', justifyContent: 'center' }}>
              <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} style={btnStyle}>
                이전
              </button>
              <span style={{ lineHeight: '2rem', color: '#475569' }}>
                {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                style={btnStyle}
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

const thStyle: React.CSSProperties = { padding: '0.75rem 1rem', fontWeight: 600, color: '#374151' }
const tdStyle: React.CSSProperties = { padding: '0.75rem 1rem', color: '#1e293b' }
const btnStyle: React.CSSProperties = { padding: '0.25rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: '0.375rem', background: '#f8fafc', color: '#374151', cursor: 'pointer' }
