'use client'

import Link from 'next/link'
import { useI18n } from '@/i18n/I18nProvider'
import type { LegalDocument } from './legalContent'
import { LEGAL_CONTACT_PLACEHOLDER } from './legalContent'

type Props = {
  documents: Record<'ko' | 'en', LegalDocument>
  /** 다른 법적 문서로 가는 링크 (예: 약관 ↔ 처리방침) */
  related: { href: string; label: { ko: string; en: string } }
}

/**
 * 개인정보처리방침·이용약관 공통 렌더러. UI 언어가 ko 면 한국어, 나머지는 영어.
 * 연락처 placeholder 가 남아 있으면 눈에 띄게 표시해 "확정 전" 상태를 숨기지 않는다.
 */
export default function LegalDocumentPage({ documents, related }: Props) {
  const { language } = useI18n()
  const lang: 'ko' | 'en' = language === 'ko' ? 'ko' : 'en'
  const doc = documents[lang]
  const hasPlaceholder = JSON.stringify(doc).includes(LEGAL_CONTACT_PLACEHOLDER)

  return (
    <article style={{ maxWidth: 760, margin: '0 auto', padding: '32px 16px 72px', lineHeight: 1.75, color: 'var(--color-text)' }}>
      <header style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 26, fontWeight: 800, margin: '0 0 8px' }}>{doc.title}</h1>
        <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-dim)' }}>
          {lang === 'ko' ? `버전 ${doc.version} · 시행 ${doc.updatedAt}` : `Version ${doc.version} · Effective ${doc.updatedAt}`}
          {' · '}
          <Link href={related.href} style={{ color: 'var(--color-primary)' }}>{related.label[lang]}</Link>
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
          {lang === 'ko'
            ? '이 문서는 초안이며 대외 연락처가 아직 확정되지 않았습니다. 연락처가 정해지면 갱신됩니다.'
            : 'This document is a draft and the public contact details are not yet finalized. It will be updated once they are.'}
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
        </section>
      ))}
    </article>
  )
}
