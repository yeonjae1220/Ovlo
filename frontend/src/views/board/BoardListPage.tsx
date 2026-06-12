'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useBoards, useCreateBoard } from '../../hooks/useBoard'
import { usePosts, useAllPosts } from '../../hooks/usePost'
import { useUniversityReports } from '../../hooks/useUniversity'
import { useAuthStore } from '../../store/authStore'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import type { CreateBoardRequest, BoardCategory, LocationScope, Post } from '../../types'
import { useI18n } from '../../i18n/I18nProvider'
import { resolveReportLang } from '../../utils/resolveReportLang'
import { Badge, Button, Card, EmptyState, PageHeader, SelectField, Tabs, TextField } from '../../components/ui'

const C = {
  border: 'var(--color-border)',
  card: 'var(--color-surface)',
  hover: 'var(--color-surface-hover)',
  text: 'var(--color-text)',
  textSec: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent)',
  warm: 'var(--color-warm)',
}

type TabId = 'all' | 'free' | 'reports'
type FeedItem =
  | { type: 'post'; data: Post }
  | { type: 'tip'; data: import('../../api/university').UniversityReportSummary }

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(date)
}

function preview(content: string) {
  return content.replace(/\s+/g, ' ').trim().slice(0, 120)
}

export default function BoardListPage() {
  const { t, language } = useI18n()
  const { isMobile } = useBreakpoint()
  const [activeTab, setActiveTab] = useState<TabId>('all')
  const [allPage, setAllPage] = useState(0)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [form, setForm] = useState<CreateBoardRequest>({ name: '', category: 'GENERAL', scope: 'GLOBAL' })

  const { currentUser } = useAuthStore()
  const router = useRouter()
  const { data: boards } = useBoards()
  const createBoard = useCreateBoard()

  const freeBoard = boards?.find((b) => b.category === 'GENERAL' && b.scope === 'GLOBAL') ?? null

  const { data: allPostsPage, isLoading: allPostsLoading } = useAllPosts(allPage, 20)
  const { data: freePosts, isLoading: freeLoading } = usePosts(freeBoard?.id ? String(freeBoard.id) : '')
  const { data: tipsPage, isLoading: tipsLoading } = useUniversityReports(language, '', 0, 20)
  const { data: reportsPage, isLoading: reportsLoading } = useUniversityReports(language, '', 0, 20)

  const allLoading = allPostsLoading || tipsLoading
  const allFeedItems: FeedItem[] = (() => {
    const posts = (allPostsPage?.content ?? []).map((p): FeedItem => ({ type: 'post', data: p }))
    const tips = (tipsPage?.content ?? []).map((r): FeedItem => ({ type: 'tip', data: r }))
    return [...posts, ...tips].sort((a, b) => {
      const dateA = a.data.createdAt
      const dateB = b.data.createdAt
      if (!dateA || !dateB) return 0
      return dateB.localeCompare(dateA)
    })
  })()

  const handleCreate = () => {
    createBoard.mutate(form, {
      onSuccess: () => {
        setShowCreateForm(false)
        setForm({ name: '', category: 'GENERAL', scope: 'GLOBAL' })
      },
    })
  }

  const canWrite = currentUser && activeTab !== 'reports' && freeBoard

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <PageHeader
        title={t('community.title')}
        description={isMobile ? undefined : t('landing.feature3.desc')}
        actions={
          <>
            {canWrite && (
              <Button variant="primary" icon="+" onClick={() => router.push(`/posts/new?boardId=${freeBoard.id}`)}>
                {t('community.write').replace('+ ', '')}
              </Button>
            )}
            <Button variant="secondary" onClick={() => setShowCreateForm((v) => !v)}>
              {showCreateForm ? t('community.createBoard.close').replace('▲ ', '') : t('community.createBoard').replace('▼ ', '')}
            </Button>
          </>
        }
      />

      {showCreateForm && (
        <Card style={{ padding: 16, marginBottom: 16 }}>
          <div style={{ display: 'grid', gap: 10 }}>
            <TextField
              placeholder={t('board.form.name')}
              value={form.name}
              onChange={(event) => setForm((f) => ({ ...f, name: event.target.value }))}
            />
            <TextField
              placeholder={t('board.form.desc')}
              value={form.description ?? ''}
              onChange={(event) => setForm((f) => ({ ...f, description: event.target.value }))}
            />
            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr 1fr auto auto', gap: 8 }}>
              <SelectField
                value={form.category}
                onChange={(event) => setForm((f) => ({ ...f, category: event.target.value as BoardCategory }))}
              >
                {(['GENERAL','LANGUAGE_EXCHANGE','TRADE','CULTURE','HOUSING','ACADEMIC','JOB'] as BoardCategory[]).map(
                  (c) => <option key={c} value={c}>{c}</option>
                )}
              </SelectField>
              <SelectField
                value={form.scope}
                onChange={(event) => setForm((f) => ({ ...f, scope: event.target.value as LocationScope }))}
              >
                {(['GLOBAL','COUNTRY','REGION','UNIVERSITY'] as LocationScope[]).map(
                  (s) => <option key={s} value={s}>{s}</option>
                )}
              </SelectField>
              <SelectField
                value={form.minTrustLevel ?? 'UNVERIFIED'}
                title={t('board.form.minTrust')}
                onChange={(event) => setForm((f) => ({ ...f, minTrustLevel: event.target.value as CreateBoardRequest['minTrustLevel'] }))}
              >
                <option value="UNVERIFIED">{t('board.form.minTrust.none')}</option>
                <option value="STUDENT">{t('verification.badge.student')}</option>
                <option value="EXCHANGE_VERIFIED">{t('verification.badge.exchange')}</option>
              </SelectField>
              <Button onClick={handleCreate} disabled={createBoard.isPending || !form.name.trim()} variant="primary">
                {t('board.form.create')}
              </Button>
              <Button onClick={() => setShowCreateForm(false)} variant="ghost">
                {t('board.form.cancel')}
              </Button>
            </div>
          </div>
        </Card>
      )}

      <div style={{ marginBottom: 16 }}>
        <Tabs
          value={activeTab}
          onChange={setActiveTab}
          items={[
            { value: 'all', label: t('tab.all'), icon: '◎' },
            { value: 'free', label: t('tab.free'), icon: '⌂' },
            { value: 'reports', label: t('tab.tips'), icon: '✦' },
          ]}
        />
      </div>

      {activeTab === 'all' && (
        <div>
          <MixedFeed
            items={allFeedItems}
            isLoading={allLoading}
            emptyText={t('community.empty')}
            language={language}
            onOpenTip={(id, lang) => router.push(`/university-reports/${id}?lang=${lang}`)}
          />
          {(allPostsPage?.totalElements ?? 0) > 20 && (
            <Pagination
              page={allPage}
              hasNext={allPostsPage?.hasNext ?? false}
              total={allPostsPage?.totalElements ?? 0}
              onPrev={() => setAllPage((p) => p - 1)}
              onNext={() => setAllPage((p) => p + 1)}
            />
          )}
        </div>
      )}

      {activeTab === 'free' && (
        <>
          {!freeBoard && !freeLoading ? (
            <EmptyState
              icon="⌂"
              title={t('community.noBoard')}
              action={<Button onClick={() => setShowCreateForm(true)}>{t('community.createBoard.action')}</Button>}
            />
          ) : (
            <PostFeed posts={freePosts ?? []} isLoading={freeLoading} emptyText={t('community.empty')} showBoardName={false} />
          )}
        </>
      )}

      {activeTab === 'reports' && (
        <ReportFeed
          reports={reportsPage?.content ?? []}
          isLoading={reportsLoading}
          language={language}
          onOpen={(id, lang) => router.push(`/university-reports/${id}?lang=${lang}`)}
        />
      )}
    </div>
  )
}

