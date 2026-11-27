/** 법적 문서의 언어별 파일이 함께 쓰는 값. legalContent.ts 와 terms/*.ts 가 서로 import 하지 않도록 따로 둔다. */

/** 법적 문서에 적는 대외 문의처. Cloudflare Email Routing 으로 운영자 메일함에 전달된다(2026-11-27). */
export const LEGAL_CONTACT = 'support@mungji.com'

/**
 * 외부 AI 처리 업체·보관 정책이 아직 확정되지 않아 문서 상단에 '초안' 안내를 띄운다.
 * 확정하면 false 로 바꾸고 모든 언어의 version·updatedAt 을 함께 올린다.
 */
export const LEGAL_DOCS_DRAFT = true

export const YOUTUBE_TERMS_URL = 'https://www.youtube.com/t/terms'
export const GOOGLE_PRIVACY_URL = 'https://policies.google.com/privacy'
