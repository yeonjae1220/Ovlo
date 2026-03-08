import { Link } from 'react-router-dom'
import { useFollowers, useFollowings, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'

export default function FollowPage() {
  const { currentUser } = useAuthStore()
  const { data: followers, isLoading: fl } = useFollowers(currentUser?.id ?? '')
  const { data: followings, isLoading: fgl } = useFollowings(currentUser?.id ?? '')
  const unfollow = useUnfollow()

  return (
    <div style={{ maxWidth: 700, margin: '0 auto', display: 'flex', gap: 32 }}>
      <div style={{ flex: 1 }}>
        <h2>팔로워 ({followers?.length ?? 0})</h2>
        {fl && <p>로딩 중...</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followers?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>
              <Link to={`/profile/${m.id}`}>{m.nickname}</Link>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        <h2>팔로잉 ({followings?.length ?? 0})</h2>
        {fgl && <p>로딩 중...</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followings?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: '1px solid #eee', display: 'flex', justifyContent: 'space-between' }}>
              <Link to={`/profile/${m.id}`}>{m.nickname}</Link>
              <button
                onClick={() => unfollow.mutate(m.id)}
                style={{ fontSize: 12, color: 'red' }}
              >
                언팔로우
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
