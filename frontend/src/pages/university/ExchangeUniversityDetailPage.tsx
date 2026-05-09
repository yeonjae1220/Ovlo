import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useExchangeUniversity, useExchangeUniversityReviews, useUniversityReportByExchangeUniv } from '../../hooks/useUniversity'
import type { VideoReview, ExchangeUniversity } from '../../types'

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

// ── 방향 필터 정의 ───────────────────────────────────────────
const DIRECTION_FILTERS = [
  { value: undefined,   label: '전체' },
  { value: 'INBOUND',  label: '🎓 이 대학으로 오는 교환' },
  { value: 'OUTBOUND', label: '✈️ 이 대학에서 나가는 교환' },
  { value: 'UNKNOWN',  label: '❓ 미분류' },
]

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
  const display = typeof value === 'boolean' ? (value ? '예' : '아니오') : String(value)
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
function buildDetailedSummary(r: VideoReview): string {
  const parts: string[] = []

  if (r.summary) parts.push(r.summary)

  // 만족도·분위기
  const ratingParts: string[] = []
  if (r.overallRating != null) ratingParts.push(`전반적인 만족도는 ${r.overallRating}/5점`)
  if (r.overallTone) ratingParts.push(`${r.overallTone}한 분위기`)
  if (r.recommend != null) ratingParts.push(r.recommend ? '교환학생으로 추천' : '교환학생으로 비추천')
  if (ratingParts.length > 0) parts.push(ratingParts.join('이며 ') + '입니다.')

  // 생활비
  if (r.costTotal || r.costRent || r.costFood) {
    const currency = r.costCurrency ? ` (${r.costCurrency})` : ''
    const costParts: string[] = []
    if (r.costTotal) costParts.push(`월 총 생활비 ${r.costTotal}`)
    if (r.costRent) costParts.push(`월세 ${r.costRent}`)
    if (r.costFood) costParts.push(`식비 ${r.costFood}`)
    if (r.costTransport) costParts.push(`교통비 ${r.costTransport}`)
    parts.push(`생활비는 ${costParts.join(', ')}${currency} 수준입니다.`)
  }

  // 학업
  if (r.difficulty != null || r.workload != null) {
    const acadParts: string[] = []
    if (r.difficulty != null) acadParts.push(`수업 난이도 ${r.difficulty}/5`)
    if (r.workload != null) acadParts.push(`학습량 ${r.workload}/5`)
    parts.push(`학업 측면에서는 ${acadParts.join(', ')} 수준입니다.`)
  }

  // 기숙사
  if (r.dormAvailable != null) {
    let dormLine = r.dormAvailable ? '기숙사 이용이 가능하며' : '학교 기숙사는 제공되지 않으며'
    if (r.dormType) dormLine += ` ${r.dormType} 형태`
    if (r.dormPrice) dormLine += `, 비용은 ${r.dormPrice}`
    parts.push(dormLine + '입니다.')
  }

  // 지원 요건·비자
  const reqParts: string[] = []
  if (r.visaType) reqParts.push(`${r.visaType} 비자`)
  if (r.languageReq) reqParts.push(`어학 성적 ${r.languageReq}`)
  if (r.gpaRequirement) reqParts.push(`GPA ${r.gpaRequirement}`)
  if (reqParts.length > 0) parts.push(`지원 요건으로 ${reqParts.join(', ')}이 필요합니다.`)

  return parts.join(' ')
}

