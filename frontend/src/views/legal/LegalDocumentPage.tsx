'use client'

import Link from 'next/link'
import { useI18n } from '@/i18n/I18nProvider'
import type { LegalDocuments } from './legalContent'
import { LEGAL_CONTACT_PLACEHOLDER } from './legalContent'

type Props = {
  documents: LegalDocuments
  /** 다른 법적 문서로 가는 링크 (예: 약관 ↔ 처리방침). label 은 i18n 키 */
  related: { href: string; labelKey: string }
}

/**
 * 개인정보처리방침·이용약관 공통 렌더러. UI 언어의 번역이 있으면 그 언어로, 없으면 영어로 보여 준다.
 * 연락처 placeholder 가 남아 있으면 눈에 띄게 표시해 "확정 전" 상태를 숨기지 않는다.
 */
export default function LegalDocumentPage({ documents, related }: Props) {
  const { language, t } = useI18n()
  const doc = documents[language] ?? documents.en
  const hasPlaceholder = JSON.stringify(doc).includes(LEGAL_CONTACT_PLACEHOLDER)

  return (
    <article style={{ maxWidth: 760, margin: '0 auto', padding: '32px 16px 72px', lineHeight: 1.75, color: 'var(--color-text)' }}>
      <header style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 26, fontWeight: 800, margin: '0 0 8px' }}>{doc.title}</h1>
        <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-dim)' }}>
          {t('legal.versionLine', { version: doc.version, date: doc.updatedAt })}
          {' · '}
          <Link href={related.href} style={{ color: 'var(--color-primary)' }}>{t(related.labelKey)}</Link>
        </p>
      </header>

      {hasPlaceholder && (
        <p
          role="note"
          style={{
            margin: '0 0 24px',
            padding: '12px 14px',
            borderRadius: 8,
            border: '1px solid var(--color-border)',
            background: 'var(--color-surface)',
            fontSize: 14,
          }}
        >
          {t('legal.draftNotice')}
        </p>
      )}

      <p style={{ margin: '0 0 24px' }}>{doc.intro}</p>

      {doc.sections.map((section) => (
        <section key={section.heading} style={{ marginBottom: 24 }}>
          <h2 style={{ fontSize: 17, fontWeight: 700, margin: '0 0 8px' }}>{section.heading}</h2>
          {section.paragraphs.map((p, i) => (
            <p key={i} style={{ margin: '0 0 8px' }}>{p}</p>
          ))}
          {section.bullets && (
            <ul style={{ margin: '0 0 8px', paddingLeft: 22 }}>
              {section.bullets.map((b, i) => (
                <li key={i}>{b}</li>
              ))}
            </ul>
          )}
          {section.links && (
            <ul style={{ margin: '0 0 8px', paddingLeft: 22 }}>
              {section.links.map((link) => (
                <li key={link.href}>
                  <a href={link.href} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--color-primary)' }}>
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          )}
        </section>
      ))}
    </article>
  )
}
