import { useState, type FormEvent } from 'react'
import { useGlobalUniversitySearch } from '../../hooks/useUniversity'
import { useCompleteOnboarding } from '../../hooks/useAuth'
import { useI18n } from '../../i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES, LANGUAGE_LABELS } from '../../i18n/messages'

const DEGREE_TYPES = [
  { value: 'BACHELOR' },
  { value: 'MASTER' },
  { value: 'DOCTOR' },
]

const DEGREE_LABELS: Record<string, Record<string, string>> = {
  en: { BACHELOR: 'Bachelor', MASTER: 'Master', DOCTOR: 'Doctor' },
  ko: { BACHELOR: '학사', MASTER: '석사', DOCTOR: '박사' },
  ja: { BACHELOR: '学士', MASTER: '修士', DOCTOR: '博士' },
  zh: { BACHELOR: '学士', MASTER: '硕士', DOCTOR: '博士' },
  es: { BACHELOR: 'Licenciatura', MASTER: 'Maestría', DOCTOR: 'Doctorado' },
  fr: { BACHELOR: 'Licence', MASTER: 'Master', DOCTOR: 'Doctorat' },
  de: { BACHELOR: 'Bachelor', MASTER: 'Master', DOCTOR: 'Doktor' },
}

type FormData = {
  hometown: string
  majorName: string
  degreeType: string
  gradeLevel: string
}

