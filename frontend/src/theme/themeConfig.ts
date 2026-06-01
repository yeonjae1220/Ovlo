export const THEME_STORAGE_KEY = 'ovlo-theme'
export const DEFAULT_THEME = 'dark'

export const themeInitScript = `
(function () {
  try {
    var stored = window.localStorage.getItem('${THEME_STORAGE_KEY}');
    var theme = stored === 'light' || stored === 'dark' ? stored : '${DEFAULT_THEME}';
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
  } catch (_) {
    document.documentElement.dataset.theme = '${DEFAULT_THEME}';
    document.documentElement.style.colorScheme = '${DEFAULT_THEME}';
  }
})();
`
