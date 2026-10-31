'use client'

import { useState } from 'react'
import Link from 'next/link'
import { isAxiosError } from 'axios'
import { useI18n } from '../../i18n/I18nProvider'
import { useRouter } from 'next/navigation'
import { useAskQuestion, useInquiryFeedback, useMyOffice, useOpenConsultation } from '../../hooks/useOffice'
import ConsentDialog from './ConsentDialog'
import type { AskResult } from '../../api/office'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines, TextAreaField } from '../../components/ui'
import { formatDate, noticeLangOf } from './noticeUi'

/** 답변 본문의 [N] 을 출처 번호 링크로 바꾼다. */
function AnswerText({ text }: { text: string }) {
  const parts = text.split(/(\[\d{1,3}\])/)
  return (
    <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
      {parts.map((part, i) => {
        const m = /^\[(\d{1,3})\]$/.exec(part)
        if (!m) return <span key={i}>{part}</span>
        return (
          <a key={i} href={`#cite-${m[1]}`} style={{ color: 'var(--color-primary)', fontSize: '0.8em', verticalAlign: 'super', textDecoration: 'none' }}>
            [{m[1]}]
          </a>
        )
      })}
    </p>
  )
}

function errorMessage(e: unknown): string {
  if (isAxiosError(e)) {
    const code = (e.response?.data as { code?: string; message?: string } | undefined)?.code
    const msg = (e.response?.data as { message?: string } | undefined)?.message
    if (code === 'INQUIRY_RATE_LIMITED' || e.response?.status === 429) return msg ?? 'Too many requests'
    if (msg) return msg
  }
  return e instanceof Error ? e.message : 'Unexpected error'
}

export default function AskPage() {
  const { t, language } = useI18n()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office && office.verifiedStudent ? office.officeId : undefined

  const router = useRouter()
  const ask = useAskQuestion(officeId)
  const feedback = useInquiryFeedback(officeId)
  const openConsultation = useOpenConsultation(officeId)
  const [consentOpen, setConsentOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const [result, setResult] = useState<AskResult | null>(null)
  const [feedbackSent, setFeedbackSent] = useState(false)

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office) return <EmptyState title={t('office.none')} />
  if (!office.verifiedStudent) {
    return <EmptyState title={t('student.notices.notVerified')} action={<Link href="/my-university">{t('nav.myUniversity')}</Link>} />
  }

  const submit = () => {
    const q = question.trim()
    if (!q || ask.isPending) return
    setFeedbackSent(false)
    ask.mutate({ question: q, lang: noticeLangOf(language) }, { onSuccess: (r) => setResult(r) })
  }

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={<Link href="/my-university">← {office.officeName}</Link>}
        title={t('ask.title')}
        description={t('ask.subtitle', { office: office.officeName })}
      />

      <Card>
        <TextAreaField
          value={question}
          rows={4}
          maxLength={1000}
          placeholder={t('ask.placeholder')}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) submit()
          }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8, gap: 8 }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{question.length}/1000</span>
          <Button variant="primary" disabled={!question.trim() || ask.isPending} onClick={submit}>
            {t('ask.submit')}
          </Button>
        </div>
        {ask.isError && (
          <p role="alert" style={{ margin: '10px 0 0', color: 'var(--color-danger, #c0392b)', fontSize: 13 }}>
            {errorMessage(ask.error)}
          </p>
        )}
      </Card>

      {ask.isPending && <SkeletonLines count={3} />}

      {result && !ask.isPending && (
        <Card>
          {result.outcome === 'ANSWERED' && result.answerText ? (
            <>
              <AnswerText text={result.answerText} />
              <h4 style={{ margin: '16px 0 6px', fontSize: 14 }}>{t('ask.sources')}</h4>
              <ol style={{ margin: 0, paddingLeft: 20, fontSize: 14 }}>
                {result.citations.map((c) => (
                  <li key={c.index} id={`cite-${c.index}`} value={c.index} style={{ marginBottom: 4 }}>
                    {c.sourceUrl ? (
                      <a href={c.sourceUrl} target="_blank" rel="noreferrer">{c.title}</a>
                    ) : (
                      <span>{c.title}</span>
                    )}
                    {c.expiresAt && (
                      <span style={{ marginLeft: 6, color: 'var(--color-text-muted)', fontSize: 12 }}>
                        ({t('ask.validUntil', { date: formatDate(c.expiresAt) })})
                      </span>
                    )}
                  </li>
                ))}
              </ol>
            </>
          ) : (
            <p style={{ margin: 0, lineHeight: 1.7 }}>{t('ask.noGrounds')}</p>
          )}

          <p style={{ margin: '14px 0 0', fontSize: 12, color: 'var(--color-text-muted)' }}>{t('ask.disclaimer')}</p>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 12, flexWrap: 'wrap' }}>
            {result.outcome === 'ANSWERED' && (
              feedbackSent || feedback.isSuccess ? (
                <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>{t('ask.thanks')}</span>
              ) : (
                <>
                  <span style={{ fontSize: 13 }}>{t('ask.helpful')}</span>
                  <Button variant="ghost" disabled={feedback.isPending} onClick={() => feedback.mutate({ inquiryId: result.inquiryId, helpful: true }, { onSuccess: () => setFeedbackSent(true) })}>👍</Button>
                  <Button variant="ghost" disabled={feedback.isPending} onClick={() => feedback.mutate({ inquiryId: result.inquiryId, helpful: false }, { onSuccess: () => setFeedbackSent(true) })}>👎</Button>
                </>
              )
            )}
            {result.canEscalate && (
              <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={() => setConsentOpen(true)}>
                {t('consult.escalate')}
              </Button>
            )}
          </div>
        </Card>
      )}

      {consentOpen && result && (
        <ConsentDialog
          pending={openConsultation.isPending}
          error={openConsultation.isError ? errorMessage(openConsultation.error) : null}
          onDecline={() => setConsentOpen(false)}
          onAgree={(version) =>
            openConsultation.mutate(
              { inquiryId: result.inquiryId, consentVersion: version },
              { onSuccess: (c) => router.push(`/my-university/consultations/${c.id}`) },
            )
          }
        />
      )}
    </div>
  )
}
