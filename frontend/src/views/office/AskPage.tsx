'use client'

import { formatDate, noticeLangOf } from './officeFormatting'
import { Suspense, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { officeApi, type AskResult, type InquiryConversation } from '../../api/office'
import { useI18n } from '../../i18n/I18nProvider'
import { useAuthStore } from '../../store/authStore'
import { useAskQuestion, useInquiryFeedback, useMyOffice, useOpenConsultation } from '../../hooks/useOffice'
import ConsentDialog from './ConsentDialog'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines, TextAreaField } from '../../components/ui'


function Answer({ result }: { result: AskResult }) {
  const { t } = useI18n()
  return <>
    <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.8, overflowWrap: 'anywhere' }}>
      {result.answerText ? result.answerText.split(/(\[\d{1,3}\])/).map((part, i) => {
        const match = /^\[(\d{1,3})\]$/.exec(part)
        return match ? <a key={i} href={`#cite-${result.inquiryId}-${match[1]}`}>{part}</a> : <span key={i}>{part}</span>
      }) : t('ask.noGrounds')}
    </p>
    {result.citations.length > 0 && <>
      <h4 style={{ margin: '12px 0 4px' }}>{t('ask.sources')}</h4>
      <ol style={{ paddingLeft: 20 }}>
        {result.citations.map(c => <li key={c.index} id={`cite-${result.inquiryId}-${c.index}`} value={c.index}>
          {c.sourceUrl && /^https?:\/\//i.test(c.sourceUrl) ? <a href={c.sourceUrl} target="_blank" rel="noreferrer">{c.title}</a> : c.currentlyValid ? <Link href={`/my-university/notices/${c.noticeId}`}>{c.title}</Link> : c.title}
          {c.sourceType && <small> · {t(`office.knowledge.${c.sourceType}`)} · {c.department}</small>}
          {c.verifiedOn && <small> · {t('office.knowledge.lastVerified')}: {formatDate(c.verifiedOn)}</small>}
          {c.versionRecorded === false && <p role="status">{t('office.knowledge.legacy')}</p>}
          {c.versionRecorded && c.currentlyValid === false && <p role="status">{t('office.knowledge.outdated')}</p>}
          {c.expiresAt && <small> ({t('ask.validUntil', { date: formatDate(c.expiresAt) })})</small>}
        </li>)}
      </ol>
    </>}
  </>
}

