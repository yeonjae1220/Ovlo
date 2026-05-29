import { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import { useParams, useNavigate } from 'react-router-dom'
import { useExchangeUniversity, useExchangeUniversityReviews, useUniversityReportByExchangeUniv } from '../../hooks/useUniversity'
import type { VideoReview, ExchangeUniversity } from '../../types'
import { useI18n } from '../../i18n/I18nProvider'
import type { MessageKey } from '../../i18n/messages'

// ── 다크 테마 색상 상수 ──────────────────────────────────────
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
  recommendBg: '#0d2a1a',
  notRecBg:    '#2a0d0d',
  recommendTx: '#4ade80',
  notRecTx:    '#f87171',
}

// ── 언어 국기 ────────────────────────────────────────────────
const LANG_FLAG: Record<string, string> = {
  ko: '🇰🇷', ja: '🇯🇵', en: '🇺🇸', zh: '🇨🇳', fr: '🇫🇷',
  de: '🇩🇪', es: '🇪🇸', pt: '🇧🇷', vi: '🇻🇳', th: '🇹🇭',
}

// Direction filter values (labels provided at render time via t())
const DIRECTION_VALUES = [
  { value: undefined,   key: 'exch.direction.all' },
  { value: 'INBOUND',  key: 'exch.direction.inbound' },
  { value: 'OUTBOUND', key: 'exch.direction.outbound' },
  { value: 'UNKNOWN',  key: 'exch.direction.unknown' },
] as const

// ── 뱃지 ─────────────────────────────────────────────────────
const Badge = ({ text, color = '#2563eb' }: { text: string; color?: string }) => (
  <span style={{
    display: 'inline-block', padding: '2px 10px', borderRadius: 20,
    background: color + '28', color, fontSize: 12, fontWeight: 600, marginRight: 4, marginBottom: 4,
  }}>{text}</span>
)

// ── 정보 행 ──────────────────────────────────────────────────
const InfoRow = ({ label, value }: { label: string; value?: string | number | boolean | null }) => {
  if (value === undefined || value === null || value === '') return null
  const display = typeof value === 'boolean' ? (value ? '✓' : '✗') : String(value)
  return (
    <div style={{ display: 'flex', gap: 12, padding: '7px 0', borderBottom: `1px solid ${C.border}` }}>
      <span style={{ minWidth: 110, color: C.textDim, fontSize: 13, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 14, color: C.textSec, wordBreak: 'break-word' }}>{display}</span>
    </div>
  )
}

// ── 태그 불일치 감지 ─────────────────────────────────────────
function isTagMismatch(review: VideoReview, univ: ExchangeUniversity): boolean {
  const tags = review.tags ?? []
  if (tags.length === 0) return false
  const nameKo = univ.nameKo ?? ''
  const nameEn = univ.nameEn ?? ''
  return !tags.some(t =>
    (nameKo && t.includes(nameKo)) || (nameEn && t.toLowerCase().includes(nameEn.toLowerCase()))
  )
}

// ── 확장 요약 생성 (3~6문장 줄글) ────────────────────────────
type ReviewTFn = (key: MessageKey) => string

function buildDetailedSummary(r: VideoReview, t: ReviewTFn): string {
  const parts: string[] = []

  if (r.summary) parts.push(r.summary)

  const ratingParts: string[] = []
  if (r.overallRating != null) ratingParts.push(`${r.overallRating}/5`)
  if (r.overallTone) ratingParts.push(r.overallTone)
  if (r.recommend != null) ratingParts.push(r.recommend ? t('exch.detail.recommended') : t('exch.detail.notRecommended'))
  if (ratingParts.length > 0) parts.push(ratingParts.join(' · '))

  if (r.costTotal || r.costRent || r.costFood) {
    const currency = r.costCurrency ? ` (${r.costCurrency})` : ''
    const costParts: string[] = []
    if (r.costTotal) costParts.push(r.costTotal)
    if (r.costRent) costParts.push(`${t('exch.detail.rent')}: ${r.costRent}`)
    if (r.costFood) costParts.push(`${t('exch.detail.food')}: ${r.costFood}`)
    if (r.costTransport) costParts.push(`${t('exch.detail.transport')}: ${r.costTransport}`)
    parts.push(`${t('exch.detail.costLabel')}${currency}: ${costParts.join(', ')}`)
  }

  if (r.difficulty != null || r.workload != null) {
    const acadParts: string[] = []
    if (r.difficulty != null) acadParts.push(`${t('exch.detail.difficulty')}: ${r.difficulty}/5`)
    if (r.workload != null) acadParts.push(`${t('exch.detail.workload')}: ${r.workload}/5`)
    parts.push(`${t('exch.detail.academics')} — ${acadParts.join(', ')}`)
  }

  if (r.dormAvailable != null) {
    let dormLine = r.dormAvailable ? t('exch.detail.dormAvailableText') : t('exch.detail.dormNotAvailableText')
    if (r.dormType) dormLine += ` (${r.dormType})`
    if (r.dormPrice) dormLine += `, ${t('exch.detail.cost')}: ${r.dormPrice}`
    parts.push(dormLine)
  }

  const reqParts: string[] = []
  if (r.visaType) reqParts.push(`${t('exch.detail.visa')}: ${r.visaType}`)
  if (r.languageReq) reqParts.push(`${t('exch.detail.language')}: ${r.languageReq}`)
  if (r.gpaRequirement) reqParts.push(`${t('exch.detail.gpa')}: ${r.gpaRequirement}`)
  if (reqParts.length > 0) parts.push(`${t('exch.detail.requirements')}: ${reqParts.join(', ')}`)

  return parts.join(' | ')
}

