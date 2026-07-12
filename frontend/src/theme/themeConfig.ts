export const THEME_STORAGE_KEY = 'ovlo-theme'

export type ThemePreference = 'system' | 'light' | 'dark'

// 저장된 선호가 없으면 dark 기본. 'system'은 사용자가 명시적으로 고를 때만.
export const DEFAULT_PREFERENCE: ThemePreference = 'dark'

// 하위호환: 해석된 기본 테마를 참조하던 코드용.
export const DEFAULT_THEME = 'dark'

// 페인트 전에 테마를 적용해 플래시 방지. 'system'은 prefers-color-scheme로 해석.
// (:root 기본값도 dark라 스크립트가 CSP로 차단돼도 dark로 안전 렌더 — 이중 방어)
export const themeInitScript = `
(function () {
  try {
    var pref = window.localStorage.getItem('${THEME_STORAGE_KEY}');
    if (pref !== 'light' && pref !== 'dark' && pref !== 'system') pref = '${DEFAULT_PREFERENCE}';
    var sysDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    var theme = pref === 'dark' || (pref === 'system' && sysDark) ? 'dark' : 'light';
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
  } catch (_) {
    document.documentElement.dataset.theme = '${DEFAULT_PREFERENCE}';
    document.documentElement.style.colorScheme = '${DEFAULT_PREFERENCE}';
  }
})();
`
