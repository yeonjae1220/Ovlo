import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useExchangeUniversity, useExchangeUniversityReviews } from '../../hooks/useUniversity'
import type { VideoReview, ExchangeUniversity } from '../../types'

// ── 언어 국기 ────────────────────────────────────────────────────
const LANG_FLAG: Record<string, string> = {
  ko: '🇰🇷', ja: '🇯🇵', en: '🇺🇸', zh: '🇨🇳', fr: '🇫🇷',
  de: '🇩🇪', es: '🇪🇸', pt: '🇧🇷', vi: '🇻🇳', th: '🇹🇭',
}

// ── 방향 필터 정의 ───────────────────────────────────────────────
const DIRECTION_FILTERS = [
  { value: undefined,    label: '전체' },
  { value: 'INBOUND',   label: '🎓 이 대학으로 오는 교환' },
  { value: 'OUTBOUND',  label: '✈️ 이 대학에서 나가는 교환' },
  { value: 'UNKNOWN',   label: '❓ 미분류' },
]

// ── 소컴포넌트 ──────────────────────────────────────────────────
const Badge = ({ text, color = '#2563eb' }: { text: string; color?: string }) => (
  <span style={{
    display: 'inline-block', padding: '2px 10px', borderRadius: 20,
    background: color + '18', color, fontSize: 12, fontWeight: 600, marginRight: 4, marginBottom: 4,
  }}>{text}</span>
)

const InfoRow = ({ label, value }: { label: string; value?: string | number | boolean | null }) => {
  if (value === undefined || value === null || value === '') return null
  const display = typeof value === 'boolean' ? (value ? '예' : '아니오') : String(value)
  return (
    <div style={{ display: 'flex', gap: 12, padding: '7px 0', borderBottom: '1px solid #f3f4f6' }}>
      <span style={{ minWidth: 110, color: '#9ca3af', fontSize: 13, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 14, color: '#111827', wordBreak: 'break-word' }}>{display}</span>
    </div>
  )
}

// ── 태그 불일치 감지 ─────────────────────────────────────────────
function isTagMismatch(review: VideoReview, univ: ExchangeUniversity): boolean {
  const tags = review.tags ?? []
  if (tags.length === 0) return false
  const nameKo = univ.nameKo ?? ''
  const nameEn = univ.nameEn ?? ''
  return !tags.some(t =>
    (nameKo && t.includes(nameKo)) || (nameEn && t.toLowerCase().includes(nameEn.toLowerCase()))
  )
}

