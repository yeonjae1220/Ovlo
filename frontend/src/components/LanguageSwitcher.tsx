const LANG_LABELS: Record<string, string> = {
  ko: '한국어',
  en: 'English',
  ja: '日本語',
  zh: '中文',
  de: 'Deutsch',
  fr: 'Français',
  es: 'Español',
}

interface Props {
  langs: string[]
  current: string
  onChange: (lang: string) => void
}

export default function LanguageSwitcher({ langs, current, onChange }: Props) {
  if (langs.length <= 1) return null

  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
      {langs.map((lang) => (
        <button
          key={lang}
          onClick={() => onChange(lang)}
          style={{
            padding: '6px 14px',
            borderRadius: 20,
            border: '1px solid',
            borderColor: lang === current ? '#4f46e5' : '#d1d5db',
            background: lang === current ? '#4f46e5' : '#fff',
            color: lang === current ? '#fff' : '#374151',
            cursor: 'pointer',
            fontWeight: lang === current ? 600 : 400,
            fontSize: 14,
          }}
        >
          {LANG_LABELS[lang] ?? lang.toUpperCase()}
        </button>
      ))}
    </div>
  )
}
