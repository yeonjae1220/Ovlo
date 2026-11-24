'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useBoard, useSubscribeBoard, useUnsubscribeBoard } from '../../hooks/useBoard'
import { usePosts } from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'
import { Badge, Button, Card, EmptyState, LinkButton, PageHeader, QueryErrorNotice } from '../../components/ui'

const C = {
  text: 'var(--color-text)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
}

export default function BoardDetailPage() {
  const { t, language } = useI18n()
  const params = useParams()
  const id = params?.id as string | undefined
  const [page, setPage] = useState(0)
  const { data: board, isLoading: boardLoading, isError: boardError, refetch: refetchBoard } = useBoard(id!)
  const { data: posts, isLoading: postsLoading, isError: postsError, refetch: refetchPosts } = usePosts(id!, page)
  const subscribe = useSubscribeBoard()
  const unsubscribe = useUnsubscribeBoard()
  const { currentUser } = useAuthStore()

  if (boardLoading) return <p style={{ color: C.muted }}>{t('common.loading')}</p>
  // 조회 실패가 '게시판을 찾을 수 없습니다' 로 보이면 안 된다 (GLOBAL-PIT-108).
  if (boardError) return <QueryErrorNotice onRetry={() => void refetchBoard()} />
  if (!board) return <EmptyState icon="!" title={t('board.notFound')} />

  return (
    <div style={{ maxWidth: 860, margin: '0 auto' }}>
      <PageHeader
        title={board.name}
        description={board.description ?? `${t('board.category.' + board.category)} · ${t('board.scope.' + board.scope)}`}
        actions={
          <>
            {board.category === 'TRADE' && <LinkButton href="/marketplace" variant="primary">{language === 'ko' ? '중고거래' : 'Marketplace'}</LinkButton>}
            <Button onClick={() => subscribe.mutate(id!)} variant="secondary">{t('board.subscribe')}</Button>
            <Button onClick={() => unsubscribe.mutate(id!)} variant="ghost">{t('board.unsubscribe')}</Button>
            {currentUser && (
              <LinkButton href={`/posts/new?boardId=${id}`} variant="primary" icon="+">
                {t('community.write').replace('+ ', '')}
              </LinkButton>
            )}
          </>
        }
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
        <Badge tone="info">{t('board.category.' + board.category)}</Badge>
        <Badge tone="neutral">{t('board.scope.' + board.scope)}</Badge>
      </div>

      {postsLoading && <p style={{ color: C.muted }}>{t('board.postsLoading')}</p>}
      <div style={{ display: 'grid', gap: 10 }}>
        {!postsError && posts?.content.map((post) => (
          <Link key={post.id} href={`/posts/${post.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>
            <Card interactive style={{ padding: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ color: post.deleted ? C.dim : C.text, fontWeight: 900, fontSize: 16, overflowWrap: 'anywhere' }}>
                    {post.deleted ? t('post.deleted') : post.title}
                  </div>
                  {!post.deleted && post.content && (
                    <p style={{ margin: '6px 0 0', color: C.muted, fontSize: 13, lineHeight: 1.6, overflowWrap: 'anywhere' }}>
                      {post.content.replace(/\s+/g, ' ').trim().slice(0, 120)}
                    </p>
                  )}
                </div>
                <div style={{ display: 'grid', gap: 6, justifyItems: 'end', flexShrink: 0 }}>
                  <Badge tone={post.likeCount > 0 ? 'warning' : 'neutral'}>♥ {post.likeCount}</Badge>
                  <Badge tone={post.commentCount > 0 ? 'accent' : 'neutral'}>↳ {post.commentCount}</Badge>
                </div>
              </div>
            </Card>
          </Link>
        ))}
      </div>
      {!postsError && posts && posts.totalPages > 1 && <nav aria-label={t('common.next')} style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 20 }}>
        <Button disabled={page === 0 || postsLoading} onClick={() => setPage(page - 1)}>{t('common.prev')}</Button>
        <span>{page + 1} / {posts.totalPages}</span>
        <Button disabled={!posts.hasNext || postsLoading} onClick={() => setPage(page + 1)}>{t('common.next')}</Button>
      </nav>}
      {postsError && <QueryErrorNotice onRetry={() => void refetchPosts()} />}
      {!postsError && !postsLoading && posts?.content.length === 0 && <EmptyState icon="◎" title={t('board.empty')} />}
    </div>
  )
}
