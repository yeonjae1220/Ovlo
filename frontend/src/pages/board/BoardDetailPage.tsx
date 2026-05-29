import { Link, useParams } from 'react-router-dom'
import { useBoard, useSubscribeBoard, useUnsubscribeBoard } from '../../hooks/useBoard'
import { usePosts } from '../../hooks/usePost'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'

export default function BoardDetailPage() {
  const { t } = useI18n()
  const { id } = useParams<{ id: string }>()
  const { data: board, isLoading: boardLoading } = useBoard(id!)
  const { data: posts, isLoading: postsLoading } = usePosts(id!)
  const subscribe = useSubscribeBoard()
  const unsubscribe = useUnsubscribeBoard()
  const { currentUser } = useAuthStore()

  if (boardLoading) return <p>{t('common.loading')}</p>
  if (!board) return <p>{t('board.notFound')}</p>

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1>{board.name}</h1>
          <p style={{ color: '#888' }}>{board.category} · {board.scope}</p>
          {board.description && <p>{board.description}</p>}
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => subscribe.mutate(id!)}>{t('board.subscribe')}</button>
          <button onClick={() => unsubscribe.mutate(id!)}>{t('board.unsubscribe')}</button>
          {currentUser && (
            <Link to={`/posts/new?boardId=${id}`}>
              <button>+ {t('community.write').replace('+ ', '')}</button>
            </Link>
          )}
        </div>
      </div>

      {postsLoading && <p>{t('board.postsLoading')}</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {posts?.map((post) => (
          <li key={post.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
            <Link to={`/posts/${post.id}`} style={{ fontWeight: 'bold' }}>
              {post.deleted ? t('post.deleted') : post.title}
            </Link>
            <span style={{ marginLeft: 8, color: '#888', fontSize: 13 }}>
              {t('board.comments')} {post.comments.length} · 👍 {post.likeCount}
            </span>
          </li>
        ))}
      </ul>
      {posts?.length === 0 && <p>{t('board.empty')}</p>}
    </div>
  )
}
