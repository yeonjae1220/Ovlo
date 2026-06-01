'use client'

import Link from 'next/link'
import { useFollowers, useFollowings, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'

const C = {
  border: 'var(--color-border)',
  text: 'var(--color-text)',
  danger: 'var(--color-danger)',
  dangerSoft: 'var(--color-danger-soft)',
}

export default function FollowPage() {
  const { t } = useI18n()
  const { currentUser } = useAuthStore()
  const { data: followers, isLoading: fl } = useFollowers(currentUser?.id ?? '')
  const { data: followings, isLoading: fgl } = useFollowings(currentUser?.id ?? '')
  const unfollow = useUnfollow()

  return (
    <div style={{ display: 'flex', gap: 32 }}>
      <div style={{ flex: 1 }}>
        <h2 style={{ color: C.text }}>{t('follow.followers')} ({followers?.length ?? 0})</h2>
        {fl && <p>{t('follow.loading')}</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followers?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: `1px solid ${C.border}` }}>
              <Link href={`/profile/${m.id}`}>{m.nickname}</Link>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        <h2 style={{ color: C.text }}>{t('follow.following')} ({followings?.length ?? 0})</h2>
        {fgl && <p>{t('follow.loading')}</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {followings?.map((m) => (
            <li key={m.id} style={{ padding: '8px 0', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between' }}>
              <Link href={`/profile/${m.id}`}>{m.nickname}</Link>
              <button onClick={() => unfollow.mutate(m.id)} style={{ fontSize: 12, color: C.danger, background: C.dangerSoft, borderColor: C.danger }}>
                {t('follow.unfollow')}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
