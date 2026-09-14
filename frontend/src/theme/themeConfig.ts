export const THEME_STORAGE_KEY = 'ovlo-theme'

export type ThemePreference = 'system' | 'light' | 'dark'

// 교환학생 서비스의 개방감과 정보 가독성을 위해 신규 사용자는 light로 시작한다.
// 저장된 선호가 있으면 기존 선택을 그대로 존중한다.
export const DEFAULT_PREFERENCE: ThemePreference = 'light'

// 하위호환: 해석된 기본 테마를 참조하던 코드용.
export const DEFAULT_THEME = 'light'

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