function MixedFeed({
  items,
  isLoading,
  emptyText,
  language,
  onOpenTip,
}: {
  items: FeedItem[]
  isLoading: boolean
  emptyText: string
  language: string
  onOpenTip: (id: number, lang: string) => void
}) {
  const { t } = useI18n()
  if (isLoading) return <p style={{ color: C.muted, padding: '24px 0' }}>{t('community.loading')}</p>
  if (items.length === 0) return <EmptyState icon="◎" title={emptyText} />

  return (
    <div style={{ display: 'grid', gap: 10 }}>
      {items.map((item) => (
        item.type === 'post' ? (
          <PostCard key={`post-${item.data.id}`} post={item.data} showBoardName />
        ) : (
          <ReportCard
            key={`tip-${item.data.id}`}
            report={item.data}
            onClick={() => onOpenTip(item.data.id, resolveReportLang(language, item.data.supportedLangs))}
          />
        )
      ))}
    </div>
  )
}

function PostFeed({ posts, isLoading, emptyText, showBoardName }: { posts: Post[]; isLoading: boolean; emptyText: string; showBoardName: boolean }) {
  const { t } = useI18n()
  if (isLoading) return <p style={{ color: C.muted, padding: '24px 0' }}>{t('community.loading')}</p>
  if (posts.length === 0) return <EmptyState icon="◎" title={emptyText} />

  return (
    <div style={{ display: 'grid', gap: 10 }}>
      {posts.map((post) => <PostCard key={post.id} post={post} showBoardName={showBoardName} />)}
    </div>
  )
}

