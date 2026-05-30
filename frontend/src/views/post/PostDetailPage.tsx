'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useRouter } from 'next/navigation'
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

  if (isLoading) return <p>{t('common.loading')}</p>
  if (!post) return <p>{t('post.notFound')}</p>

  const isMyPost = currentUser && String(post.authorId) === currentUser.id
  const likedByMe = post.likedByMe ?? false

  const handleDelete = () => {
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
    addComment.mutate({ postId: id!, req: { content: commentContent } }, {
      onSuccess: () => setCommentContent('')
    })
  }

  return (
    <div>
      <h1>{post.deleted ? t('post.deleted') : post.title}</h1>
      <p style={{ color: '#888', fontSize: 13 }}>{t('post.author')}{post.authorId}</p>

      {!post.deleted && (
        <>
          <div style={{ lineHeight: 1.6, marginBottom: 24 }}>{post.content}</div>

          <div style={{ display: 'flex', gap: 12, marginBottom: 24, alignItems: 'center' }}>
            <button
              onClick={handleLikeToggle}
              disabled={react.isPending || unreact.isPending}
              style={{
                padding: '6px 14px',
                background: likedByMe ? '#4a90e2' : '#f0f0f0',
                color: likedByMe ? 'white' : 'inherit',
                border: 'none', borderRadius: 6, cursor: 'pointer',
              }}
            >
              👍 {post.likeCount}
            </button>
            {isMyPost && (
              <button onClick={handleDelete} style={{ marginLeft: 'auto', color: 'red' }}>
                {t('post.delete')}
              </button>
            )}
          </div>
        </>
      )}

      <h3>{t('post.comments')} {post.comments.filter((c) => !c.deleted).length}</h3>
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {post.comments.filter((c) => !c.deleted).map((c) => (
          <li key={c.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <span style={{ fontWeight: 'bold', fontSize: 13, color: '#555' }}>#{c.authorId}</span>
              {currentUser && String(c.authorId) === currentUser.id && (
                <button
                  onClick={() => deleteComment.mutate({ postId: id!, commentId: String(c.id) })}
                  style={{ color: 'red', fontSize: 12, background: 'none', border: 'none', cursor: 'pointer' }}
                >
                  {t('post.delete')}
                </button>
              )}
            </div>
            <p style={{ margin: 0, lineHeight: 1.5 }}>{c.content}</p>
          </li>
        ))}
      </ul>

      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <input
          placeholder={t('post.comment.placeholder')}
          value={commentContent}
          onChange={(e) => setCommentContent(e.target.value)}
          style={{ flex: 1 }}
          onKeyDown={(e) => e.key === 'Enter' && handleComment()}
        />
        <button onClick={handleComment} disabled={addComment.isPending}>{t('post.comment.submit')}</button>
      </div>
    </div>
  )
}