// ── 메인 컴포넌트 ────────────────────────────────────────────────
export default function ExchangeUniversityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const univId = Number(id)
  const [direction, setDirection] = useState<string | undefined>(undefined)

  const { data: univ, isLoading: univLoading } = useExchangeUniversity(univId)
  const { data: reviewPage, isLoading: reviewLoading } = useExchangeUniversityReviews(univId, direction)
  const reviews = reviewPage?.content ?? []

  if (univLoading) return <div style={{ padding: 40, color: '#9ca3af' }}>불러오는 중...</div>
  if (!univ) return <div style={{ padding: 40, color: '#dc2626' }}>대학 정보를 찾을 수 없습니다.</div>

  const starCount = univ.avgRating ? Math.round(univ.avgRating) : 0

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로가기 */}
      <button onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6b7280', fontSize: 14, marginBottom: 20, padding: 0 }}>
        ← 목록으로
      </button>

      {/* 대학 헤더 */}
      <div style={{ padding: '24px', background: '#f8fafc', borderRadius: 12, marginBottom: 28 }}>
        <h1 style={{ margin: '0 0 4px', fontSize: 26, fontWeight: 800, color: '#111827' }}>
          {univ.nameKo || univ.nameEn}
        </h1>
        {univ.nameKo && univ.nameEn && (
          <p style={{ margin: '0 0 12px', color: '#6b7280', fontSize: 15 }}>{univ.nameEn}</p>
        )}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
          <Badge text={`${univ.country} · ${univ.city}`} color="#374151" />
          {univ.avgRating != null && (
            <>
              <span style={{ color: '#f59e0b', fontSize: 18, letterSpacing: 1 }}>
                {'★'.repeat(starCount)}{'☆'.repeat(5 - starCount)}
              </span>
              <span style={{ fontSize: 14, fontWeight: 700, color: '#374151' }}>
                {univ.avgRating.toFixed(1)}
              </span>
            </>
          )}
          <Badge text={`리뷰 ${univ.reviewCount}개`} color="#2563eb" />
        </div>
        {univ.website && (
          <a href={univ.website} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-block', marginTop: 14, color: '#2563eb', fontSize: 14, fontWeight: 500 }}>
            🔗 공식 홈페이지 →
          </a>
        )}
      </div>

      {/* 방향 필터 */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        {DIRECTION_FILTERS.map(f => (
          <button key={String(f.value)} onClick={() => setDirection(f.value)}
            style={{
              padding: '7px 14px', borderRadius: 20, fontSize: 13, cursor: 'pointer',
              border: direction === f.value ? '2px solid #2563eb' : '1px solid #e5e7eb',
              background: direction === f.value ? '#eff6ff' : '#fff',
              color: direction === f.value ? '#2563eb' : '#374151',
              fontWeight: direction === f.value ? 700 : 400,
            }}>
            {f.label}
          </button>
        ))}
      </div>

      {/* 리뷰 제목 */}
      <h2 style={{ fontSize: 18, fontWeight: 700, color: '#111827', marginBottom: 16 }}>
        영상 리뷰{' '}
        {!reviewLoading && (
          <span style={{ fontSize: 14, fontWeight: 400, color: '#9ca3af' }}>
            ({reviewPage?.totalElements ?? 0}개)
          </span>
        )}
      </h2>

      {reviewLoading && <p style={{ color: '#9ca3af' }}>리뷰 불러오는 중...</p>}
      {!reviewLoading && reviews.length === 0 && (
        <p style={{ color: '#9ca3af', padding: '20px 0' }}>해당하는 리뷰가 없습니다.</p>
      )}

      {/* 리뷰 목록 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {reviews.map(r => <ReviewCard key={r.id} review={r} univ={univ} />)}
      </div>
    </div>
  )
}

// ── 리뷰 카드 ────────────────────────────────────────────────────
function ReviewCard({ review: r, univ }: { review: VideoReview; univ: ExchangeUniversity }) {
  const [expanded, setExpanded] = useState(false)
  const mismatch = isTagMismatch(r, univ)
  const langFlag = r.sourceLang ? (LANG_FLAG[r.sourceLang.slice(0, 2)] ?? '') : ''

  const hasCost = r.costTotal || r.costRent || r.costFood || r.costTransport
  const hasVisa = r.visaType || r.visaCost || r.visaDuration || r.visaProcessingDays
  const hasDorm = r.dormAvailable != null || r.dormType || r.dormPrice
  const hasAcademic = r.difficulty || r.workload || r.gpaRequirement || r.languageReq || r.deadlineInfo

  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: 10, overflow: 'hidden' }}>
      {/* 카드 헤더 */}
      <div style={{ padding: '16px 20px', background: '#fafafa', borderBottom: '1px solid #f3f4f6' }}>
        {/* 배지 행 */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8, alignItems: 'center' }}>
          {r.direction === 'INBOUND' && <Badge text="🎓 이 대학으로 오는 교환" color="#2563eb" />}
          {r.direction === 'OUTBOUND' && <Badge text="✈️ 이 대학에서 나가는 교환" color="#16a34a" />}
          {langFlag && <Badge text={langFlag} color="#6b7280" />}
          {mismatch && <Badge text="⚠️ 다른 대학 관련 영상일 수 있음" color="#d97706" />}
        </div>

        {/* 제목 + 평점 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
          <div style={{ flex: 1 }}>
            <a href={r.youtubeUrl} target="_blank" rel="noopener noreferrer"
              style={{ fontSize: 15, fontWeight: 700, color: '#1d4ed8', textDecoration: 'none', lineHeight: 1.4 }}>
              ▶ {r.title}
            </a>
            <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>
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
              <div style={{ fontSize: 11, color: '#9ca3af' }}>종합 {r.overallRating}/5</div>
            </div>
          )}
        </div>

        {/* 태그 */}
        {r.tags && r.tags.length > 0 && (
          <div style={{ marginTop: 10 }}>
            {r.tags.map(tag => <Badge key={tag} text={tag} color="#7c3aed" />)}
          </div>
        )}
      </div>

      {/* 카드 본문 */}
      <div style={{ padding: '14px 20px' }}>
        {/* 요약 */}
        {r.summary && (
          <p style={{ margin: '0 0 12px', fontSize: 14, color: '#374151', lineHeight: 1.7, whiteSpace: 'pre-line' }}>
            {r.summary}
          </p>
        )}

        {/* 추천 여부 */}
        {r.recommend != null && (
          <div style={{
            display: 'inline-block', marginBottom: 12, padding: '6px 14px', borderRadius: 8,
            background: r.recommend ? '#f0fdf4' : '#fef2f2',
          }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: r.recommend ? '#16a34a' : '#dc2626' }}>
              {r.recommend ? '✓ 추천' : '✗ 비추천'}
            </span>
            {r.overallTone && (
              <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{r.overallTone}</span>
            )}
          </div>
        )}

        {/* 상세 정보 펼치기/접기 */}
        {(hasCost || hasVisa || hasDorm || hasAcademic) && (
          <>
            <button onClick={() => setExpanded(v => !v)}
              style={{
                display: 'block', width: '100%', textAlign: 'center', padding: '8px',
                background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: 6,
                cursor: 'pointer', fontSize: 13, color: '#6b7280', marginBottom: expanded ? 12 : 0,
              }}>
              {expanded ? '▲ 상세 정보 접기' : '▼ 비용·비자·기숙사·학업 상세 보기'}
            </button>

            {expanded && (
              <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginTop: 4 }}>
                {hasAcademic && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 }}>학업</p>
                    <InfoRow label="난이도" value={r.difficulty ? `${r.difficulty}/5` : null} />
                    <InfoRow label="학습량" value={r.workload ? `${r.workload}/5` : null} />
                    <InfoRow label="GPA 요건" value={r.gpaRequirement} />
                    <InfoRow label="어학 요건" value={r.languageReq} />
                    <InfoRow label="지원 마감" value={r.deadlineInfo} />
                  </div>
                )}
                {hasCost && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 }}>
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
                        <p style={{ margin: '0 0 6px', fontSize: 11, fontWeight: 700, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 }}>비자</p>
                        <InfoRow label="종류" value={r.visaType} />
                        <InfoRow label="비용" value={r.visaCost} />
                        <InfoRow label="기간" value={r.visaDuration} />
                        <InfoRow label="처리 기간" value={r.visaProcessingDays} />
                      </>
                    )}
                    {hasDorm && (
                      <>
                        <p style={{ margin: `${hasVisa ? '12px' : '0'} 0 6px`, fontSize: 11, fontWeight: 700, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 }}>기숙사</p>
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
