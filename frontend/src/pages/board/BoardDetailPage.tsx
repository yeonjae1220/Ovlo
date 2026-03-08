import { Link, useParams } from 'react-router-dom'
import { useBoard, useSubscribeBoard, useUnsubscribeBoard } from '../../hooks/useBoard'
import { usePosts } from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'

export default function BoardDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: board, isLoading: boardLoading } = useBoard(id!)
  const { data: posts, isLoading: postsLoading } = usePosts(id!)
  const subscribe = useSubscribeBoard()
  const unsubscribe = useUnsubscribeBoard()
  const { currentUser } = useAuthStore()

  if (boardLoading) return <p>로딩 중...</p>
  if (!board) return <p>게시판을 찾을 수 없습니다.</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1>{board.name}</h1>
          <p style={{ color: '#888' }}>{board.category} · {board.scope}</p>
          {board.description && <p>{board.description}</p>}
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => subscribe.mutate(id!)}>구독</button>
          <button onClick={() => unsubscribe.mutate(id!)}>구독취소</button>
          {currentUser && (
            <Link to={`/posts/new?boardId=${id}`}>
              <button>+ 글쓰기</button>
            </Link>
          )}
        </div>
      </div>

      {postsLoading && <p>게시글 로딩 중...</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {posts?.map((post) => (
          <li key={post.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
            <Link to={`/posts/${post.id}`} style={{ fontWeight: 'bold' }}>
              {post.deleted ? '[삭제된 게시글]' : post.title}
            </Link>
            <span style={{ marginLeft: 8, color: '#888', fontSize: 13 }}>
              댓글 {post.comments.length} · 👍 {post.likeCount}
            </span>
          </li>
        ))}
      </ul>
      {posts?.length === 0 && <p>아직 게시글이 없습니다.</p>}
    </div>
  )
}