function Conversation({ officeId, officeName, memberId, inquiryId }: {
  officeId: number; officeName: string; memberId: string; inquiryId?: number
}) {
  const { t, language } = useI18n()
  const router = useRouter()
  const qc = useQueryClient()
  const ask = useAskQuestion(officeId)
  const feedback = useInquiryFeedback(officeId)
  const handoff = useOpenConsultation(officeId)
  const [question, setQuestion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [consentOpen, setConsentOpen] = useState(false)
  const [feedbackIds, setFeedbackIds] = useState<number[]>([])
  const [sending, setSending] = useState(false)
  const busy = useRef(false)
  const mounted = useRef(true)
  const end = useRef<HTMLDivElement>(null)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const isCurrent = () => mounted.current && String(useAuthStore.getState().currentUser?.id) === memberId
  const queryKey = ['office', officeId, 'conversation', memberId, inquiryId] as const
  const conversation = useQuery({
    queryKey,
    queryFn: ({ signal }) => officeApi.conversation(officeId, inquiryId!, signal),
    enabled: inquiryId !== undefined,
    retry: false,
    refetchOnWindowFocus: !consentOpen,
  })
  const turns = conversation.data?.turns ?? []
  const last = turns.at(-1)?.result
  const consultationId = conversation.data?.consultationId
  const canContinue = conversation.data?.canContinue ?? true
  const unavailable = inquiryId !== undefined && (conversation.isPending || conversation.isError)
  useEffect(() => { end.current?.scrollIntoView({ block: 'nearest' }) }, [turns.length, sending])

  function failure(e: unknown) {
    if (isAxiosError(e) && e.response?.status === 409) return t('ask.refresh')
    if (isAxiosError(e) && typeof e.response?.data?.message === 'string') return e.response.data.message as string
    return t('ask.failed')
  }

  async function submit() {
    const text = question.trim()
    if (!text || busy.current || unavailable || !canContinue || consentOpen) return
    busy.current = true
    setSending(true)
    setError(null)
    let awaitingConversationRoute = false
    try {
      const result = await ask.mutateAsync({ question: text, lang: noticeLangOf(language), previousInquiryId: last?.inquiryId })
      if (!isCurrent()) return
      const rootId = inquiryId ?? result.inquiryId
      const next: InquiryConversation = { turns: [...turns, { question: text, result }], consultationId: null, canContinue: turns.length + 1 < 20 }
      qc.setQueryData(['office', officeId, 'conversation', memberId, rootId], next)
      setQuestion('')
      if (!inquiryId) {
        // Keep input locked until the saved conversation mounts. Releasing it here lets
        // a fast follow-up run with the old route's empty history and start a new chat.
        router.replace(`/my-university/ask?inquiry=${rootId}`, { scroll: false })
        awaitingConversationRoute = true
      }
    } catch (e) {
      if (isCurrent()) {
        setError(failure(e))
        if (isAxiosError(e) && e.response?.status === 409 && inquiryId) void conversation.refetch()
      }
    } finally {
      if (isCurrent() && !awaitingConversationRoute) { busy.current = false; setSending(false) }
    }
  }

  async function connect(version: string) {
    if (!last || busy.current) return
    busy.current = true
    setError(null)
    try {
      const result = await handoff.mutateAsync({ inquiryId: last.inquiryId, consentVersion: version })
      if (isCurrent()) router.push(`/my-university/consultations/${result.id}`)
    } catch (e) {
      if (isCurrent()) setError(failure(e))
    } finally { if (isCurrent()) busy.current = false }
  }

  return <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
    <PageHeader eyebrow={<Link href="/my-university">← {officeName}</Link>} title={t('ask.title')}
      description={t('ask.subtitle', { office: officeName })}
      actions={<Button variant="secondary" disabled={sending || handoff.isPending} onClick={() => {
        setError(null); setQuestion(''); setConsentOpen(false)
        router.replace('/my-university/ask', { scroll: false })
      }}>{t('ask.newConversation')}</Button>} />
    {inquiryId && conversation.isPending && <SkeletonLines count={4} />}
    {conversation.isError && <QueryErrorNotice onRetry={() => conversation.refetch()} />}
    {!unavailable && <div role="log" aria-label={t('ask.history')} aria-live="polite" style={{ display: 'grid', gap: 16 }}>
      {turns.map(({ question: q, result }) => <section key={result.inquiryId} style={{ display: 'grid', gap: 8 }}>
        <Card style={{ padding: 16, marginLeft: '12%', background: 'var(--color-surface-soft)' }}>
          <small>{t('ask.you')}</small><p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{q}</p>
        </Card>
        <Card style={{ padding: 16, marginRight: '4%' }}>
          <small>{t('ask.assistant')}</small><Answer result={result} />
          {result.answerText && (feedbackIds.includes(result.inquiryId) ? <small>{t('ask.thanks')}</small> :
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
              <small>{t('ask.helpful')}</small>
              {[true, false].map(helpful => <Button key={String(helpful)} variant="ghost" disabled={feedback.isPending}
                onClick={() => feedback.mutate({ inquiryId: result.inquiryId, helpful }, {
                  onSuccess: () => { if (isCurrent()) setFeedbackIds(ids => [...ids, result.inquiryId]) },
                  onError: e => { if (isCurrent()) setError(failure(e)) },
                })}>{t(helpful ? 'ask.helpfulYes' : 'ask.helpfulNo')}</Button>)}
            </div>)}
        </Card>
      </section>)}
      {sending && <Card style={{ padding: 16 }}><p style={{ whiteSpace: 'pre-wrap' }}>{question}</p><p role="status">{t('ask.connecting')}</p></Card>}
      <div ref={end} />
    </div>}
    {!unavailable && !sending && last && <Card style={{ padding: 16 }}>
      {consultationId ? <><p>{t('ask.staffPending')}</p><Link href={`/my-university/consultations/${consultationId}`}>{t('ask.openConsultation')}</Link></> : <>
        {last.outcome === 'NO_GROUNDS' && <p>{t('ask.handoff')}</p>}
        {last.canEscalate && <Button variant={last.outcome === 'NO_GROUNDS' ? 'primary' : 'secondary'} onClick={() => { setError(null); setConsentOpen(true) }}>{t('consult.escalate')}</Button>}
      </>}
    </Card>}
    {!unavailable && canContinue && <Card style={{ padding: 16 }}>
      <TextAreaField aria-label={t('ask.submit')} value={question} rows={3} maxLength={1000} style={{ minHeight: 96 }}
        disabled={sending || handoff.isPending || consentOpen} placeholder={t(last ? 'ask.continue' : 'ask.placeholder')}
        onChange={e => setQuestion(e.target.value)} onKeyDown={e => {
          if (e.nativeEvent.isComposing || e.keyCode === 229) return
          if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void submit() }
        }} />
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
        <small>{question.length}/1000</small>
        <Button variant="primary" disabled={!question.trim() || sending || consentOpen} onClick={() => void submit()}>{t('ask.submit')}</Button>
      </div>
    </Card>}
    {!unavailable && !canContinue && !consultationId && <p>{t('ask.limit')}</p>}
    {error && !consentOpen && <p role="alert">{error}</p>}
    {inquiryId && <Button variant="ghost" disabled={sending || handoff.isPending || consentOpen} onClick={() => { setError(null); void conversation.refetch() }}>{t('ask.refresh')}</Button>}
    <small>{t('ask.disclaimer')}</small>
    {consentOpen && last && <ConsentDialog pending={handoff.isPending} error={error}
      onDecline={() => { setConsentOpen(false); setError(null) }} onAgree={version => void connect(version)}
      preview={<><p>{t('ask.preview')}</p>{turns.map(({ question: q, result }) => <div key={result.inquiryId} style={{ marginBottom: 12 }}>
        <strong>{t('ask.you')}</strong><p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{q}</p>
        <strong>{t('ask.assistant')}</strong><p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{result.answerText ?? t('ask.noGrounds')}</p>
      </div>)}</>} />}
  </div>
}

function OfficeChat() {
  const { t } = useI18n()
  const office = useMyOffice()
  const memberId = useAuthStore(s => s.currentUser?.id)
  const params = useSearchParams()
  const raw = params.get('inquiry')
  const parsed = raw === null ? undefined : Number(raw)
  const inquiryId = parsed !== undefined && Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined
  if (office.isLoading) return <SkeletonLines count={4} />
  if (office.isError) return <QueryErrorNotice onRetry={() => office.refetch()} />
  if (!office.data) return <EmptyState title={t('office.none')} />
  if (!office.data.verifiedStudent || !memberId) return <EmptyState title={t('student.notices.notVerified')} />
  if (raw !== null && inquiryId === undefined) return <EmptyState title={t('ask.unavailable')} action={<Link href="/my-university/ask">{t('ask.newConversation')}</Link>} />
  return <Conversation key={`${memberId}-${office.data.officeId}-${inquiryId ?? 'new'}`} memberId={String(memberId)}
    officeId={office.data.officeId} officeName={office.data.officeName} inquiryId={inquiryId} />
}

export default function AskPage() {
  return <Suspense fallback={<SkeletonLines count={4} />}><OfficeChat /></Suspense>
}
