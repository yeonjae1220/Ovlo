'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { helpApi, type HelpAnswer } from '../../api/help'
import { useI18n } from '../../i18n/I18nProvider'
import { useAuthStore } from '../../store/authStore'
import { noticeLangOf } from '../office/officeFormatting'
import { Button, Card, PageHeader, QueryErrorNotice, SkeletonLines, TextAreaField } from '../../components/ui'

const MAX_QUESTION_LENGTH = 1000

function AnswerView({ answer }: { answer: HelpAnswer }) {
  const { t } = useI18n()
  if (!answer.answerText) {
    return <p style={{ margin: 0 }}>{t(answer.outcome === 'OUT_OF_SCOPE' ? 'help.outOfScope' : 'help.noGrounds')}</p>
  }
  return <>
    <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.8, overflowWrap: 'anywhere' }}>{answer.answerText}</p>
    {answer.citations.length > 0 && <>
      <h4 style={{ margin: '12px 0 4px' }}>{t('help.sources')}</h4>
      <ol style={{ paddingLeft: 20, margin: 0 }}>
        {answer.citations.map(c => <li key={c.index} value={c.index}><a href={`#${c.articleId}`}>{c.title}</a></li>)}
      </ol>
    </>}
  </>
}

function AskBox() {
  const { t, language } = useI18n()
  const [question, setQuestion] = useState('')
  const ask = useMutation({ mutationFn: (text: string) => helpApi.ask(text, noticeLangOf(language)) })

  function submit() {
    const text = question.trim()
    if (!text || ask.isPending) return
    ask.mutate(text)
  }

  function failure(e: unknown) {
    if (isAxiosError(e) && typeof e.response?.data?.message === 'string') return e.response.data.message as string
    return t('help.failed')
  }

  return <Card style={{ padding: 16, display: 'grid', gap: 8 }}>
    <h3 style={{ margin: 0 }}>{t('help.askTitle')}</h3>
    <TextAreaField aria-label={t('help.askTitle')} value={question} rows={3} maxLength={MAX_QUESTION_LENGTH}
      disabled={ask.isPending} placeholder={t('help.placeholder')} onChange={e => setQuestion(e.target.value)}
      onKeyDown={e => {
        if (e.nativeEvent.isComposing || e.keyCode === 229) return
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
      }} />
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <small>{question.length}/{MAX_QUESTION_LENGTH}</small>
      <Button variant="primary" disabled={!question.trim() || ask.isPending} onClick={submit}>
        {ask.isPending ? t('help.asking') : t('help.submit')}
      </Button>
    </div>
    {ask.isError && <p role="alert" style={{ margin: 0 }}>{failure(ask.error)}</p>}
    {ask.data && <div role="status" aria-live="polite"><AnswerView answer={ask.data} /></div>}
    <small>{t('help.disclaimer')}</small>
  </Card>
}

export default function HelpPage() {
  const { t } = useI18n()
  const loggedIn = useAuthStore(s => s.currentUser != null)
  const articles = useQuery({ queryKey: ['help', 'articles'], queryFn: ({ signal }) => helpApi.articles(signal) })

  return <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
    <PageHeader title={t('help.title')} description={t('help.subtitle')} />
    {loggedIn ? <AskBox /> : <Card style={{ padding: 16 }}>
      <p style={{ margin: 0 }}>{t('help.loginToAsk')} <Link href="/login">{t('help.login')}</Link></p>
    </Card>}
    {articles.isPending && <SkeletonLines count={6} />}
    {articles.isError && <QueryErrorNotice onRetry={() => articles.refetch()} />}
    {articles.data && <>
      <nav aria-label={t('help.contents')}>
        <ul style={{ paddingLeft: 20, margin: 0, display: 'grid', gap: 4 }}>
          {articles.data.map(a => <li key={a.id}><a href={`#${a.id}`}>{a.title}</a></li>)}
        </ul>
      </nav>
      {articles.data.map(a => <Card key={a.id} id={a.id} style={{ padding: 16, scrollMarginTop: 80 }}>
        <h2 style={{ margin: '0 0 8px', fontSize: '1.1rem' }}>{a.title}</h2>
        <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.8, overflowWrap: 'anywhere' }}>{a.body}</p>
      </Card>)}
    </>}
  </div>
}
