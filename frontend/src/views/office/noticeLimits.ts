// Keep in sync with NoticeAnswerLimits.MAX_BODY_CHARS (Java UTF-16 length).
export const NOTICE_ANSWER_BODY_CHARS = 3000

export function exceedsNoticeAnswerLimit(body: string): boolean {
  // Notice.requireText uses String.trim(): only U+0000..U+0020 at the edges.
  const savedBody = body.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, '')
  return savedBody.length > NOTICE_ANSWER_BODY_CHARS
}
