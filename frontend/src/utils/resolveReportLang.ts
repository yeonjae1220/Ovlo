import { resolveUiLang, SUPPORTED_UI_LANGUAGES } from '../i18n/messages'

/**
 * Picks the best available language for a university report.
 *
 * Priority:
 * 1. userLang  — user's explicit UI language preference
 * 2. fallback  — navigator.language or account secondary
 * 3. supportedLangs[0] — original content language (shown as-is)
 */
export function resolveReportLang(
  userLang: string,
  supportedLangs: string[],
  fallbackLang?: string,
): string {
  const supported = supportedLangs.map((l) => l.toLowerCase())

  if (supported.includes(userLang.toLowerCase())) return userLang

  if (fallbackLang) {
    const fb = resolveUiLang(fallbackLang)
    if (supported.includes(fb)) return fb
  }

  // prefer any SUPPORTED_UI_LANGUAGES entry available in the report
  for (const uiLang of SUPPORTED_UI_LANGUAGES) {
    if (supported.includes(uiLang)) return uiLang
  }

  return supported[0] ?? 'en'
}
