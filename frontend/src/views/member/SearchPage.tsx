'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMemberSearch } from '../../hooks/useMember'
import { useFollowings, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useI18n } from '../../i18n/I18nProvider'

const C = {
  border: 'var(--color-border)',
  borderStrong: 'var(--color-border-strong)',
  surface: 'var(--color-surface)',
  text: 'var(--color-text)',
  muted: 'var(--color-text-muted)',
  accent: 'var(--color-accent-strong)',
}

export default function SearchPage() {
  const { t } = useI18n()
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
      <h2 style={{ color: C.text }}>{t('search.title')}</h2>
      <input
        placeholder={t('search.placeholder')}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        style={{ width: '100%', padding: '10px 12px', marginBottom: 16, boxSizing: 'border-box', border: `1px solid ${C.borderStrong}`, borderRadius: 8, fontSize: 15 }}
        autoFocus
      />

      {isFetching && <p style={{ color: C.muted, fontSize: 14 }}>{t('search.searching')}</p>}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {results
          ?.filter((m) => String(m.id) !== String(currentUser?.id))
          .map((m) => (
            <li key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: `1px solid ${C.border}` }}>
              <div style={{
                width: 44, height: 44, borderRadius: '50%', background: C.accent,
                color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontWeight: 'bold', fontSize: 18, flexShrink: 0,
              }}>
                {m.nickname[0]?.toUpperCase() ?? '?'}
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <Link href={`/profile/${m.id}`} style={{ fontWeight: 600, textDecoration: 'none', color: C.text, display: 'block' }}>
                  {m.nickname}
                </Link>
                <div style={{ fontSize: 12, color: C.muted, marginTop: 2 }}>{m.name}</div>
              </div>

              {isFollowing(String(m.id)) ? (
                <button
                  onClick={() => unfollow.mutate(String(m.id))}
                  style={{ fontSize: 13, color: C.muted, border: `1px solid ${C.borderStrong}`, background: C.surface, padding: '5px 14px', borderRadius: 20, cursor: 'pointer', flexShrink: 0 }}
                >
                  {t('search.following')}
                </button>
              ) : (
                <button
                  onClick={() => follow.mutate(String(m.id))}
                  style={{ fontSize: 13, color: '#fff', background: C.accent, border: 'none', padding: '5px 14px', borderRadius: 20, cursor: 'pointer', flexShrink: 0 }}
                >
                  {t('search.follow')}
                </button>
              )}
            </li>
          ))}
      </ul>

      {results?.filter((m) => String(m.id) !== String(currentUser?.id)).length === 0
        && query.length >= 1
        && !isFetching && (
          <p style={{ color: C.muted, fontSize: 14 }}>{t('search.noResults')}</p>
        )}
    </div>
  )
}
