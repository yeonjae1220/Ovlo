import type { UiLanguage } from '../i18n/messages'

interface UniversityNames {
  nameEn: string
  nameKo?: string | null
  nameLocal?: string | null
}

export interface UniversityDisplayName {
  /** 큰 글씨로 보여 줄 이름 — 항상 UI 언어 이름, 없으면 영문 */
  primary: string
  /** 보조로 작게 보여 줄 이름 — primary 가 영문이 아닐 때의 영문명. 같으면 null */
  secondary: string | null
}

/**
 * 한 목록 안에서 대학명 언어가 섞이지 않게, UI 언어 기준으로 표시명을 정한다.
 *
 * - ko: 한글명(nameKo) → 없으면 영문
 * - en: 영문명
 * - 그 외: 백엔드가 lang 으로 내려준 nameLocal → 없으면 영문
 *
 * nameLocal 은 서버가 요청 lang 으로 계산해 주므로 ko/en 도 보통 들어 있지만,
 * 옛 응답·캐시(lang 없이 받은 행)에도 동작하도록 nameKo/nameEn 을 직접 본다.
 */
export function resolveUniversityDisplayName(u: UniversityNames, language: UiLanguage): UniversityDisplayName {
  const localized = language === 'ko' ? u.nameKo : language === 'en' ? u.nameEn : u.nameLocal
  const primary = (localized || u.nameEn || '').trim() || u.nameEn
  const secondary = primary !== u.nameEn && u.nameEn ? u.nameEn : null
  return { primary, secondary }
}
