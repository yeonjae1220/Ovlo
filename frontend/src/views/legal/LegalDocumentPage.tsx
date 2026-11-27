'use client'

import Link from 'next/link'
import { useI18n } from '@/i18n/I18nProvider'
import type { LegalDocuments } from './legalContent'
import { LEGAL_DOCS_DRAFT } from './legalConstants'

type Props = {
  documents: LegalDocuments
  /** 다른 법적 문서로 가는 링크 (예: 약관 ↔ 처리방침). label 은 i18n 키 */
  related: { href: string; labelKey: string }
  /** 이 문서의 경로(예: /terms). 한국어 원문은 `${path}/ko` 에 있다. */
  path: string
  /** true 면 UI 언어와 상관없이 정본인 한국어 원문을 보여 준다. */
  original?: boolean
}

/**
 * 개인정보처리방침·이용약관 공통 렌더러. UI 언어의 번역이 있으면 그 언어로, 없으면 영어로 보여 준다.
 * 연락처 placeholder 가 남아 있으면 눈에 띄게 표시해 "확정 전" 상태를 숨기지 않는다.
 * 정본은 한국어다(각 문서의 마지막 '언어' 조항). 번역본을 보여 줄 때는 한국어 원문으로 가는 링크를 단다.
 */
export default function LegalDocumentPage({ documents, related, path, original = false }: Props) {
  const { language, t } = useI18n()
  const translated = documents[language] ?? documents.en
  const doc = original ? documents.ko : translated
  const docLanguage = doc === documents.ko ? 'ko' : doc === documents[language] ? language : 'en'
  const isTranslation = doc !== documents.ko
  // 한국어가 아닌 UI 에서 원문을 열었다면 읽던 번역본으로 돌아갈 길을 남긴다
  const canReturnToTranslation = original && translated !== documents.ko
  // 원문을 읽는 중이면 관련 문서도 원문으로 잇는다
  const relatedHref = original ? `${related.href}/ko` : related.href

  return (
    <article lang={docLanguage} style={{ maxWidth: 760, margin: '0 auto', padding: '32px 16px 72px', lineHeight: 1.75, color: 'var(--color-text)' }}>
      <header style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 26, fontWeight: 800, margin: '0 0 8px' }}>{doc.title}</h1>
        <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-dim)' }}>
          {t('legal.versionLine', { version: doc.version, date: doc.updatedAt })}
          {' · '}
          <Link href={relatedHref} style={{ color: 'var(--color-primary)' }}>{t(related.labelKey)}</Link>
        </p>
      </header>

      {LEGAL_DOCS_DRAFT && (
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

      {isTranslation && (
        <p style={{ margin: '0 0 24px', fontSize: 14 }}>
          <Link href={`${path}/ko`} hrefLang="ko" style={{ color: 'var(--color-primary)' }}>{t('legal.viewKoreanOriginal')}</Link>
        </p>
      )}

      {canReturnToTranslation && (
        <p lang={language} style={{ margin: '0 0 24px', fontSize: 14 }}>
          <Link href={path} style={{ color: 'var(--color-primary)' }}>{t('legal.viewTranslation')}</Link>
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
