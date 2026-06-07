'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useBoard, useSubscribeBoard, useUnsubscribeBoard } from '../../hooks/useBoard'
import { usePosts } from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'
import { Badge, Button, Card, EmptyState, LinkButton, PageHeader } from '../../components/ui'

const C = {
  text: 'var(--color-text)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
}

export default function BoardDetailPage() {
  const { t } = useI18n()
  const params = useParams()
  const id = params?.id as string | undefined
  const { data: board, isLoading: boardLoading } = useBoard(id!)
  const { data: posts, isLoading: postsLoading } = usePosts(id!)
  const subscribe = useSubscribeBoard()
  const unsubscribe = useUnsubscribeBoard()
  const { currentUser } = useAuthStore()

  if (boardLoading) return <p style={{ color: C.muted }}>{t('common.loading')}</p>
  if (!board) return <EmptyState icon="!" title={t('board.notFound')} />

  return (
    <div style={{ maxWidth: 860, margin: '0 auto' }}>
      <PageHeader
        title={board.name}
        description={board.description ?? `${board.category} · ${board.scope}`}
        actions={
          <>
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
        <Badge tone="info">{board.category}</Badge>
        <Badge tone="neutral">{board.scope}</Badge>
      </div>

      {postsLoading && <p style={{ color: C.muted }}>{t('board.postsLoading')}</p>}
      <div style={{ display: 'grid', gap: 10 }}>
        {posts?.map((post) => (
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
                  <Badge tone={post.comments?.length ? 'accent' : 'neutral'}>↳ {post.comments?.filter((comment) => !comment.deleted).length ?? 0}</Badge>
                </div>
              </div>
            </Card>
          </Link>
        ))}
      </div>
      {posts?.length === 0 && <EmptyState icon="◎" title={t('board.empty')} />}
    </div>
  )
}
