import apiClient from '../utils/axios'

/** 백엔드 domain/help 와 1:1. */
export type HelpAnswerOutcome = 'ANSWERED' | 'NO_GROUNDS' | 'OUT_OF_SCOPE'

export interface HelpArticle {
  id: string
  title: string
  body: string
}

export interface HelpCitation {
  index: number
  articleId: string
  title: string
}

/** answerText 의 [N] 은 citations 의 index 와 같다. ANSWERED 가 아니면 answerText 는 null. */
export interface HelpAnswer {
  outcome: HelpAnswerOutcome
  lang: string
  answerText: string | null
  citations: HelpCitation[]
}

export const helpApi = {
  /** GET /help/articles — 공개 */
  articles: (signal?: AbortSignal) =>
    apiClient.get<HelpArticle[]>('/help/articles', { signal }).then((r) => r.data),
  /** POST /help/ask — 로그인 필요 */
  ask: (question: string, lang: string) =>
    apiClient.post<HelpAnswer>('/help/ask', { question, lang }).then((r) => r.data),
}
