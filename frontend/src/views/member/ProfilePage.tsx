'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useRouter } from 'next/navigation'
import { useMember, useUpdateProfile } from '../../hooks/useMember'
import { useFollowers, useFollowings, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useDropzone } from 'react-dropzone'
import { useUploadMedia } from '../../hooks/useMedia'
import { memberApi } from '../../api/member'
import { authApi } from '../../api/auth'
import { useQueryClient } from '@tanstack/react-query'
import { usePostsByAuthor } from '../../hooks/usePost'
import { useI18n } from '../../i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES, LANGUAGE_LABELS } from '../../i18n/messages'
import { useTheme, type ThemeMode } from '../../theme/themeContext'
import { useMyVerification } from '../../hooks/useVerification'
import { TrustBadge } from '../verification/TrustBadge'
import { VerificationSection } from '../verification/VerificationSection'

const C = {
  card:        'var(--color-surface)',
  border:      'var(--color-border)',
  borderLight: 'var(--color-border-strong)',
  textPrimary: 'var(--color-text)',
  textSec:     'var(--color-text-secondary)',
  textMuted:   'var(--color-text-muted)',
  textDim:     'var(--color-text-dim)',
  activeText:  'var(--color-info)',
  purple:      'var(--color-accent)',
  accentSoft:  'var(--color-accent-soft)',
  surfaceSoft: 'var(--color-surface-soft)',
  disabled:    'var(--color-surface-disabled)',
  danger:      'var(--color-danger)',
  dangerSoft:  'var(--color-danger-soft)',
}

