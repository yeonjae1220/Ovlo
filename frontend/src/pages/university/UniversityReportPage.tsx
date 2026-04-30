import { useState } from 'react'
import { useParams, useSearchParams, useNavigate } from 'react-router-dom'
import { useUniversityReport, useUniversityReportLanguages } from '../../hooks/useUniversityReport'
import LanguageSwitcher from '../../components/LanguageSwitcher'

export default function UniversityReportPage() {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [lang, setLang] = useState(searchParams.get('lang') ?? 'en')

  const reportId = Number(id)
  const { data: report, isLoading, isError } = useUniversityReport(reportId, lang)
  const { data: langs } = useUniversityReportLanguages(reportId)

  const handleLangChange = (l: string) => {
    setLang(l)
    navigate(`/university-reports/${id}?lang=${l}`, { replace: true })
  }

  if (isLoading) return <div style={{ padding: 32 }}>불러오는 중...</div>
  if (isError || !report) return <div style={{ padding: 32 }}>보고서를 찾을 수 없습니다.</div>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '24px 16px' }}>
      {/* Header */}
      <div
        style={{
          background: '#f9fafb',
          border: '1px solid #e5e7eb',
          borderRadius: 12,
          padding: 24,
          marginBottom: 24,
        }}
      >
        <h1 style={{ margin: '0 0 12px', fontSize: 24 }}>{report.title}</h1>
        {report.summary && (
          <p style={{ color: '#4b5563', margin: '0 0 16px', fontSize: 15 }}>{report.summary}</p>
        )}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 20, fontSize: 14 }}>
          <StatBadge
            label="분석 출처"
            value={`영상 ${report.sourceVideoCount}개${report.sourceWebCount > 0 ? ` · 웹 ${report.sourceWebCount}개` : ''}`}
          />
        </div>
      </div>

      {/* Language switcher */}
      {langs && langs.length > 1 && (
        <LanguageSwitcher langs={langs} current={lang} onChange={handleLangChange} />
      )}

      {/* Body (markdown-style text rendered as plain paragraphs) */}
      <article>
        <MarkdownBody body={report.body} />
      </article>

      <div
        style={{
          marginTop: 40,
          paddingTop: 16,
          borderTop: '1px solid #e5e7eb',
          fontSize: 13,
          color: '#9ca3af',
        }}
      >
        마지막 업데이트: {new Date(report.createdAt).toLocaleDateString()}
      </div>
    </div>
  )
}

function StatBadge({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ color: '#6b7280', fontSize: 12, marginBottom: 2 }}>{label}</div>
      <div style={{ fontWeight: 600 }}>{value}</div>
    </div>
  )
}

function MarkdownBody({ body }: { body: string }) {
  if (!body) return null

  const lines = body.split('\n')
  const elements: React.ReactNode[] = []
  let key = 0

  for (const line of lines) {
    if (line.startsWith('## ')) {
      elements.push(
        <h2 key={key++} style={{ fontSize: 18, marginTop: 28, marginBottom: 8, color: '#111827' }}>
          {line.slice(3)}
        </h2>,
      )
    } else if (line.startsWith('### ')) {
      elements.push(
        <h3 key={key++} style={{ fontSize: 15, marginTop: 16, marginBottom: 4, color: '#1f2937' }}>
          {line.slice(4)}
        </h3>,
      )
    } else if (line.startsWith('- ') || line.startsWith('• ')) {
      elements.push(
        <li key={key++} style={{ marginLeft: 20, marginBottom: 4, fontSize: 15, color: '#374151' }}>
          {line.slice(2)}
        </li>,
      )
    } else if (line.trim() === '') {
      elements.push(<div key={key++} style={{ height: 8 }} />)
    } else {
      elements.push(
        <p key={key++} style={{ margin: '0 0 8px', fontSize: 15, color: '#374151', lineHeight: 1.7 }}>
          {line}
        </p>,
      )
    }
  }

  return <div>{elements}</div>
}
