import { useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { useUniversityReport, useUniversityReportLanguages } from '../../hooks/useUniversity'

const C = {
  bg:          '#242424',
  card:        '#1e2836',
  cardHeader:  '#1a2332',
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  activeBg:    '#1e3a5f',
  activeBorder:'#2563eb',
  activeText:  '#60a5fa',
}

const LANG_LABEL: Record<string, string> = { ko: '한국어', en: 'English' }

interface ReportContent {
  costs?: { monthly_total?: string; currency?: string; rent?: string; food?: string; transport?: string }
  housing?: { dorm_available?: boolean; dorm_price?: string; dorm_type?: string }
  visa?: { type?: string; cost?: string; duration?: string; processing_days?: string }
  academics?: { difficulty?: string; workload?: string; gpa_req?: string; language_req?: string; deadline?: string }
}

function InfoCard({ icon, title, children }: { icon: string; title: string; children: React.ReactNode }) {
  return (
    <div style={{
      flex: 1, minWidth: 160, background: C.bg, borderRadius: 8,
      padding: '12px 14px', border: `1px solid ${C.border}`,
    }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 6 }}>
        {icon} {title}
      </div>
      {children}
    </div>
  )
}

export default function UniversityReportDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const reportId = Number(id)
  const [lang, setLang] = useState(searchParams.get('lang') ?? 'ko')

  const { data: report, isLoading } = useUniversityReport(reportId, lang)
  const { data: langs = [] } = useUniversityReportLanguages(reportId)

  const handleLang = (l: string) => {
    setLang(l)
    navigate(`/university-reports/${reportId}?lang=${l}`, { replace: true })
  }

  if (isLoading) return <div style={{ padding: 40, color: C.textMuted }}>불러오는 중...</div>
  if (!report)   return <div style={{ padding: 40, color: '#f87171' }}>보고서를 찾을 수 없습니다.</div>

  let parsed: ReportContent | null = null
  if (report.content) {
    try { parsed = typeof report.content === 'string' ? JSON.parse(report.content) : report.content }
    catch { /* ignore */ }
  }

  const availableLangs = langs.length > 1 ? langs : ['ko', 'en']

  return (
    <div style={{ padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.textMuted, fontSize: 14, marginBottom: 20, padding: 0 }}
      >
        ← 목록으로
      </button>

      {/* 헤더 */}
      <div style={{ background: C.card, borderRadius: 12, padding: '20px 24px', marginBottom: 24, border: `1px solid ${C.border}` }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 800, color: C.textPrimary }}>{report.title}</h1>
          {availableLangs.length > 1 && (
            <div style={{ display: 'flex', gap: 6 }}>
              {availableLangs.map((l) => (
                <button
                  key={l}
                  onClick={() => handleLang(l)}
                  style={{
                    padding: '4px 12px', borderRadius: 16, fontSize: 12, cursor: 'pointer',
                    border: lang === l ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
                    background: lang === l ? C.activeBg : 'transparent',
                    color: lang === l ? C.activeText : C.textMuted,
                    fontWeight: lang === l ? 700 : 400,
                  }}
                >
                  {LANG_LABEL[l] ?? l.toUpperCase()}
                </button>
              ))}
            </div>
          )}
        </div>
        <div style={{ marginTop: 10, fontSize: 12, color: C.textDim }}>
          영상 {report.sourceVideoCount}개 분석
          {report.sourceWebCount > 0 && ` · 웹 ${report.sourceWebCount}개`}
        </div>
      </div>

      {/* AI 면책 고지 */}
      <div style={{
        padding: '10px 14px', marginBottom: 20, borderRadius: 6,
        background: '#1a2234', border: `1px solid #2d3748`,
        fontSize: 12, color: C.textDim,
      }}>
        ⓘ AI가 영상 후기를 요약한 가이드입니다. 정확하지 않을 수 있습니다.
      </div>

      {/* 요약 */}
      {report.summary && (
        <p style={{ margin: '0 0 20px', fontSize: 15, color: C.textSec, lineHeight: 1.8 }}>
          {report.summary}
        </p>
      )}

      {/* 구조화 정보 카드 */}
      {parsed && (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 24 }}>
          {parsed.costs && (
            <InfoCard icon="💰" title="생활비">
              <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>
                {parsed.costs.monthly_total ?? '-'}
              </div>
              {parsed.costs.currency && (
                <div style={{ fontSize: 11, color: C.textMuted, marginTop: 2 }}>{parsed.costs.currency}</div>
              )}
              {parsed.costs.rent && (
                <div style={{ fontSize: 12, color: C.textMuted, marginTop: 4 }}>월세 {parsed.costs.rent}</div>
              )}
            </InfoCard>
          )}
          {parsed.housing && (
            <InfoCard icon="🏠" title="기숙사">
              <div style={{ fontSize: 13, fontWeight: 600, color: parsed.housing.dorm_available ? '#4ade80' : '#f87171' }}>
                {parsed.housing.dorm_available ? '입사 가능' : '입사 불가'}
              </div>
              {parsed.housing.dorm_price && (
                <div style={{ fontSize: 11, color: C.textMuted, marginTop: 2 }}>{parsed.housing.dorm_price}</div>
              )}
              {parsed.housing.dorm_type && (
                <div style={{ fontSize: 11, color: C.textMuted }}>{parsed.housing.dorm_type}</div>
              )}
            </InfoCard>
          )}
          {parsed.visa && (
            <InfoCard icon="🛂" title="비자">
              <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>{parsed.visa.type ?? '-'}</div>
              {parsed.visa.cost && (
                <div style={{ fontSize: 11, color: C.textMuted, marginTop: 2 }}>비용 {parsed.visa.cost}</div>
              )}
              {parsed.visa.processing_days && (
                <div style={{ fontSize: 11, color: C.textMuted }}>처리 {parsed.visa.processing_days}</div>
              )}
            </InfoCard>
          )}
          {parsed.academics && (
            <InfoCard icon="📚" title="학업">
              {parsed.academics.difficulty && (
                <div style={{ fontSize: 12, color: C.textSec }}>난이도 {parsed.academics.difficulty}</div>
              )}
              {parsed.academics.gpa_req && (
                <div style={{ fontSize: 12, color: C.textSec }}>GPA {parsed.academics.gpa_req}</div>
              )}
              {parsed.academics.language_req && (
                <div style={{ fontSize: 12, color: C.textSec }}>어학 {parsed.academics.language_req}</div>
              )}
              {parsed.academics.deadline && (
                <div style={{ fontSize: 12, color: C.textMuted, marginTop: 2 }}>마감 {parsed.academics.deadline}</div>
              )}
            </InfoCard>
          )}
        </div>
      )}

      {/* 본문 */}
      {report.body && (
        <div style={{
          background: C.card, borderRadius: 10, padding: '20px 24px',
          border: `1px solid ${C.border}`,
        }}>
          <p style={{
            margin: 0, fontSize: 14, color: C.textSec,
            lineHeight: 1.9, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
          }}>
            {report.body}
          </p>
        </div>
      )}
    </div>
  )
}
