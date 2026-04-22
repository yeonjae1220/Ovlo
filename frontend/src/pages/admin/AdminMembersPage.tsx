import { useEffect, useState } from 'react'
import { adminApi, type AdminMemberResponse } from '../../api/admin'
import type { MemberRole } from '../../types'

export default function AdminMembersPage() {
  const [members, setMembers] = useState<AdminMemberResponse[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  const PAGE_SIZE = 20

  useEffect(() => {
    setLoading(true)
    adminApi
      .getMembers(page, PAGE_SIZE)
      .then((data) => {
        setMembers(data.content)
        setTotalElements(data.totalElements)
      })
      .finally(() => setLoading(false))
  }, [page])

  const handleRoleToggle = async (member: AdminMemberResponse) => {
    const newRole: MemberRole = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'
    if (!confirm(`${member.nickname}의 역할을 ${newRole}로 변경하시겠습니까?`)) return

    setUpdatingId(member.id)
    try {
      const updated = await adminApi.updateMemberRole(member.id, newRole)
      setMembers((prev) => prev.map((m) => (m.id === updated.id ? updated : m)))
    } catch {
      alert('역할 변경에 실패했습니다.')
    } finally {
      setUpdatingId(null)
    }
  }

  const totalPages = Math.ceil(totalElements / PAGE_SIZE)

  return (
    <div style={{ padding: '2rem', maxWidth: 1000, margin: '0 auto', background: '#fff', color: '#1e293b' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem', color: '#0f172a' }}>회원 관리</h1>
      <p style={{ color: '#475569', marginBottom: '1.5rem' }}>전체 {totalElements}명</p>

      {loading ? (
        <p style={{ color: '#475569' }}>로딩 중...</p>
      ) : (
        <>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ background: '#f1f5f9', textAlign: 'left' }}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>닉네임</th>
                <th style={thStyle}>이메일</th>
                <th style={thStyle}>상태</th>
                <th style={thStyle}>역할</th>
                <th style={thStyle}>제공자</th>
                <th style={thStyle}>역할 변경</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => (
                <tr key={member.id} style={{ borderBottom: '1px solid #e2e8f0', background: '#fff' }}>
                  <td style={tdStyle}>{member.id}</td>
                  <td style={tdStyle}>{member.nickname}</td>
                  <td style={tdStyle}>{member.email}</td>
                  <td style={tdStyle}>
                    <StatusBadge status={member.status} />
                  </td>
                  <td style={tdStyle}>
                    <span
                      style={{
                        padding: '0.2rem 0.5rem',
                        borderRadius: '0.375rem',
                        background: member.role === 'ADMIN' ? '#dbeafe' : '#e2e8f0',
                        color: member.role === 'ADMIN' ? '#1d4ed8' : '#334155',
                        fontWeight: 600,
                        fontSize: '0.8rem',
                      }}
                    >
                      {member.role}
                    </span>
                  </td>
                  <td style={tdStyle}>{member.provider}</td>
                  <td style={tdStyle}>
                    <button
                      onClick={() => handleRoleToggle(member)}
                      disabled={updatingId === member.id}
                      style={{
                        padding: '0.25rem 0.75rem',
                        borderRadius: '0.375rem',
                        border: '1px solid #cbd5e1',
                        background: '#f8fafc',
                        color: '#374151',
                        cursor: 'pointer',
                        fontSize: '0.8rem',
                      }}
                    >
                      {updatingId === member.id
                        ? '변경 중...'
                        : member.role === 'ADMIN'
                          ? 'MEMBER로'
                          : 'ADMIN으로'}
                    </button>
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

function StatusBadge({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    ACTIVE: '#dcfce7',
    WITHDRAWN: '#fee2e2',
    PENDING_ONBOARDING: '#fef9c3',
  }
  const textMap: Record<string, string> = {
    ACTIVE: '#166534',
    WITHDRAWN: '#991b1b',
    PENDING_ONBOARDING: '#92400e',
  }
  return (
    <span
      style={{
        padding: '0.2rem 0.5rem',
        borderRadius: '0.375rem',
        background: colorMap[status] ?? '#e2e8f0',
        color: textMap[status] ?? '#334155',
        fontSize: '0.8rem',
        fontWeight: 600,
      }}
    >
      {status}
    </span>
  )
}

const thStyle: React.CSSProperties = {
  padding: '0.75rem 1rem',
  fontWeight: 600,
  color: '#374151',
}

const tdStyle: React.CSSProperties = {
  padding: '0.75rem 1rem',
  color: '#1e293b',
}

const btnStyle: React.CSSProperties = {
  padding: '0.25rem 0.75rem',
  border: '1px solid #cbd5e1',
  borderRadius: '0.375rem',
  background: '#f8fafc',
  color: '#374151',
  cursor: 'pointer',
}