function PostCard({ post, showBoardName }: { post: Post; showBoardName: boolean }) {
  const { t } = useI18n()
  return (
    <Link href={`/posts/${post.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>
      <Card interactive style={{ padding: 16 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
          <div style={{ minWidth: 0 }}>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
              {showBoardName && post.boardName && <Badge tone="info">{post.boardName}</Badge>}
              {post.createdAt && <span style={{ color: C.dim, fontSize: 12, fontWeight: 750 }}>{formatDate(post.createdAt)}</span>}
            </div>
            <div style={{ color: post.deleted ? C.dim : C.text, fontWeight: 900, fontSize: 16, lineHeight: 1.45, overflowWrap: 'anywhere' }}>
              {post.deleted ? t('community.deleted') : post.title}
            </div>
            {!post.deleted && post.content && (
              <p style={{ margin: '6px 0 0', color: C.muted, fontSize: 13, lineHeight: 1.6, overflowWrap: 'anywhere' }}>
                {preview(post.content)}
              </p>
            )}
          </div>
          <div style={{ display: 'grid', gap: 6, justifyItems: 'end', flexShrink: 0 }}>
            <Badge tone={post.likeCount > 0 ? 'warning' : 'neutral'}>♥ {post.likeCount}</Badge>
            <Badge tone={post.comments?.length ? 'accent' : 'neutral'}>↳ {post.comments?.filter((c) => !c.deleted).length ?? 0}</Badge>
          </div>
        </div>
      </Card>
    </Link>
  )
}

function ReportFeed({
  reports,
  isLoading,
  language,
  onOpen,
}: {
  reports: import('../../api/university').UniversityReportSummary[]
  isLoading: boolean
  language: string
  onOpen: (id: number, lang: string) => void
}) {
  const { t } = useI18n()
  if (isLoading) return <p style={{ color: C.muted, padding: '24px 0' }}>{t('community.loading')}</p>
  if (reports.length === 0) return <EmptyState icon="✦" title={t('community.emptyTips')} />

  return (
    <div style={{ display: 'grid', gap: 10 }}>
      {reports.map((report) => (
        <ReportCard
          key={report.id}
          report={report}
          onClick={() => onOpen(report.id, resolveReportLang(language, report.supportedLangs))}
        />
      ))}
      <div style={{ textAlign: 'center', marginTop: 10 }}>
        <Link href="/university-reports" style={{ color: C.accent, fontSize: 13, fontWeight: 850 }}>
          {t('community.viewAll')}
        </Link>
      </div>
    </div>
  )
}

function ReportCard({ report, onClick }: { report: import('../../api/university').UniversityReportSummary; onClick: () => void }) {
  const { t } = useI18n()
  return (
    <Card interactive onClick={onClick} style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          <Badge tone="accent">{t('community.tip.badge')}</Badge>
          <div style={{ marginTop: 8, color: C.text, fontWeight: 900, fontSize: 16, lineHeight: 1.45, overflowWrap: 'anywhere' }}>{report.title}</div>
          {report.summary && (
            <p style={{ margin: '6px 0 0', color: C.muted, fontSize: 13, lineHeight: 1.6, overflowWrap: 'anywhere' }}>
              {preview(report.summary)}
            </p>
          )}
        </div>
        <div style={{ display: 'grid', gap: 6, justifyItems: 'end', flexShrink: 0 }}>
          <Badge tone="neutral">{report.sourceVideoCount}</Badge>
          <span style={{ color: C.dim, fontSize: 12, fontWeight: 750 }}>{report.supportedLangs.map((l) => l.toUpperCase()).join(' · ')}</span>
        </div>
      </div>
    </Card>
  )
}

function Pagination({
  page,
  hasNext,
  total,
  onPrev,
  onNext,
}: {
  page: number
  hasNext: boolean
  total: number
  onPrev: () => void
  onNext: () => void
}) {
  const { t } = useI18n()
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12, marginTop: 20, flexWrap: 'wrap' }}>
      <Button onClick={onPrev} disabled={page === 0}>{t('common.prev')}</Button>
      <span style={{ color: C.muted, fontSize: 13, fontWeight: 750 }}>{page + 1} · {total}</span>
      <Button onClick={onNext} disabled={!hasNext}>{t('common.next')}</Button>
    </div>
  )
}
