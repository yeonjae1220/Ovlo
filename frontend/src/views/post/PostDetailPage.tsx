'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import {
  usePost,
  useDeletePost,
  useAddComment,
  useDeleteComment,
  useReact,
  useUnreact,
} from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'
import { Avatar, Badge, Button, Card, EmptyState, PageHeader, TextField } from '../../components/ui'

const C = {
  border: 'var(--color-border)',
  surface: 'var(--color-surface)',
  text: 'var(--color-text)',
  textSecondary: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent-strong)',
  danger: 'var(--color-danger)',
}

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default function PostDetailPage() {
  const { t } = useI18n()
  const params = useParams()
  const id = params?.id as string | undefined
  const router = useRouter()
  const { data: post, isLoading } = usePost(id!)
  const { currentUser } = useAuthStore()
  const deletePost = useDeletePost()
  const addComment = useAddComment()
  const deleteComment = useDeleteComment()
  const react = useReact()
  const unreact = useUnreact()

  const [commentContent, setCommentContent] = useState('')

  if (isLoading) return <p style={{ color: C.muted }}>{t('common.loading')}</p>
  if (!post) return <EmptyState icon="!" title={t('post.notFound')} />

  const isMyPost = currentUser && String(post.authorId) === currentUser.id
  const likedByMe = post.likedByMe ?? false
  const visibleComments = post.comments.filter((comment) => !comment.deleted)

  const handleDelete = () => {
    if (!window.confirm(t('post.delete.confirm'))) return
    deletePost.mutate(id!, { onSuccess: () => router.back() })
  }

  const handleLikeToggle = () => {
    if (likedByMe) {
      unreact.mutate(id!)
    } else {
      react.mutate({ postId: id!, reactionType: 'LIKE' })
    }
  }

  const handleComment = () => {
    if (!commentContent.trim()) return
    addComment.mutate({ postId: id!, req: { content: commentContent.trim() } }, {
      onSuccess: () => setCommentContent('')
    })
  }

  return (
    <div style={{ maxWidth: 820, margin: '0 auto' }}>
      <PageHeader
        title={post.deleted ? t('post.deleted') : post.title}
        description={post.boardName}
        actions={<Button variant="ghost" onClick={() => router.back()} icon="←">{t('exch.back').replace('← ', '')}</Button>}
      />

      <Card style={{ padding: 20, marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', marginBottom: 18 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
            <Avatar label={`#${post.authorId}`} />
            <div>
              <div style={{ color: C.text, fontWeight: 900 }}>{t('post.author')}{post.authorId}</div>
              <div style={{ color: C.dim, fontSize: 13 }}>{formatDate(post.createdAt)}</div>
            </div>
          </div>
          {post.boardName && <Badge tone="info">{post.boardName}</Badge>}
        </div>

        {!post.deleted && (
          <>
            <div style={{
              whiteSpace: 'pre-wrap',
              lineHeight: 1.75,
              marginBottom: 22,
              color: C.textSecondary,
              fontSize: 16,
              overflowWrap: 'anywhere',
            }}>
              {post.content}
            </div>

            <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
              <Button
                onClick={handleLikeToggle}
                disabled={react.isPending || unreact.isPending}
                variant={likedByMe ? 'primary' : 'secondary'}
                icon="♥"
              >
                {post.likeCount}
              </Button>
              <Badge tone="neutral">{t('post.comments')} {visibleComments.length}</Badge>
              {isMyPost && (
                <Button onClick={handleDelete} variant="danger" style={{ marginLeft: 'auto' }}>
                  {t('post.delete')}
                </Button>
              )}
            </div>
          </>
        )}
      </Card>

      <Card style={{ padding: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <h2 style={{ margin: 0, color: C.text, fontSize: 18, fontWeight: 900 }}>{t('post.comments')}</h2>
          <Badge tone="accent">{visibleComments.length}</Badge>
        </div>

        {visibleComments.length === 0 ? (
          <EmptyState icon="↳" title={t('post.comment.empty')} />
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {visibleComments.map((comment) => (
              <li key={comment.id} style={{ padding: '13px 0', borderBottom: `1px solid ${C.border}` }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 6 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Avatar label={`#${comment.authorId}`} size="sm" />
                    <span style={{ fontWeight: 850, fontSize: 13, color: C.muted }}>#{comment.authorId}</span>
                  </div>
                  {currentUser && String(comment.authorId) === currentUser.id && (
                    <button
                      onClick={() => deleteComment.mutate({ postId: id!, commentId: String(comment.id) })}
                      style={{ color: C.danger, fontSize: 12, background: 'none', border: 'none', cursor: 'pointer', padding: 4 }}
                    >
                      {t('post.delete')}
                    </button>
                  )}
                </div>
                <p style={{ margin: 0, lineHeight: 1.6, color: C.textSecondary, overflowWrap: 'anywhere' }}>{comment.content}</p>
              </li>
            ))}
          </ul>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, marginTop: 14 }}>
          <TextField
            placeholder={t('post.comment.placeholder')}
            value={commentContent}
            onChange={(event) => setCommentContent(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && handleComment()}
          />
          <Button onClick={handleComment} disabled={addComment.isPending || !commentContent.trim()} variant="primary">
            {t('post.comment.submit')}
          </Button>
        </div>
      </Card>
    </div>
  )
}
