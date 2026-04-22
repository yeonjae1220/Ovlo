import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMemberSearch } from '../../hooks/useMember'
import { useFollowings, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'

export default function SearchPage() {
  const { currentUser } = useAuthStore()
  const [query, setQuery] = useState('')
  const { data: results, isFetching } = useMemberSearch(query)
  const { data: followings } = useFollowings(currentUser?.id ?? '')
  const follow = useFollow()
  const unfollow = useUnfollow()

  const isFollowing = (memberId: string) =>
    followings?.some((f) => String(f.id) === memberId) ?? false

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <h2>사용자 검색</h2>
      <input
        placeholder="닉네임으로 검색..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        style={{ width: '100%', padding: '10px 12px', marginBottom: 16, boxSizing: 'border-box', border: '1px solid #ddd', borderRadius: 8, fontSize: 15 }}
        autoFocus
      />

      {isFetching && <p style={{ color: '#888', fontSize: 14 }}>검색 중...</p>}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {results
          ?.filter((m) => String(m.id) !== String(currentUser?.id))
          .map((m) => (
            <li
              key={m.id}
              style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: '1px solid #eee' }}
            >
              <div style={{
                width: 44, height: 44, borderRadius: '50%', background: '#007bff',
                color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontWeight: 'bold', fontSize: 18, flexShrink: 0,
              }}>
                {m.nickname[0]?.toUpperCase() ?? '?'}
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <Link
                  to={`/profile/${m.id}`}
                  style={{ fontWeight: 600, textDecoration: 'none', color: '#000', display: 'block' }}
                >
                  {m.nickname}
                </Link>
                <div style={{ fontSize: 12, color: '#888', marginTop: 2 }}>{m.name}</div>
              </div>

              {isFollowing(String(m.id)) ? (
                <button
                  onClick={() => unfollow.mutate(String(m.id))}
                  style={{ fontSize: 13, color: '#555', border: '1px solid #ccc', background: '#f5f5f5', padding: '5px 14px', borderRadius: 20, cursor: 'pointer', flexShrink: 0 }}
                >
                  팔로잉
                </button>
              ) : (
                <button
                  onClick={() => follow.mutate(String(m.id))}
                  style={{ fontSize: 13, color: '#fff', background: '#007bff', border: 'none', padding: '5px 14px', borderRadius: 20, cursor: 'pointer', flexShrink: 0 }}
                >
                  팔로우
                </button>
              )}
            </li>
          ))}
      </ul>

      {results?.filter((m) => String(m.id) !== String(currentUser?.id)).length === 0
        && query.length >= 1
        && !isFetching && (
          <p style={{ color: '#888', fontSize: 14 }}>검색 결과가 없습니다</p>
        )}
    </div>
  )
}
