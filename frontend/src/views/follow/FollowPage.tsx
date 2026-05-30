'use client'

import Link from 'next/link'
import { useFollowers, useFollowings, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'

export default function FollowPage() {
  const { t } = useI18n()
  const { currentUser } = useAuthStore()
  const { data: followers, isLoading: fl } = useFollowers(currentUser?.id ?? '')
  const { data: followings, isLoading: fgl } = useFollowings(currentUser?.id ?? '')
  const unfollow = useUnfollow()

  return (
    <div style={{ display: 'flex', gap: 32 }}>
      <div style={{ flex: 1 }}>
        <h2>{t('follow.followers')} ({followers?.length ?? 0})</h2>
        {fl && <p>{t('follow.loading')}</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followers?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>
              <Link href={`/profile/${m.id}`}>{m.nickname}</Link>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        <h2>{t('follow.following')} ({followings?.length ?? 0})</h2>
        {fgl && <p>{t('follow.loading')}</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followings?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: '1px solid #eee', display: 'flex', justifyContent: 'space-between' }}>
              <Link href={`/profile/${m.id}`}>{m.nickname}</Link>
              <button onClick={() => unfollow.mutate(m.id)} style={{ fontSize: 12, color: 'red' }}>
                {t('follow.unfollow')}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
