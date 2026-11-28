/** 법적 문서의 언어별 파일이 함께 쓰는 값. legalContent.ts 와 terms/*.ts 가 서로 import 하지 않도록 따로 둔다. */

/** 법적 문서에 적는 대외 문의처. Cloudflare Email Routing 으로 운영자 메일함에 전달된다(2026-11-27). */
export const LEGAL_CONTACT = 'support@mungji.com'

/**
 * true 면 문서 상단에 '초안' 안내를 띄운다. 방침 v4(2026-11-28)에서 AI 처리 업체·국외 이전·보관 기간을 적고 내렸다.
 */
export const LEGAL_DOCS_DRAFT = false

export const YOUTUBE_TERMS_URL = 'https://www.youtube.com/t/terms'
export const GOOGLE_PRIVACY_URL = 'https://policies.google.com/privacy'