// ── 메인 컴포넌트 ────────────────────────────────────────────
export default function ExchangeUniversityDetailPage() {
  const { t, language } = useI18n()
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const univId = Number(id)
  const [direction, setDirection] = useState<string | undefined>(undefined)
  const [reportLang, setReportLang] = useState(language)

  const { data: univ, isLoading: univLoading } = useExchangeUniversity(univId)
  const { data: reviewPage, isLoading: reviewLoading } = useExchangeUniversityReviews(univId, direction)
  const { data: aiReport } = useUniversityReportByExchangeUniv(univId || null, reportLang)
  const reviews = reviewPage?.content ?? []

  if (univLoading) return <div style={{ padding: 40, color: C.textMuted }}>{t('exch.detail.loading')}</div>
  if (!univ) return <div style={{ padding: 40, color: C.notRecTx }}>{t('exch.detail.notFound')}</div>

  const starCount = univ.avgRating ? Math.round(univ.avgRating) : 0

  return (
    <div style={{ padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로가기 */}
      <button onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.textMuted, fontSize: 14, marginBottom: 20, padding: 0 }}>
        {t('exch.detail.back')}
      </button>

      {/* 대학 헤더 */}
      <div style={{ padding: '24px', background: C.card, borderRadius: 12, marginBottom: 28, border: `1px solid ${C.border}` }}>
        <h1 style={{ margin: '0 0 4px', fontSize: 26, fontWeight: 800, color: C.textPrimary }}>
          {univ.nameKo || univ.nameEn}
        </h1>
        {univ.nameKo && univ.nameEn && (
          <p style={{ margin: '0 0 12px', color: C.textMuted, fontSize: 15 }}>{univ.nameEn}</p>
        )}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
          <Badge text={`${univ.country} · ${univ.city}`} color="#94a3b8" />
          {univ.avgRating != null && (
            <>
              <span style={{ color: '#f59e0b', fontSize: 18, letterSpacing: 1 }}>
                {'★'.repeat(starCount)}{'☆'.repeat(5 - starCount)}
              </span>
              <span style={{ fontSize: 14, fontWeight: 700, color: C.textSec }}>
                {univ.avgRating.toFixed(1)}
              </span>
            </>
          )}
          <Badge text={`${univ.reviewCount} ${t('exch.detail.reviews')}`} color="#60a5fa" />
        </div>
        {univ.website && (
          <a href={univ.website} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-block', marginTop: 14, color: '#60a5fa', fontSize: 14, fontWeight: 500 }}>
            {t('exch.detail.website')}
          </a>
        )}
      </div>

      {/* AI 종합 보고서 */}
      {aiReport && (
        <div style={{ background: C.card, borderRadius: 12, marginBottom: 24, border: `1px solid ${C.border}`, overflow: 'hidden' }}>
          <div style={{ padding: '14px 20px', background: C.cardHeader, borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <span style={{ fontSize: 14, fontWeight: 700, color: C.textPrimary }}>{t('exch.detail.aiReport')}</span>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              {['ko', 'en', 'ja', 'zh', 'de', 'fr', 'vi'].map(l => (
                <button key={l} onClick={() => setReportLang(l as typeof language)} style={{
                  padding: '3px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
                  border: reportLang === l ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
                  background: reportLang === l ? C.activeBg : 'transparent',
                  color: reportLang === l ? C.activeText : C.textMuted,
                  fontWeight: reportLang === l ? 700 : 400,
                }}>
                  {l.toUpperCase()}
                </button>
              ))}
              <button onClick={() => navigate(`/university-reports/${aiReport.id}?lang=${reportLang}`)} style={{
                padding: '3px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
                border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.activeText,
              }}>
                {t('exch.detail.viewFull')}
              </button>
            </div>
          </div>
          <div style={{ padding: '16px 20px' }}>
            {aiReport.summary && (
              <p style={{ margin: '0 0 14px', fontSize: 14, color: C.textSec, lineHeight: 1.8 }}>{aiReport.summary}</p>
            )}
            {(() => {
              let parsed: { costs?: { monthly_total?: string; currency?: string; rent?: string }; housing?: { dorm_available?: boolean; dorm_price?: string; dorm_type?: string }; visa?: { type?: string; cost?: string; processing_days?: string }; academics?: { difficulty?: string; gpa_req?: string; language_req?: string } } | null = null
              if (aiReport.content) {
                try { parsed = typeof aiReport.content === 'string' ? JSON.parse(aiReport.content) : aiReport.content } catch { /* ignore */ }
              }
              if (!parsed) return null
              return (
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 10 }}>
                  {parsed.costs && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>💰 {t('exch.detail.cost')}</div>
                      <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>{parsed.costs.monthly_total ?? '-'}</div>
                      {parsed.costs.currency && <div style={{ fontSize: 11, color: C.textMuted }}>{parsed.costs.currency}</div>}
                      {parsed.costs.rent && <div style={{ fontSize: 11, color: C.textMuted }}>{t('exch.detail.rent')} {parsed.costs.rent}</div>}
                    </div>
                  )}
                  {parsed.housing && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>🏠 {t('exch.detail.dorm')}</div>
                      <div style={{ fontSize: 13, fontWeight: 600, color: parsed.housing.dorm_available ? '#4ade80' : '#f87171' }}>
                        {parsed.housing.dorm_available ? t('exch.detail.dormAvailable') : t('exch.detail.dormNotAvailable')}
                      </div>
                      {parsed.housing.dorm_price && <div style={{ fontSize: 11, color: C.textMuted }}>{parsed.housing.dorm_price}</div>}
                    </div>
                  )}
                  {parsed.visa && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>🛂 {t('exch.detail.visa')}</div>
                      <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>{parsed.visa.type ?? '-'}</div>
                      {parsed.visa.cost && <div style={{ fontSize: 11, color: C.textMuted }}>{t('exch.detail.visaCost')} {parsed.visa.cost}</div>}
                      {parsed.visa.processing_days && <div style={{ fontSize: 11, color: C.textMuted }}>{t('exch.detail.processing')} {parsed.visa.processing_days}</div>}
                    </div>
                  )}
                  {parsed.academics && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>📚 {t('exch.detail.academics')}</div>
                      {parsed.academics.difficulty && <div style={{ fontSize: 12, color: C.textSec }}>{t('exch.detail.difficulty')} {parsed.academics.difficulty}</div>}
                      {parsed.academics.gpa_req && <div style={{ fontSize: 12, color: C.textSec }}>GPA {parsed.academics.gpa_req}</div>}
                      {parsed.academics.language_req && <div style={{ fontSize: 12, color: C.textSec }}>{t('exch.detail.languageReq')} {parsed.academics.language_req}</div>}
                    </div>
                  )}
                </div>
              )
            })()}
            {/* 전체 보고서 본문 (마크다운 렌더링) */}
            {aiReport.body && (
              <details style={{ marginTop: 12 }}>
                <summary style={{ cursor: 'pointer', fontSize: 13, color: C.textMuted, padding: '6px 0', userSelect: 'none' }}>
                  {t('exch.detail.showReport')}
                </summary>
                <div style={{ marginTop: 10, fontSize: 13, color: C.textSec, lineHeight: 1.9, borderTop: `1px solid ${C.border}`, paddingTop: 10 }}>
                  <ReactMarkdown
                    components={{
                      h2: ({ children }) => (
                        <h2 style={{ fontSize: 15, fontWeight: 700, marginTop: 20, marginBottom: 6, color: C.textPrimary, borderBottom: `1px solid ${C.border}`, paddingBottom: 4 }}>
                          {children}
                        </h2>
                      ),
                      h3: ({ children }) => (
                        <h3 style={{ fontSize: 14, fontWeight: 600, marginTop: 14, marginBottom: 4, color: C.textPrimary }}>
                          {children}
                        </h3>
                      ),
                      p: ({ children }) => (
                        <p style={{ margin: '6px 0', color: C.textSec, lineHeight: 1.9 }}>
                          {children}
                        </p>
                      ),
                      strong: ({ children }) => (
                        <strong style={{ fontWeight: 700, color: C.textPrimary }}>
                          {children}
                        </strong>
                      ),
                      em: ({ children }) => (
                        <em style={{ fontStyle: 'italic', color: C.textSec }}>
                          {children}
                        </em>
                      ),
                      ul: ({ children }) => (
                        <ul style={{ paddingLeft: 20, margin: '6px 0' }}>
                          {children}
                        </ul>
                      ),
                      ol: ({ children }) => (
                        <ol style={{ paddingLeft: 20, margin: '6px 0' }}>
                          {children}
                        </ol>
                      ),
                      li: ({ children }) => (
                        <li style={{ margin: '3px 0', color: C.textSec }}>
                          {children}
                        </li>
                      ),
                      hr: () => (
                        <hr style={{ border: 'none', borderTop: `1px solid ${C.border}`, margin: '14px 0' }} />
                      ),
                    }}
                  >
                    {aiReport.body}
                  </ReactMarkdown>
                </div>
              </details>
            )}
            <div style={{ fontSize: 11, color: C.textDim, marginTop: 10 }}>{t('exch.detail.aiDisclaimer')}</div>
          </div>
        </div>
      )}

      {/* 방향 필터 */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        {DIRECTION_VALUES.map(f => (
          <button key={String(f.value)} onClick={() => setDirection(f.value)}
            style={{
              padding: '7px 14px', borderRadius: 20, fontSize: 13, cursor: 'pointer',
              border: direction === f.value ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
              background: direction === f.value ? C.activeBg : C.card,
              color: direction === f.value ? C.activeText : C.textMuted,
              fontWeight: direction === f.value ? 700 : 400,
            }}>
            {t(f.key)}
          </button>
        ))}
      </div>

      {/* 리뷰 제목 */}
      <h2 style={{ fontSize: 18, fontWeight: 700, color: C.textPrimary, marginBottom: 16 }}>
        {t('exch.detail.reviews')} ({reviewPage?.totalElements ?? 0})
      </h2>

      {reviewLoading && <p style={{ color: C.textMuted }}>{t('common.loading')}</p>}
      {!reviewLoading && reviews.length === 0 && (
        <p style={{ color: C.textMuted, padding: '20px 0' }}>{t('exch.empty')}</p>
      )}

      {/* 리뷰 목록 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {reviews.map(r => <ReviewCard key={r.id} review={r} univ={univ} />)}
      </div>
    </div>
  )
}

// ── 리뷰 카드 ────────────────────────────────────────────────
function ReviewCard({ review: r, univ }: { review: VideoReview; univ: ExchangeUniversity }) {
  const { t } = useI18n()
  const [expanded, setExpanded] = useState(false)
  const mismatch = isTagMismatch(r, univ)
  const langFlag = r.sourceLang ? (LANG_FLAG[r.sourceLang.slice(0, 2)] ?? '') : ''

  const hasCost = r.costTotal || r.costRent || r.costFood || r.costTransport
  const hasVisa = r.visaType || r.visaCost || r.visaDuration || r.visaProcessingDays
  const hasDorm = r.dormAvailable != null || r.dormType || r.dormPrice
  const hasAcademic = r.difficulty || r.workload || r.gpaRequirement || r.languageReq || r.deadlineInfo

  const detailedSummary = buildDetailedSummary(r, t)

  return (
    <div style={{ border: `1px solid ${C.border}`, borderRadius: 10, overflow: 'hidden', background: C.bg }}>
      {/* 카드 헤더 */}
      <div style={{ padding: '16px 20px', background: C.cardHeader, borderBottom: `1px solid ${C.border}` }}>
        {/* 배지 행 */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8, alignItems: 'center' }}>
          {r.direction === 'INBOUND' && <Badge text={t('exch.direction.inbound')} color="#60a5fa" />}
          {r.direction === 'OUTBOUND' && <Badge text={t('exch.direction.outbound')} color="#4ade80" />}
          {langFlag && <Badge text={langFlag} color="#94a3b8" />}
          {mismatch && <Badge text="⚠️" color="#fbbf24" />}
        </div>

        {/* 제목 + 평점 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
          <div style={{ flex: 1 }}>
            <a href={r.youtubeUrl} target="_blank" rel="noopener noreferrer"
              style={{ fontSize: 15, fontWeight: 700, color: '#93c5fd', textDecoration: 'none', lineHeight: 1.4 }}>
              ▶ {r.title}
            </a>
            <div style={{ fontSize: 12, color: C.textDim, marginTop: 4 }}>
              {r.channel}
              {r.publishedAt && ` · ${new Date(r.publishedAt).toLocaleDateString('ko-KR')}`}
              {r.country && ` · ${r.country}`}
              {r.city && ` ${r.city}`}
            </div>
          </div>
          {r.overallRating != null && (
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <span style={{ color: '#f59e0b', fontSize: 16 }}>
                {'★'.repeat(r.overallRating)}{'☆'.repeat(5 - r.overallRating)}
              </span>
              <div style={{ fontSize: 11, color: C.textDim }}>{r.overallRating}/5</div>
            </div>
          )}
        </div>

        {/* 태그 */}
        {r.tags && r.tags.length > 0 && (
          <div style={{ marginTop: 10 }}>
            {r.tags.map(tag => <Badge key={tag} text={tag} color="#a78bfa" />)}
          </div>
        )}
      </div>

      {/* 카드 본문 */}
      <div style={{ padding: '14px 20px' }}>
        {/* 확장 요약 (3~6문장) */}
        {detailedSummary && (
          <p style={{
            margin: '0 0 12px', fontSize: 14, color: C.textSec,
            lineHeight: 1.8, whiteSpace: 'pre-line',
          }}>
            {detailedSummary}
          </p>
        )}

        {/* 추천 여부 */}
        {r.recommend != null && (
          <div style={{
            display: 'inline-block', marginBottom: 12, padding: '6px 14px', borderRadius: 8,
            background: r.recommend ? C.recommendBg : C.notRecBg,
          }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: r.recommend ? C.recommendTx : C.notRecTx }}>
              {r.recommend ? '✓' : '✗'}
            </span>
            {r.overallTone && (
              <span style={{ fontSize: 12, color: C.textMuted, marginLeft: 8 }}>{r.overallTone}</span>
            )}
          </div>
        )}

        {/* 상세 정보 펼치기/접기 */}
        {(hasCost || hasVisa || hasDorm || hasAcademic) && (
          <>
            <button onClick={() => setExpanded(v => !v)}
              style={{
                display: 'block', width: '100%', textAlign: 'center', padding: '8px',
                background: C.card, border: `1px solid ${C.border}`, borderRadius: 6,
                cursor: 'pointer', fontSize: 13, color: C.textMuted, marginBottom: expanded ? 12 : 0,
              }}>
              {expanded ? `▲ ${t('univ.detail.cost')} / ${t('univ.detail.visa')} / ${t('univ.detail.dorm')} / ${t('univ.detail.academics')}` : `▼ ${t('univ.detail.cost')} / ${t('univ.detail.visa')} / ${t('univ.detail.dorm')} / ${t('univ.detail.academics')}`}
            </button>

            {expanded && (
              <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginTop: 4 }}>
                {hasAcademic && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>{t('exch.detail.academics')}</p>
                    <InfoRow label={t('exch.detail.difficulty')} value={r.difficulty ? `${r.difficulty}/5` : null} />
                    <InfoRow label={t('exch.detail.workload')} value={r.workload ? `${r.workload}/5` : null} />
                    <InfoRow label={t('exch.detail.gpa')} value={r.gpaRequirement} />
                    <InfoRow label={t('exch.detail.languageReq')} value={r.languageReq} />
                    <InfoRow label={t('exch.detail.deadline')} value={r.deadlineInfo} />
                  </div>
                )}
                {hasCost && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      {t('exch.detail.cost')}{r.costCurrency ? ` (${r.costCurrency})` : ''}
                    </p>
                    <InfoRow label={t('exch.detail.cost')} value={r.costTotal} />
                    <InfoRow label={t('exch.detail.rent')} value={r.costRent} />
                    <InfoRow label={t('exch.detail.food')} value={r.costFood} />
                    <InfoRow label={t('exch.detail.transport')} value={r.costTransport} />
                  </div>
                )}
                {(hasVisa || hasDorm) && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    {hasVisa && (
                      <>
                        <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>{t('exch.detail.visa')}</p>
                        <InfoRow label={t('exch.detail.type')} value={r.visaType} />
                        <InfoRow label={t('exch.detail.visaCost')} value={r.visaCost} />
                        <InfoRow label={t('exch.detail.duration')} value={r.visaDuration} />
                        <InfoRow label={t('exch.detail.processing')} value={r.visaProcessingDays} />
                      </>
                    )}
                    {hasDorm && (
                      <>
                        <p style={{ margin: `${hasVisa ? '12px' : '0'} 0 6px`, fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>{t('exch.detail.dorm')}</p>
                        <InfoRow label={t('exch.detail.dorm')} value={r.dormAvailable} />
                        <InfoRow label={t('exch.detail.type')} value={r.dormType} />
                        <InfoRow label={t('exch.detail.cost')} value={r.dormPrice} />
                      </>
                    )}
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
