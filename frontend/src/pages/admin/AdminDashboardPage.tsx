import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi, type AdminStatsResponse } from '../../api/admin'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStatsResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    adminApi.getStats()
      .then(setStats)
      .finally(() => setLoading(false))
  }, [])

  return (
    <div style={{ padding: '2rem', maxWidth: 900, margin: '0 auto', background: '#fff', minHeight: '100vh', color: '#1e293b' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1.5rem', color: '#0f172a' }}>
        어드민 대시보드
      </h1>

      {loading ? (
        <p style={{ color: '#475569' }}>로딩 중...</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '1rem' }}>
          <StatCard label="전체 회원 수" value={stats?.totalMembers ?? 0} />
          <StatCard label="전체 게시판 수" value={stats?.totalBoards ?? 0} />
          <StatCard label="전체 게시글 수" value={stats?.totalPosts ?? 0} />
          <StatCard label="전체 대학교 수" value={stats?.totalUniversities ?? 0} />
        </div>
      )}

      <div style={{ marginTop: '2rem' }}>
        <h2 style={{ fontSize: '1rem', fontWeight: 600, color: '#374151', marginBottom: '0.75rem' }}>관리 메뉴</h2>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <NavButton to="/admin/members" label="회원 관리" />
          <NavButton to="/admin/boards" label="게시판 관리" />
          <NavButton to="/admin/posts" label="게시글 관리" />
          <NavButton to="/admin/universities" label="대학교 관리" />
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div
      style={{
        padding: '1.5rem',
        background: '#f8fafc',
        border: '1px solid #e2e8f0',
        borderRadius: '0.75rem',
      }}
    >
      <p style={{ fontSize: '0.875rem', color: '#475569', marginBottom: '0.5rem' }}>{label}</p>
      <p style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a' }}>{value.toLocaleString()}</p>
    </div>
  )
}

function NavButton({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      style={{
        padding: '0.6rem 1.25rem',
        background: '#1e293b',
        color: '#f1f5f9',
        borderRadius: '0.5rem',
        textDecoration: 'none',
        fontWeight: 600,
        fontSize: '0.9rem',
      }}
    >
      {label}
    </Link>
  )
}