export default function OnboardingPage() {
  const { t, language, setLanguage } = useI18n()

  const [form, setForm] = useState<FormData>({
    hometown: '',
    majorName: '',
    degreeType: 'BACHELOR',
    gradeLevel: '1',
  })
  const [uniQuery, setUniQuery]               = useState('')
  const [selectedUniId, setSelectedUniId]     = useState<number | null>(null)
  const [selectedUniName, setSelectedUniName] = useState('')
  const [formError, setFormError]             = useState('')

  const completeOnboarding = useCompleteOnboarding()
  const { data: universities } = useGlobalUniversitySearch(uniQuery)

  const set = (key: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [key]: e.target.value }))
      setFormError('')
    }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.hometown.trim()) { setFormError(t('onboarding.error.hometown')); return }
    if (!selectedUniId)        { setFormError(t('onboarding.error.university')); return }
    if (!form.majorName.trim()) { setFormError(t('onboarding.error.major')); return }

    completeOnboarding.mutate({
      hometown: form.hometown,
      homeUniversityId: selectedUniId,
      majorName: form.majorName,
      degreeType: form.degreeType,
      gradeLevel: Number(form.gradeLevel),
    })
  }

  const degreeLabels = DEGREE_LABELS[language] ?? DEGREE_LABELS.en

  const containerStyle: React.CSSProperties = {
    maxWidth: 460,
    margin: '60px auto',
    padding: '32px 28px',
    border: '1px solid #e5e7eb',
    borderRadius: 12,
    boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
    fontFamily: 'system-ui, sans-serif',
    background: '#ffffff',
    colorScheme: 'light',
  }

  const fieldStyle: React.CSSProperties  = { display: 'flex', flexDirection: 'column', gap: 6 }
  const labelStyle: React.CSSProperties  = { fontSize: 13, fontWeight: 600, color: '#374151' }
  const hintStyle: React.CSSProperties   = { fontSize: 11, color: '#9ca3af', marginTop: 2 }
  const inputStyle: React.CSSProperties  = {
    padding: '10px 12px',
    border: '1px solid #d1d5db',
    borderRadius: 8,
    fontSize: 14,
    outline: 'none',
  }
  const selectStyle: React.CSSProperties = { ...inputStyle, background: '#f9fafb', color: '#374151' }
  const primaryBtn: React.CSSProperties  = {
    padding: '11px 0',
    background: '#2563eb',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 600,
    cursor: 'pointer',
    marginTop: 4,
  }

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: 28 }}>
        <p style={{ fontSize: 12, color: '#7c3aed', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: 8 }}>
          {t('onboarding.almost')}
        </p>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, color: '#111827' }}>{t('onboarding.title')}</h2>
        <p style={{ margin: '8px 0 0', fontSize: 13, color: '#6b7280' }}>{t('onboarding.subtitle')}</p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* UI Language — shown first so the rest of the form renders in chosen language */}
        <div style={fieldStyle}>
          <label style={labelStyle}>{t('onboarding.uiLanguage')}</label>
          <select style={selectStyle} value={language} onChange={(e) => setLanguage(e.target.value)}>
            {SUPPORTED_UI_LANGUAGES.map((lang) => (
              <option key={lang} value={lang}>{LANGUAGE_LABELS[lang]}</option>
            ))}
          </select>
          <span style={hintStyle}>{t('onboarding.uiLanguage.hint')}</span>
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle}>{t('onboarding.hometown')}</label>
          <input
            style={inputStyle}
            value={form.hometown}
            onChange={set('hometown')}
            placeholder={t('onboarding.hometown.placeholder')}
            required
          />
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle}>{t('onboarding.university')}</label>
          {selectedUniId ? (
            <button
              type="button"
              onClick={() => { setSelectedUniId(null); setSelectedUniName(''); setUniQuery('') }}
              style={{
                padding: '10px 12px',
                border: '1px solid #86efac',
                borderRadius: 8,
                background: '#f0fdf4',
                color: '#16a34a',
                fontWeight: 500,
                fontSize: 14,
                cursor: 'pointer',
                textAlign: 'left',
                width: '100%',
              }}
            >
              ✓ {selectedUniName}{' '}
              <span style={{ color: '#9ca3af', fontSize: 12, fontWeight: 400 }}>
                {t('onboarding.university.change')}
              </span>
            </button>
          ) : (
            <>
              <input
                style={inputStyle}
                placeholder={t('onboarding.university.placeholder')}
                value={uniQuery}
                onChange={(e) => { setUniQuery(e.target.value); setFormError('') }}
              />
              {uniQuery.length >= 1 && universities && universities.length > 0 && (
                <ul style={{ listStyle: 'none', padding: 0, margin: 0, border: '1px solid #e5e7eb', borderRadius: 8, maxHeight: 180, overflowY: 'auto', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
                  {universities.slice(0, 8).map((u) => (
                    <li
                      key={u.id}
                      onClick={() => { setSelectedUniId(Number(u.id)); setSelectedUniName(u.name); setUniQuery('') }}
                      style={{ padding: '9px 12px', cursor: 'pointer', borderBottom: '1px solid #f3f4f6', fontSize: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                    >
                      <span>{u.name}</span>
                      <span style={{ color: '#9ca3af', fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                    </li>
                  ))}
                </ul>
              )}
              {uniQuery.length >= 1 && universities?.length === 0 && (
                <p style={{ margin: 0, fontSize: 13, color: '#9ca3af' }}>{t('onboarding.university.notFound')}</p>
              )}
            </>
          )}
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle}>{t('onboarding.major')}</label>
          <input style={inputStyle} value={form.majorName} onChange={set('majorName')} placeholder={t('onboarding.major.placeholder')} required />
        </div>

        <div style={{ display: 'flex', gap: 12 }}>
          <div style={{ ...fieldStyle, flex: 1 }}>
            <label style={labelStyle}>{t('onboarding.degree')}</label>
            <select style={selectStyle} value={form.degreeType} onChange={set('degreeType')}>
              {DEGREE_TYPES.map((d) => (
                <option key={d.value} value={d.value}>{degreeLabels[d.value] ?? d.value}</option>
              ))}
            </select>
          </div>
          <div style={{ ...fieldStyle, flex: 1 }}>
            <label style={labelStyle}>{t('onboarding.grade')}</label>
            <select style={selectStyle} value={form.gradeLevel} onChange={set('gradeLevel')}>
              {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
        </div>

        {formError && <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{formError}</p>}
        {completeOnboarding.isError && (
          <p style={{ color: '#dc2626', margin: 0, fontSize: 13 }}>{t('onboarding.error.save')}</p>
        )}

        <button type="submit" style={primaryBtn} disabled={completeOnboarding.isPending}>
          {completeOnboarding.isPending ? t('onboarding.submitting') : t('onboarding.submit')}
        </button>
      </form>
    </div>
  )
}
