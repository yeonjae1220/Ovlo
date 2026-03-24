import { useParams, useNavigate } from 'react-router-dom'
import { useExchangeUniversity, useExchangeUniversityReviews } from '../../hooks/useUniversity'

const badge = (label: string, color = '#2563eb') => (
  <span style={{
    display: 'inline-block', padding: '2px 10px', borderRadius: 20,
    background: color + '18', color, fontSize: 12, fontWeight: 600, marginRight: 6,
  }}>
    {label}
  </span>
)

const InfoRow = ({ label, value }: { label: string; value?: string | number | boolean | null }) => {
  if (value === undefined || value === null || value === '') return null
  const display = typeof value === 'boolean' ? (value ? '예' : '아니오') : String(value)
  return (
    <div style={{ display: 'flex', gap: 12, padding: '8px 0', borderBottom: '1px solid #f3f4f6' }}>
      <span style={{ minWidth: 120, color: '#9ca3af', fontSize: 13 }}>{label}</span>
      <span style={{ fontSize: 14, color: '#111827' }}>{display}</span>
    </div>
  )
}

export default function ExchangeUniversityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const univId = Number(id)

  const { data: univ, isLoading: univLoading } = useExchangeUniversity(univId)
  const { data: reviewPage, isLoading: reviewLoading } = useExchangeUniversityReviews(univId)
  const reviews = reviewPage?.content ?? []

  if (univLoading) return <div style={{ padding: 40, color: '#9ca3af' }}>불러오는 중...</div>
  if (!univ) return <div style={{ padding: 40, color: '#dc2626' }}>대학 정보를 찾을 수 없습니다.</div>

  const starCount = univ.avgRating ? Math.round(univ.avgRating) : 0

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      {/* 뒤로 가기 */}
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6b7280', fontSize: 14, marginBottom: 20, padding: 0 }}
      >
        ← 목록으로
      </button>

      {/* 헤더 */}
      <div style={{ padding: '24px', background: '#f8fafc', borderRadius: 12, marginBottom: 28 }}>
        <h1 style={{ margin: '0 0 4px', fontSize: 26, fontWeight: 800, color: '#111827' }}>
          {univ.nameKo || univ.nameEn}
        </h1>
        {univ.nameKo && univ.nameEn && (
          <p style={{ margin: '0 0 12px', color: '#6b7280', fontSize: 15 }}>{univ.nameEn}</p>
        )}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          {badge(`${univ.country} · ${univ.city}`, '#374151')}
          {univ.avgRating !== undefined && univ.avgRating !== null && (
            <>
              <span style={{ color: '#f59e0b', fontSize: 18, letterSpacing: 1 }}>
                {'★'.repeat(starCount)}{'☆'.repeat(5 - starCount)}
              </span>
              <span style={{ fontSize: 14, fontWeight: 700, color: '#374151' }}>
                {univ.avgRating.toFixed(1)}
              </span>
            </>
          )}
          {badge(`리뷰 ${univ.reviewCount}개`, '#2563eb')}
        </div>
        {univ.website && (
          <a
            href={univ.website}
            target="_blank"
            rel="noopener noreferrer"
            style={{ display: 'inline-block', marginTop: 14, color: '#2563eb', fontSize: 14, fontWeight: 500 }}
          >
            🔗 공식 홈페이지 →
          </a>
        )}
      </div>

      {/* 영상 리뷰 */}
      <h2 style={{ fontSize: 18, fontWeight: 700, color: '#111827', marginBottom: 16 }}>
        영상 리뷰 {reviewLoading ? '' : `(${reviewPage?.totalElements ?? 0}개)`}
      </h2>

      {reviewLoading && <p style={{ color: '#9ca3af' }}>리뷰 불러오는 중...</p>}
      {!reviewLoading && reviews.length === 0 && (
        <p style={{ color: '#9ca3af', padding: '20px 0' }}>등록된 리뷰가 없습니다.</p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {reviews.map((r) => (
          <div key={r.id} style={{ border: '1px solid #e5e7eb', borderRadius: 10, overflow: 'hidden' }}>
            {/* 리뷰 헤더 */}
            <div style={{ padding: '16px 20px', background: '#fafafa', borderBottom: '1px solid #f3f4f6' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <a
                    href={r.youtubeUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ fontSize: 15, fontWeight: 700, color: '#1d4ed8', textDecoration: 'none' }}
                  >
                    ▶ {r.title}
                  </a>
                  <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>
                    {r.channel} · {r.publishedAt ? new Date(r.publishedAt).toLocaleDateString('ko-KR') : ''}
                    {r.sourceLang && ` · ${r.sourceLang}`}
                  </div>
                </div>
                {r.overallRating && (
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <span style={{ color: '#f59e0b', fontSize: 16 }}>
                      {'★'.repeat(r.overallRating)}{'☆'.repeat(5 - r.overallRating)}
                    </span>
                    <div style={{ fontSize: 11, color: '#9ca3af' }}>종합 {r.overallRating}/5</div>
                  </div>
                )}
              </div>
              {r.tags && r.tags.length > 0 && (
                <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                  {r.tags.map((tag) => badge(tag, '#7c3aed'))}
                </div>
              )}
            </div>

            {/* 리뷰 상세 */}
            <div style={{ padding: '12px 20px' }}>
              {r.summary && (
                <p style={{ margin: '0 0 12px', fontSize: 14, color: '#374151', lineHeight: 1.6 }}>{r.summary}</p>
              )}
              {r.exchangeInfo && (
                <p style={{ margin: '0 0 12px', fontSize: 13, color: '#6b7280', lineHeight: 1.5 }}>{r.exchangeInfo}</p>
              )}

              <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', marginTop: 8 }}>
                {/* 학업 정보 */}
                {(r.difficulty || r.workload || r.gpaRequirement || r.languageReq) && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 12, fontWeight: 700, color: '#374151', textTransform: 'uppercase', letterSpacing: 0.5 }}>학업</p>
                    <InfoRow label="난이도" value={r.difficulty ? `${r.difficulty}/5` : undefined} />
                    <InfoRow label="학습량" value={r.workload ? `${r.workload}/5` : undefined} />
                    <InfoRow label="GPA 요건" value={r.gpaRequirement} />
                    <InfoRow label="어학 요건" value={r.languageReq} />
                    <InfoRow label="지원 마감" value={r.deadlineInfo} />
                  </div>
                )}

                {/* 생활비 */}
                {(r.costTotal || r.costRent || r.costFood) && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 12, fontWeight: 700, color: '#374151', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      생활비 {r.costCurrency ? `(${r.costCurrency})` : ''}
                    </p>
                    <InfoRow label="총 생활비" value={r.costTotal} />
                    <InfoRow label="월세" value={r.costRent} />
                    <InfoRow label="식비" value={r.costFood} />
                    <InfoRow label="교통비" value={r.costTransport} />
                  </div>
                )}

                {/* 비자 & 기숙사 */}
                {(r.visaType || r.dormAvailable !== undefined) && (
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <p style={{ margin: '0 0 6px', fontSize: 12, fontWeight: 700, color: '#374151', textTransform: 'uppercase', letterSpacing: 0.5 }}>비자 · 숙소</p>
                    <InfoRow label="비자 종류" value={r.visaType} />
                    <InfoRow label="비자 비용" value={r.visaCost} />
                    <InfoRow label="비자 기간" value={r.visaDuration} />
                    <InfoRow label="처리 기간" value={r.visaProcessingDays} />
                    <InfoRow label="기숙사 가능" value={r.dormAvailable} />
                    <InfoRow label="기숙사 유형" value={r.dormType} />
                    <InfoRow label="기숙사 비용" value={r.dormPrice} />
                  </div>
                )}
              </div>

              {/* 추천 여부 */}
              {r.recommend !== undefined && (
                <div style={{ marginTop: 12, padding: '8px 14px', borderRadius: 8, background: r.recommend ? '#f0fdf4' : '#fef2f2', display: 'inline-block' }}>
                  <span style={{ fontSize: 13, fontWeight: 600, color: r.recommend ? '#16a34a' : '#dc2626' }}>
                    {r.recommend ? '✓ 추천' : '✗ 비추천'}
                  </span>
                  {r.overallTone && (
                    <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{r.overallTone}</span>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
