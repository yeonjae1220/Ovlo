import { SUPPORTED_UI_LANGUAGES, type UiLanguage } from '../i18n/messages'

/**
 * 국제처 파일럿 집중용 화면 숨김 스위치 (2026-11-27).
 *
 * 멘토링 의견("작게 시작")에 따라 해외 파견(교환대학·리포트) 진입점과 일부 UI 언어를
 * **화면에서만** 숨긴다. 페이지·API·데이터·번역 문구는 그대로 남아 있어 주소로는 계속 열리고,
 * 이미 숨긴 언어를 고른 사용자도 그 언어로 계속 쓴다.
 *
 * 복구: 아래 세 값만 바꾸면 된다.
 *   SHOW_OUTBOUND_EXCHANGE = true
 *   CATALOG_COUNTRY_LOCK   = null
 *   HIDDEN_UI_LANGUAGES    = []
 */
export const SHOW_OUTBOUND_EXCHANGE = false

/**
 * 대학 정보(카탈로그) 메뉴는 보이되 이 나라 대학만 보여 준다 (2026-11-27, 임시).
 * null 이면 나라 제한이 없다. 값이 있으면 SHOW_OUTBOUND_EXCHANGE 가 false 여도 메뉴가 보인다.
 * 화면 제한일 뿐이다 — API 는 다른 나라도 그대로 응답한다.
 */
export const CATALOG_COUNTRY_LOCK: string | null = 'KR'

/**
 * 카탈로그 메뉴·제목·설명 문구 키. 나라를 고정하면 `<key>.<나라코드 소문자>` 문구를 쓴다
 * (예: 'exch.title' → 'exch.title.kr'). CATALOG_COUNTRY_LOCK 을 null 로 되돌리면 원래 문구로 돌아간다.
 */
export function catalogTextKey(key: string): string {
  return CATALOG_COUNTRY_LOCK === null ? key : `${key}.${CATALOG_COUNTRY_LOCK.toLowerCase()}`
}

/** 상단·모바일 '더보기'에 대학 정보 메뉴를 보일지. */
export const SHOW_CATALOG_MENU = SHOW_OUTBOUND_EXCHANGE || CATALOG_COUNTRY_LOCK !== null

/** 언어 선택 UI 에서 숨길 언어. 번역 문구는 messages.ts 에 그대로 있다. */
export const HIDDEN_UI_LANGUAGES: readonly UiLanguage[] = ['es', 'fr', 'de', 'pt']

/** 언어 선택 UI 에 보이는 언어. 순서는 SUPPORTED_UI_LANGUAGES 를 따른다. */
export const VISIBLE_UI_LANGUAGES: readonly UiLanguage[] = SUPPORTED_UI_LANGUAGES.filter(
  (lang) => !HIDDEN_UI_LANGUAGES.includes(lang),
)

/**
 * 언어 선택지. 현재 언어가 숨김 대상이어도 선택지에 남겨, 선택 상자가 엉뚱한 값을
 * 보여 주거나 사용자가 자기 언어를 잃지 않게 한다.
 */
export function uiLanguageOptions(current: UiLanguage): readonly UiLanguage[] {
  return VISIBLE_UI_LANGUAGES.includes(current)
    ? VISIBLE_UI_LANGUAGES
    : SUPPORTED_UI_LANGUAGES.filter((lang) => lang === current || VISIBLE_UI_LANGUAGES.includes(lang))
}
