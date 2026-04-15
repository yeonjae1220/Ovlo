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
    <div style={{ padding: '2rem', maxWidth: 800, margin: '0 auto' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1.5rem' }}>
        어드민 대시보드
      </h1>

      {loading ? (
        <p>로딩 중...</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
          <StatCard label="전체 회원 수" value={stats?.totalMembers ?? 0} />
        </div>
      )}

      <div style={{ marginTop: '2rem', display: 'flex', gap: '1rem' }}>
        <Link
          to="/admin/members"
          style={{
            padding: '0.75rem 1.5rem',
            background: '#3b82f6',
            color: '#fff',
            borderRadius: '0.5rem',
            textDecoration: 'none',
            fontWeight: 600,
          }}
        >
          회원 관리
        </Link>
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
      <p style={{ fontSize: '0.875rem', color: '#64748b', marginBottom: '0.5rem' }}>{label}</p>
      <p style={{ fontSize: '2rem', fontWeight: 700, color: '#1e293b' }}>{value.toLocaleString()}</p>
    </div>
  )
}
