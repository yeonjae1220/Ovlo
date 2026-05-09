import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUniversityReports } from '../../hooks/useUniversity'

function stripReportSuffix(title: string) {
  return title
    .replace(/\s*(교환학생\s*가이드|Exchange\s*(Student\s*)?Guide|Austauschführer|Guide d'échange|Guía de intercambio|Trao đổi sinh viên|交换生指南|交換学生ガイド)\s*$/i, '')
    .replace(/:\s*$/, '')
    .trim()
}

const C = {
  bg:          '#242424',
  card:        '#1e2836',
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

const LANG_LABEL: Record<string, string> = {
  ko: '한국어', en: 'English', ja: '日本語', zh: '中文', de: 'DE', fr: 'FR', vi: 'VI',
}
const PAGE_SIZE = 20

export default function UniversityReportsPage() {
  const [lang, setLang] = useState('ko')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data, isLoading } = useUniversityReports(lang, keyword, page, PAGE_SIZE)
  const reports = data?.content ?? []

  const handleLang = (l: string) => { setLang(l); setPage(0) }
  const handleKeyword = (e: React.ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value)
    setPage(0)
  }

  return (
    <div style={{ padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.textMuted, fontSize: 14, marginBottom: 16, padding: 0 }}
      >
        ← 뒤로
      </button>

      <h1 style={{ fontSize: 24, fontWeight: 700, color: C.textPrimary, margin: '0 0 24px' }}>
        대학 교환학생 종합 보고서
      </h1>

      {/* 검색 + 언어 필터 */}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 20 }}>
        <input
          style={{
            flex: 1, minWidth: 200, padding: '9px 14px',
            border: `1px solid ${C.borderLight}`, borderRadius: 8,
            fontSize: 14, background: C.card, color: C.textPrimary, outline: 'none',
          }}
          placeholder="대학명 검색 (영문)"
          value={keyword}
          onChange={handleKeyword}
        />
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {['ko', 'en', 'ja', 'zh', 'de', 'fr', 'vi'].map((l) => (
            <button
              key={l}
              onClick={() => handleLang(l)}
              style={{
                padding: '8px 16px', borderRadius: 20, fontSize: 13, cursor: 'pointer',
                border: lang === l ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
                background: lang === l ? C.activeBg : C.card,
                color: lang === l ? C.activeText : C.textMuted,
                fontWeight: lang === l ? 700 : 400,
              }}
            >
              {LANG_LABEL[l]}
            </button>
          ))}
        </div>
      </div>

      {isLoading && <p style={{ color: C.textMuted }}>불러오는 중...</p>}

      {!isLoading && reports.length === 0 && (
        <p style={{ color: C.textDim, textAlign: 'center', paddingTop: 48 }}>
          {keyword ? '검색 결과가 없습니다.' : '아직 생성된 보고서가 없습니다.'}
        </p>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {reports.map((r) => (
          <li
            key={r.id}
            onClick={() => navigate(`/university-reports/${r.id}?lang=${lang}`)}
            style={{
              padding: '16px 20px', borderBottom: `1px solid ${C.border}`,
              cursor: 'pointer', transition: 'background 0.15s',
            }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = C.card }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
          >
            <div style={{ fontWeight: 600, fontSize: 16, color: C.textPrimary, marginBottom: 4 }}>
              {stripReportSuffix(r.title)}
            </div>
            {r.summary && (
              <div style={{ fontSize: 14, color: C.textSec, marginBottom: 8, lineHeight: 1.6 }}>
                {r.summary}
              </div>
            )}
            <div style={{ display: 'flex', gap: 16, fontSize: 12, color: C.textMuted, alignItems: 'center' }}>
              <span>영상 {r.sourceVideoCount}개 분석</span>
              {r.sourceWebCount > 0 && <span>웹 {r.sourceWebCount}개</span>}
              <span style={{ marginLeft: 'auto' }}>
                {r.supportedLangs.map((l) => l.toUpperCase()).join(' · ')}
              </span>
            </div>
            <div style={{ marginTop: 8, fontSize: 11, color: C.textDim }}>
              * AI가 영상 후기를 요약한 가이드입니다. 정확하지 않을 수 있습니다.
            </div>
          </li>
        ))}
      </ul>

      {data && data.totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24, alignItems: 'center' }}>
          <button
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
            style={{
              padding: '8px 18px', borderRadius: 8, border: `1px solid ${C.borderLight}`,
              background: page === 0 ? '#1a2234' : C.card,
              color: page === 0 ? '#475569' : C.textSec,
              cursor: page === 0 ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            ← 이전
          </button>
          <span style={{ color: C.textMuted, fontSize: 13 }}>
            {page + 1} / {data.totalPages}페이지
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={!data.hasNext}
            style={{
              padding: '8px 18px', borderRadius: 8, border: `1px solid ${C.borderLight}`,
              background: !data.hasNext ? '#1a2234' : C.card,
              color: !data.hasNext ? '#475569' : C.textSec,
              cursor: !data.hasNext ? 'default' : 'pointer', fontSize: 14,
            }}
          >
            다음 →
          </button>
        </div>
      )}
    </div>
  )
}
