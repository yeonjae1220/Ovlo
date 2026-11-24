'use client'

import { useI18n } from '../../i18n/I18nProvider'
import { reportEvidence } from '../../utils/reportEvidence'

export default function ReportEvidencePanel({ content }: { content: unknown }) {
  const { t } = useI18n()
  const evidence = reportEvidence(content)
  return (
    <section aria-label={t('report.evidence.title')} style={{ padding: '14px 16px', marginBottom: 20, borderRadius: 8, background: 'var(--color-surface-soft)', border: '1px solid var(--color-border)' }}>
      <h2 style={{ fontSize: 15, margin: '0 0 8px' }}>{t('report.evidence.title')}</h2>
      <p style={{ margin: '0 0 6px', fontSize: 13 }}>{evidence.asOf ? t('univ.detail.asOf', { date: evidence.asOf }) : t('report.evidence.unknownDate')}</p>
      <p style={{ margin: '0 0 6px', fontSize: 13 }}>{t(`report.evidence.${evidence.status}`)}</p>
      <p style={{ margin: 0, fontSize: 12, color: 'var(--color-text-secondary)' }}>{t('report.evidence.volatile')}</p>
      {evidence.sources.length > 0 && (
        <details style={{ marginTop: 10, fontSize: 13 }}>
          <summary style={{ cursor: 'pointer' }}>{t('report.evidence.links', { count: evidence.sources.length })}</summary>
          <ul style={{ paddingLeft: 20, overflowWrap: 'anywhere' }}>
            {evidence.sources.map(source => <li key={source.url} style={{ marginTop: 8 }}><a href={source.url} target="_blank" rel="noopener noreferrer">{source.title}</a></li>)}
          </ul>
        </details>
      )}
    </section>
  )
}
