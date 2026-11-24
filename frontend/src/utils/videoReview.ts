import type { VideoReview } from '../types'

const hasText = (value: string | null | undefined) => typeof value === 'string' && value.trim() !== ''

/**
 * 영상 행에 분석 값(요약·평점·비용 등)이 하나라도 있는지.
 * 없으면 링크·제목·채널뿐인 "영상 링크"이므로 후기 카드가 아니라 영상 링크 구역에서 보여 준다.
 */
export function hasReviewAnalysis(r: VideoReview): boolean {
  return (
    hasText(r.summary) || hasText(r.exchangeInfo) || hasText(r.overallTone) ||
    r.overallRating != null || r.recommend != null || r.difficulty != null || r.workload != null ||
    r.dormAvailable != null || hasText(r.dormType) || hasText(r.dormPrice) ||
    hasText(r.costTotal) || hasText(r.costRent) || hasText(r.costFood) || hasText(r.costTransport) ||
    hasText(r.visaType) || hasText(r.visaCost) || hasText(r.visaDuration) || hasText(r.visaProcessingDays) ||
    hasText(r.gpaRequirement) || hasText(r.languageReq) || hasText(r.deadlineInfo) ||
    (r.tags?.length ?? 0) > 0
  )
}