export default function ProfilePage() {
  const params = useParams()
  const id = params?.id as string | undefined
  const router = useRouter()
  const { currentUser, clearAuth } = useAuthStore()
  const { language, setLanguage, t } = useI18n()
  const { theme, setTheme } = useTheme()
  const { data: member, isLoading } = useMember(id!)
  const { data: followers } = useFollowers(id!)
  const { data: followings } = useFollowings(id!)
  const updateProfile = useUpdateProfile()
  const followMutation = useFollow()
  const unfollowMutation = useUnfollow()
  const uploadMedia = useUploadMedia()

  const [editing, setEditing] = useState(false)
  const [nickname, setNickname] = useState('')
  const [bio, setBio] = useState('')
  const [myPostsPage, setMyPostsPage] = useState(0)

  const { data: myPostsData, isLoading: myPostsLoading } = usePostsByAuthor(id, myPostsPage, 10)

  const queryClient = useQueryClient()
  const isOwner = String(currentUser?.id) === id
  const isFollowing = followers?.some((f) => String(f.id) === String(currentUser?.id))
  const { data: myVerification } = useMyVerification(isOwner)

  const { getRootProps, getInputProps } = useDropzone({
    accept: { 'image/*': [] },
    maxFiles: 1,
    onDrop: async (files) => {
      if (!files[0] || !id) return
      const media = await uploadMedia.mutateAsync(files[0])
      await memberApi.updateProfileImage(id, String(media.mediaId))
    },
  })

  const startEdit = () => {
    setNickname(member?.nickname ?? '')
    setBio(member?.bio ?? '')
    setEditing(true)
  }

  const saveEdit = () => {
    updateProfile.mutate({ id: id!, req: { nickname, bio } })
    setEditing(false)
  }

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      queryClient.clear()
      clearAuth()
      router.push('/login')
    }
  }

  const handleWithdraw = async () => {
    if (!confirm(t('profile.withdraw.confirm'))) return
    try {
      await memberApi.withdraw(id!)
    } finally {
      clearAuth()
      router.push('/login')
    }
  }

  if (isLoading) return <p>{t('profile.loading')}</p>
  if (!member) return <p>{t('profile.notFound')}</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
        {isOwner ? (
          <div {...getRootProps()} style={{ cursor: 'pointer', width: 80, height: 80, borderRadius: '50%', background: C.surfaceSoft, border: `1px solid ${C.border}`, color: C.textMuted, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <input {...getInputProps()} />
            <span style={{ fontSize: 12 }}>{t('profile.changePhoto')}</span>
          </div>
        ) : (
          <div style={{ width: 80, height: 80, borderRadius: '50%', background: C.surfaceSoft, border: `1px solid ${C.border}` }} />
        )}

        <div style={{ flex: 1 }}>
          {editing ? (
            <>
              <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder={t('profile.nickname.placeholder')} />
              <textarea value={bio} onChange={(e) => setBio(e.target.value)} placeholder={t('profile.bio.placeholder')} rows={3} style={{ display: 'block', width: '100%' }} />
              <button onClick={saveEdit}>{t('profile.save')}</button>
              <button onClick={() => setEditing(false)}>{t('profile.cancel')}</button>
            </>
          ) : (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <h2 style={{ color: C.textPrimary, margin: 0 }}>{member.nickname}</h2>
                {isOwner && myVerification && <TrustBadge level={myVerification.trustLevel} />}
              </div>
              <p style={{ color: C.textMuted }}>{member.name} · {member.email}</p>
              {member.bio && <p style={{ color: C.textSec }}>{member.bio}</p>}
              <p style={{ color: C.textMuted }}>{t('profile.followers')} {followers?.length ?? 0} · {t('profile.following')} {followings?.length ?? 0}</p>
              {isOwner ? (
                <button onClick={startEdit}>{t('profile.editBtn')}</button>
              ) : (
                isFollowing ? (
                  <button onClick={() => unfollowMutation.mutate(id!)}>{t('profile.unfollow')}</button>
                ) : (
                  <button onClick={() => followMutation.mutate(id!)}>{t('profile.follow')}</button>
                )
              )}
            </>
          )}
        </div>
      </div>

      {/* 내가 쓴 글 */}
      <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
        <h3 style={{ margin: '0 0 12px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
          {isOwner ? t('profile.myPosts') : t('profile.theirPosts')}
        </h3>
        {myPostsLoading && <p style={{ color: C.textMuted, fontSize: 14 }}>{t('profile.postsLoading')}</p>}
        {!myPostsLoading && (myPostsData?.content ?? []).length === 0 && (
          <p style={{ color: C.textDim, fontSize: 14 }}>{t('profile.noPosts')}</p>
        )}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {(myPostsData?.content ?? []).map((post) => (
            <li key={post.id} style={{ borderBottom: `1px solid ${C.border}` }}>
              <Link
                href={`/posts/${post.id}`}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 4px', textDecoration: 'none', color: 'inherit' }}
              >
                <span style={{ fontSize: 14, color: C.textPrimary, fontWeight: 500 }}>
                  {post.deleted ? <span style={{ color: C.textDim }}>{t('post.deleted')}</span> : post.title}
                </span>
                {post.boardName && (
                  <span style={{ fontSize: 11, color: C.activeText, background: 'var(--color-info-soft)', padding: '2px 6px', borderRadius: 4, flexShrink: 0, marginLeft: 8 }}>
                    {post.boardName}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
        {(myPostsData?.totalElements ?? 0) > 10 && (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 16, marginTop: 16 }}>
            <button
              onClick={() => setMyPostsPage((p) => p - 1)}
              disabled={myPostsPage === 0}
              style={{ padding: '6px 16px', borderRadius: 8, border: `1px solid ${C.borderLight}`, background: myPostsPage === 0 ? C.disabled : C.card, color: myPostsPage === 0 ? C.textDim : C.textSec, cursor: myPostsPage === 0 ? 'default' : 'pointer', fontSize: 13 }}
            >{t('common.prev')}</button>
            <span style={{ color: C.textMuted, fontSize: 13 }}>{myPostsPage + 1}</span>
            <button
              onClick={() => setMyPostsPage((p) => p + 1)}
              disabled={!(myPostsData?.hasNext)}
              style={{ padding: '6px 16px', borderRadius: 8, border: `1px solid ${C.borderLight}`, background: !(myPostsData?.hasNext) ? C.disabled : C.card, color: !(myPostsData?.hasNext) ? C.textDim : C.textSec, cursor: !(myPostsData?.hasNext) ? 'default' : 'pointer', fontSize: 13 }}
            >{t('common.next')}</button>
          </div>
        )}
      </div>

      {isOwner && (
        <>
          {/* 학생 인증 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
            <h3 style={{ margin: '0 0 4px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
              {t('verification.section.title')}
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: 12, color: C.textDim }}>{t('verification.section.hint')}</p>
            <VerificationSection />
          </div>

          {/* UI 언어 설정 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
              {t('profile.uiLanguage')}
            </h3>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {SUPPORTED_UI_LANGUAGES.map((lang) => (
                <button
                  key={lang}
                  onClick={() => setLanguage(lang)}
                  style={{
                    padding: '6px 14px',
                    borderRadius: 20,
                    border: `1.5px solid ${language === lang ? C.purple : C.borderLight}`,
                    background: language === lang ? C.accentSoft : 'transparent',
                    color: language === lang ? C.purple : C.textMuted,
                    fontSize: 13,
                    fontWeight: language === lang ? 700 : 400,
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                >
                  {LANGUAGE_LABELS[lang]}
                </button>
              ))}
            </div>
            <p style={{ margin: '8px 0 0', fontSize: 12, color: C.textDim }}>{t('profile.uiLanguage.hint')}</p>
          </div>

          {/* 화면 모드 설정 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
              {t('profile.theme')}
            </h3>
            <div style={{ display: 'inline-flex', gap: 4, padding: 4, border: `1px solid ${C.border}`, borderRadius: 999, background: C.surfaceSoft }}>
              {(['light', 'dark'] as ThemeMode[]).map((mode) => {
                const active = theme === mode
                return (
                  <button
                    key={mode}
                    onClick={() => setTheme(mode)}
                    style={{
                      minWidth: 88,
                      padding: '7px 16px',
                      borderRadius: 999,
                      border: 'none',
                      background: active ? C.card : 'transparent',
                      color: active ? C.purple : C.textMuted,
                      boxShadow: active ? 'var(--shadow-soft)' : 'none',
                      fontSize: 13,
                      fontWeight: active ? 700 : 500,
                      cursor: 'pointer',
                    }}
                  >
                    {mode === 'light' ? t('profile.theme.light') : t('profile.theme.dark')}
                  </button>
                )
              })}
            </div>
            <p style={{ margin: '8px 0 0', fontSize: 12, color: C.textDim }}>{t('profile.theme.hint')}</p>
          </div>

          {/* 로그아웃 / 탈퇴 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}`, display: 'flex', gap: 12 }}>
            <button onClick={handleLogout}>{t('profile.logout')}</button>
            <button
              onClick={handleWithdraw}
              style={{ color: C.danger, border: `1px solid ${C.danger}`, background: C.dangerSoft }}
            >
              {t('profile.withdraw')}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
