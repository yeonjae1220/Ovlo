import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUniversityReportList } from '../../hooks/useUniversityReport'
import LanguageSwitcher from '../../components/LanguageSwitcher'

const AVAILABLE_LANGS = ['en', 'ko']

export default function UniversityReportListPage() {
  const [lang, setLang] = useState('en')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const { data, isLoading } = useUniversityReportList(lang, page)

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '24px 16px' }}>
      <h1 style={{ marginBottom: 8 }}>대학 교환학생 종합 보고서</h1>
      <p style={{ color: '#6b7280', marginBottom: 20 }}>
        AI가 유튜브 후기와 공식 프로그램 정보를 종합 분석한 대학별 가이드입니다.
      </p>

      <LanguageSwitcher langs={AVAILABLE_LANGS} current={lang} onChange={(l) => { setLang(l); setPage(0) }} />

      {isLoading && <p>불러오는 중...</p>}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {data?.content.map((r) => (
          <li
            key={r.id}
            onClick={() => navigate(`/university-reports/${r.id}?lang=${lang}`)}
            style={{
              padding: 16,
              borderBottom: '1px solid #e5e7eb',
              cursor: 'pointer',
            }}
          >
            <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 4 }}>{r.title}</div>
            {r.summary && (
              <div style={{ color: '#4b5563', fontSize: 14, marginBottom: 8 }}>{r.summary}</div>
            )}
            <div style={{ display: 'flex', gap: 16, fontSize: 13, color: '#6b7280' }}>
              <span>영상 {r.sourceVideoCount}개</span>
              {r.sourceWebCount > 0 && <span>웹 {r.sourceWebCount}개</span>}
              <span style={{ marginLeft: 'auto' }}>
                {r.supportedLangs.map((l) => l.toUpperCase()).join(' · ')}
              </span>
            </div>
          </li>
        ))}
      </ul>

      {data && data.totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24 }}>
          <button onClick={() => setPage(page - 1)} disabled={page === 0}>
            이전
          </button>
          <span style={{ lineHeight: '32px', fontSize: 14 }}>
            {page + 1} / {data.totalPages}
          </span>
          <button onClick={() => setPage(page + 1)} disabled={!data.hasNext}>
            다음
          </button>
        </div>
      )}

      {data?.content.length === 0 && (
        <p style={{ color: '#9ca3af', textAlign: 'center', marginTop: 48 }}>
          아직 생성된 보고서가 없습니다.
        </p>
      )}
    </div>
  )
}
