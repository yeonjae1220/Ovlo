import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  usePost,
  useDeletePost,
  useAddComment,
  useDeleteComment,
  useReact,
  useUnreact,
} from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: post, isLoading } = usePost(id!)
  const { currentUser } = useAuthStore()
  const deletePost = useDeletePost()
  const addComment = useAddComment()
  const deleteComment = useDeleteComment()
  const react = useReact()
  const unreact = useUnreact()

  const [commentContent, setCommentContent] = useState('')

  if (isLoading) return <p>로딩 중...</p>
  if (!post) return <p>게시글을 찾을 수 없습니다.</p>

  const isMyPost = currentUser && String(post.authorId) === currentUser.id
  const likedByMe = post.likedByMe ?? false

  const handleDelete = () => {
    deletePost.mutate(id!, { onSuccess: () => navigate(-1) })
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
    <div style={{ maxWidth: 700, margin: '0 auto' }}>
      <h1>{post.deleted ? '[삭제된 게시글]' : post.title}</h1>
      <p style={{ color: '#888', fontSize: 13 }}>작성자 #{post.authorId}</p>

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
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
              }}
            >
              👍 {post.likeCount}
            </button>
            {isMyPost && (
              <button onClick={handleDelete} style={{ marginLeft: 'auto', color: 'red' }}>
                삭제
              </button>
            )}
          </div>
        </>
      )}

      <h3>댓글 {post.comments.filter((c) => !c.deleted).length}</h3>
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
                  삭제
                </button>
              )}
            </div>
            <p style={{ margin: 0, lineHeight: 1.5 }}>{c.content}</p>
          </li>
        ))}
      </ul>

      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <input
          placeholder="댓글을 입력하세요..."
          value={commentContent}
          onChange={(e) => setCommentContent(e.target.value)}
          style={{ flex: 1 }}
          onKeyDown={(e) => e.key === 'Enter' && handleComment()}
        />
        <button onClick={handleComment} disabled={addComment.isPending}>등록</button>
      </div>
    </div>
  )
}
