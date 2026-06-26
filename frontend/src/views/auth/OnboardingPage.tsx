'use client'

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
    border: '1px solid var(--color-border-strong)',
    borderRadius: 12,
    boxShadow: 'var(--shadow-soft)',
    fontFamily: 'system-ui, sans-serif',
    background: 'var(--color-surface)',
  }

  const fieldStyle: React.CSSProperties  = { display: 'flex', flexDirection: 'column', gap: 6 }
  const labelStyle: React.CSSProperties  = { fontSize: 13, fontWeight: 600, color: 'var(--color-text-secondary)' }
  const hintStyle: React.CSSProperties   = { fontSize: 11, color: 'var(--color-text-dim)', marginTop: 2 }
  const inputStyle: React.CSSProperties  = {
    padding: '10px 12px',
    border: '1px solid var(--color-border-strong)',
    borderRadius: 8,
    fontSize: 14,
    outline: 'none',
    background: 'var(--color-input-bg)',
    color: 'var(--color-text)',
  }
  const selectStyle: React.CSSProperties = { ...inputStyle }
  const primaryBtn: React.CSSProperties  = {
    padding: '11px 0',
    background: 'var(--color-accent-strong)',
    color: 'var(--color-on-accent)',
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
        <p style={{ fontSize: 12, color: 'var(--color-accent)', fontWeight: 800, letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: 8 }}>
          {t('onboarding.almost')}
        </p>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 800, color: 'var(--color-text)' }}>{t('onboarding.title')}</h2>
        <p style={{ margin: '8px 0 0', fontSize: 13, color: 'var(--color-text-muted)' }}>{t('onboarding.subtitle')}</p>
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
                border: '1px solid color-mix(in srgb, var(--color-success) 48%, var(--color-border-strong))',
                borderRadius: 8,
                background: 'var(--color-success-soft)',
                color: 'var(--color-success)',
                fontWeight: 500,
                fontSize: 14,
                cursor: 'pointer',
                textAlign: 'left',
                width: '100%',
              }}
            >
              ✓ {selectedUniName}{' '}
              <span style={{ color: 'var(--color-text-dim)', fontSize: 12, fontWeight: 500 }}>
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
                <ul style={{ listStyle: 'none', padding: 0, margin: 0, border: '1px solid var(--color-border-strong)', borderRadius: 8, maxHeight: 180, overflowY: 'auto', boxShadow: 'var(--shadow-soft)', background: 'var(--color-surface)' }}>
                  {universities.slice(0, 8).map((u) => (
                    <li
                      key={u.id}
                      onClick={() => { setSelectedUniId(Number(u.id)); setSelectedUniName(u.name); setUniQuery('') }}
                      style={{ padding: '9px 12px', cursor: 'pointer', borderBottom: '1px solid var(--color-border)', fontSize: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text)' }}
                    >
                      <span>{u.name}</span>
                      <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{u.countryCode} · {u.city}</span>
                    </li>
                  ))}
                </ul>
              )}
              {uniQuery.length >= 1 && universities?.length === 0 && (
                <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-muted)' }}>{t('onboarding.university.notFound')}</p>
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

        {formError && <p style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>{formError}</p>}
        {completeOnboarding.isError && (
          <p style={{ color: 'var(--color-danger)', margin: 0, fontSize: 13, fontWeight: 700 }}>{t('onboarding.error.save')}</p>
        )}

        <button type="submit" style={primaryBtn} disabled={completeOnboarding.isPending}>
          {completeOnboarding.isPending ? t('onboarding.submitting') : t('onboarding.submit')}
        </button>
      </form>
    </div>
  )
}
