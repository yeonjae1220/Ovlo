import { useEffect, useState } from 'react'
import { adminApi, type AdminBoardResponse } from '../../api/admin'

export default function AdminBoardsPage() {
  const [boards, setBoards] = useState<AdminBoardResponse[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const PAGE_SIZE = 20

  useEffect(() => {
    setLoading(true)
    adminApi
      .getBoards(page, PAGE_SIZE)
      .then((data) => {
        setBoards(data.content)
        setTotalElements(data.totalElements)
      })
      .finally(() => setLoading(false))
  }, [page])

  const totalPages = Math.ceil(totalElements / PAGE_SIZE)

  return (
    <div style={{ padding: '2rem', maxWidth: 1000, margin: '0 auto', background: '#fff', minHeight: '100vh', color: '#1e293b' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem', color: '#0f172a' }}>게시판 관리</h1>
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
                <th style={thStyle}>카테고리</th>
                <th style={thStyle}>범위</th>
                <th style={thStyle}>생성자 ID</th>
                <th style={thStyle}>대학 ID</th>
                <th style={thStyle}>활성</th>
              </tr>
            </thead>
            <tbody>
              {boards.map((board) => (
                <tr key={board.id} style={{ borderBottom: '1px solid #e2e8f0', background: '#fff' }}>
                  <td style={tdStyle}>{board.id}</td>
                  <td style={tdStyle}>{board.name}</td>
                  <td style={tdStyle}>
                    <span style={tagStyle}>{board.category}</span>
                  </td>
                  <td style={tdStyle}>
                    <span style={tagStyle}>{board.scope}</span>
                  </td>
                  <td style={tdStyle}>{board.creatorId}</td>
                  <td style={tdStyle}>{board.universityId ?? '-'}</td>
                  <td style={tdStyle}>
                    <span
                      style={{
                        padding: '0.2rem 0.5rem',
                        borderRadius: '0.375rem',
                        fontSize: '0.8rem',
                        fontWeight: 600,
                        background: board.active ? '#dcfce7' : '#fee2e2',
                        color: board.active ? '#166534' : '#991b1b',
                      }}
                    >
                      {board.active ? '활성' : '비활성'}
                    </span>
                  </td>
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
const tagStyle: React.CSSProperties = { padding: '0.2rem 0.5rem', borderRadius: '0.375rem', background: '#e2e8f0', color: '#334155', fontSize: '0.8rem', fontWeight: 500 }