// ── 메인 컴포넌트 ────────────────────────────────────────────
export default function ExchangeUniversityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const univId = Number(id)
  const [direction, setDirection] = useState<string | undefined>(undefined)
  const [reportLang, setReportLang] = useState('ko')

  const { data: univ, isLoading: univLoading } = useExchangeUniversity(univId)
  const { data: reviewPage, isLoading: reviewLoading } = useExchangeUniversityReviews(univId, direction)
  const { data: aiReport } = useUniversityReportByExchangeUniv(univId || null, reportLang)
  const reviews = reviewPage?.content ?? []

  if (univLoading) return <div style={{ padding: 40, color: C.textMuted }}>불러오는 중...</div>
  if (!univ) return <div style={{ padding: 40, color: C.notRecTx }}>대학 정보를 찾을 수 없습니다.</div>

  const starCount = univ.avgRating ? Math.round(univ.avgRating) : 0

  return (
    <div style={{ padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로가기 */}
      <button onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.textMuted, fontSize: 14, marginBottom: 20, padding: 0 }}>
        ← 목록으로
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
          <Badge text={`리뷰 ${univ.reviewCount}개`} color="#60a5fa" />
        </div>
        {univ.website && (
          <a href={univ.website} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-block', marginTop: 14, color: '#60a5fa', fontSize: 14, fontWeight: 500 }}>
            🔗 공식 홈페이지 →
          </a>
        )}
      </div>

      {/* AI 종합 보고서 */}
      {aiReport && (
        <div style={{ background: C.card, borderRadius: 12, marginBottom: 24, border: `1px solid ${C.border}`, overflow: 'hidden' }}>
          <div style={{ padding: '14px 20px', background: C.cardHeader, borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <span style={{ fontSize: 14, fontWeight: 700, color: C.textPrimary }}>✦ AI 종합 보고서</span>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              {['ko', 'en'].map(l => (
                <button key={l} onClick={() => setReportLang(l)} style={{
                  padding: '3px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
                  border: reportLang === l ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
                  background: reportLang === l ? C.activeBg : 'transparent',
                  color: reportLang === l ? C.activeText : C.textMuted,
                  fontWeight: reportLang === l ? 700 : 400,
                }}>
                  {l === 'ko' ? '한국어' : 'English'}
                </button>
              ))}
              <button onClick={() => navigate(`/university-reports/${aiReport.id}?lang=${reportLang}`)} style={{
                padding: '3px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
                border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.activeText,
              }}>
                전체 보기 →
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
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>💰 생활비</div>
                      <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>{parsed.costs.monthly_total ?? '-'}</div>
                      {parsed.costs.currency && <div style={{ fontSize: 11, color: C.textMuted }}>{parsed.costs.currency}</div>}
                      {parsed.costs.rent && <div style={{ fontSize: 11, color: C.textMuted }}>월세 {parsed.costs.rent}</div>}
                    </div>
                  )}
                  {parsed.housing && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>🏠 기숙사</div>
                      <div style={{ fontSize: 13, fontWeight: 600, color: parsed.housing.dorm_available ? '#4ade80' : '#f87171' }}>
                        {parsed.housing.dorm_available ? '입사 가능' : '입사 불가'}
                      </div>
                      {parsed.housing.dorm_price && <div style={{ fontSize: 11, color: C.textMuted }}>{parsed.housing.dorm_price}</div>}
                    </div>
                  )}
                  {parsed.visa && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>🛂 비자</div>
                      <div style={{ fontSize: 13, color: C.textSec, fontWeight: 600 }}>{parsed.visa.type ?? '-'}</div>
                      {parsed.visa.cost && <div style={{ fontSize: 11, color: C.textMuted }}>비용 {parsed.visa.cost}</div>}
                      {parsed.visa.processing_days && <div style={{ fontSize: 11, color: C.textMuted }}>처리 {parsed.visa.processing_days}</div>}
                    </div>
                  )}
                  {parsed.academics && (
                    <div style={{ flex: 1, minWidth: 130, background: C.bg, borderRadius: 8, padding: '10px 12px', border: `1px solid ${C.border}` }}>
                      <div style={{ fontSize: 10, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', marginBottom: 4 }}>📚 학업</div>
                      {parsed.academics.difficulty && <div style={{ fontSize: 12, color: C.textSec }}>난이도 {parsed.academics.difficulty}</div>}
                      {parsed.academics.gpa_req && <div style={{ fontSize: 12, color: C.textSec }}>GPA {parsed.academics.gpa_req}</div>}
                      {parsed.academics.language_req && <div style={{ fontSize: 12, color: C.textSec }}>어학 {parsed.academics.language_req}</div>}
                    </div>
                  )}
                </div>
              )
            })()}
            <div style={{ fontSize: 11, color: C.textDim }}>* AI가 영상 후기를 요약한 가이드입니다. 정확하지 않을 수 있습니다.</div>
          </div>
        </div>
      )}

      {/* 방향 필터 */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        {DIRECTION_FILTERS.map(f => (
          <button key={String(f.value)} onClick={() => setDirection(f.value)}
            style={{
              padding: '7px 14px', borderRadius: 20, fontSize: 13, cursor: 'pointer',
              border: direction === f.value ? `2px solid ${C.activeBorder}` : `1px solid ${C.borderLight}`,
              background: direction === f.value ? C.activeBg : C.card,
              color: direction === f.value ? C.activeText : C.textMuted,
              fontWeight: direction === f.value ? 700 : 400,
            }}>
            {f.label}
          </button>
        ))}
      </div>

      {/* 리뷰 제목 */}
      <h2 style={{ fontSize: 18, fontWeight: 700, color: C.textPrimary, marginBottom: 16 }}>
        영상 리뷰{' '}
        {!reviewLoading && (
          <span style={{ fontSize: 14, fontWeight: 400, color: C.textDim }}>
            ({reviewPage?.totalElements ?? 0}개)
          </span>
        )}
      </h2>

      {reviewLoading && <p style={{ color: C.textMuted }}>리뷰 불러오는 중...</p>}
      {!reviewLoading && reviews.length === 0 && (
        <p style={{ color: C.textMuted, padding: '20px 0' }}>해당하는 리뷰가 없습니다.</p>
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
  const [expanded, setExpanded] = useState(false)
  const mismatch = isTagMismatch(r, univ)
  const langFlag = r.sourceLang ? (LANG_FLAG[r.sourceLang.slice(0, 2)] ?? '') : ''

  const hasCost = r.costTotal || r.costRent || r.costFood || r.costTransport
  const hasVisa = r.visaType || r.visaCost || r.visaDuration || r.visaProcessingDays
  const hasDorm = r.dormAvailable != null || r.dormType || r.dormPrice
  const hasAcademic = r.difficulty || r.workload || r.gpaRequirement || r.languageReq || r.deadlineInfo

  const detailedSummary = buildDetailedSummary(r)

  return (
    <div style={{ border: `1px solid ${C.border}`, borderRadius: 10, overflow: 'hidden', background: C.bg }}>
      {/* 카드 헤더 */}
      <div style={{ padding: '16px 20px', background: C.cardHeader, borderBottom: `1px solid ${C.border}` }}>
        {/* 배지 행 */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8, alignItems: 'center' }}>
          {r.direction === 'INBOUND' && <Badge text="🎓 이 대학으로 오는 교환" color="#60a5fa" />}
          {r.direction === 'OUTBOUND' && <Badge text="✈️ 이 대학에서 나가는 교환" color="#4ade80" />}
          {langFlag && <Badge text={langFlag} color="#94a3b8" />}
          {mismatch && <Badge text="⚠️ 다른 대학 관련 영상일 수 있음" color="#fbbf24" />}
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
              <div style={{ fontSize: 11, color: C.textDim }}>종합 {r.overallRating}/5</div>
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
              {r.recommend ? '✓ 추천' : '✗ 비추천'}
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
              {expanded ? '▲ 상세 정보 접기' : '▼ 비용·비자·기숙사·학업 상세 보기'}
            </button>

            {expanded && (
              <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginTop: 4 }}>
                {hasAcademic && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>학업</p>
                    <InfoRow label="난이도" value={r.difficulty ? `${r.difficulty}/5` : null} />
                    <InfoRow label="학습량" value={r.workload ? `${r.workload}/5` : null} />
                    <InfoRow label="GPA 요건" value={r.gpaRequirement} />
                    <InfoRow label="어학 요건" value={r.languageReq} />
                    <InfoRow label="지원 마감" value={r.deadlineInfo} />
                  </div>
                )}
                {hasCost && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      생활비{r.costCurrency ? ` (${r.costCurrency})` : ''}
                    </p>
                    <InfoRow label="총 생활비" value={r.costTotal} />
                    <InfoRow label="월세" value={r.costRent} />
                    <InfoRow label="식비" value={r.costFood} />
                    <InfoRow label="교통비" value={r.costTransport} />
                  </div>
                )}
                {(hasVisa || hasDorm) && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    {hasVisa && (
                      <>
                        <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>비자</p>
                        <InfoRow label="종류" value={r.visaType} />
                        <InfoRow label="비용" value={r.visaCost} />
                        <InfoRow label="기간" value={r.visaDuration} />
                        <InfoRow label="처리 기간" value={r.visaProcessingDays} />
                      </>
                    )}
                    {hasDorm && (
                      <>
                        <p style={{ margin: `${hasVisa ? '12px' : '0'} 0 6px`, fontSize: 11, fontWeight: 700, color: C.textDim, textTransform: 'uppercase', letterSpacing: 0.5 }}>기숙사</p>
                        <InfoRow label="가능 여부" value={r.dormAvailable} />
                        <InfoRow label="유형" value={r.dormType} />
                        <InfoRow label="비용" value={r.dormPrice} />
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
