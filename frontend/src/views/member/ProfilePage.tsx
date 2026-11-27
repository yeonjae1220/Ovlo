'use client'

import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useRouter } from 'next/navigation'
import { useMember, useUpdateProfile } from '../../hooks/useMember'
import { useFollowers, useFollowings, useFollowingStatus, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useDropzone } from 'react-dropzone'
import { useUploadMedia } from '../../hooks/useMedia'
import { memberApi } from '../../api/member'
import { authApi } from '../../api/auth'
import { captureAuthSession, isCurrentAuthSession } from '@/utils/authSession'
import { usePostsByAuthor } from '../../hooks/usePost'
import { useI18n } from '../../i18n/I18nProvider'
import { LANGUAGE_LABELS } from '../../i18n/messages'
import { uiLanguageOptions } from '../../config/pilotFocus'
import { useTheme, type ThemeMode } from '../../theme/themeContext'
import { useMyVerification } from '../../hooks/useVerification'
import { TrustBadge } from '../verification/TrustBadge'
import { VerificationSection } from '../verification/VerificationSection'
import { Avatar, QueryErrorNotice } from '../../components/ui'
import { useBlockStatus, useUnblockMember } from '../../hooks/useModeration'
import { ReportDialog } from '../../components/moderation/ReportDialog'
import { BlockConfirmDialog } from '../../components/moderation/BlockConfirmDialog'
import { AppleSignInCanceledError, type AppleCredential } from '../../utils/appleSignIn'
import { AppleWithdrawPanel } from './AppleWithdrawPanel'

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
  const queryClient = useQueryClient()
  const { language, setLanguage, t } = useI18n()
  const { theme, setTheme } = useTheme()
  const { data: member, isLoading, isError, refetch } = useMember(id!)
  const { data: followers, isError: followersError } = useFollowers(id!, 0, 1)
  const { data: followings, isError: followingsError } = useFollowings(id!, 0, 1)
  const updateProfile = useUpdateProfile()
  const followMutation = useFollow()
  const unfollowMutation = useUnfollow()
  const uploadMedia = useUploadMedia()

  const [editing, setEditing] = useState(false)
  const [nickname, setNickname] = useState('')
  const [bio, setBio] = useState('')
  const [myPostsPage, setMyPostsPage] = useState(0)
  const [actionError, setActionError] = useState('')
  const [withdrawing, setWithdrawing] = useState(false)
  const [appleWithdrawOpen, setAppleWithdrawOpen] = useState(false)
  const [savingPhoto, setSavingPhoto] = useState(false)

  const { data: myPostsData, isLoading: myPostsLoading, isError: myPostsError, refetch: refetchPosts } = usePostsByAuthor(id, myPostsPage, 10)

  const isOwner = String(currentUser?.id) === id
  const { data: followingIds } = useFollowingStatus(isOwner || !id ? [] : [id])
  const isFollowing = followingIds?.has(String(id)) ?? false
  const { data: myVerification } = useMyVerification(isOwner)
  const viewedMemberId = id && /^[1-9][0-9]*$/.test(id) ? Number(id) : null
  const canModerate = !!currentUser && !isOwner && viewedMemberId !== null
  const blockStatus = useBlockStatus(viewedMemberId, canModerate)
  const blockedByMe = blockStatus.data?.blockedByMe ?? false
  const unblockMutation = useUnblockMember()
  const [reportOpen, setReportOpen] = useState(false)
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false)

  const { getRootProps, getInputProps } = useDropzone({
    accept: { 'image/jpeg': ['.jpg', '.jpeg'], 'image/png': ['.png'] },
    maxSize: 20 * 1024 * 1024,
    onDropRejected: () => setActionError(t('profile.photoFormats')),
    maxFiles: 1,
    onDrop: async (files) => {
      if (!files[0] || !id || savingPhoto) return
      const session = captureAuthSession()
      setActionError('')
      setSavingPhoto(true)
      try {
        const media = await uploadMedia.mutateAsync(files[0])
        if (!isCurrentAuthSession(session)) return
        await memberApi.updateProfileImage(id, String(media.mediaId))
        if (isCurrentAuthSession(session)) await queryClient.invalidateQueries({ queryKey: ['member', id] })
      } catch {
        console.error('Profile photo update failed')
        if (isCurrentAuthSession(session)) setActionError(t('common.actionFailed'))
      } finally {
        if (isCurrentAuthSession(session)) setSavingPhoto(false)
      }
    },
  })

  const startEdit = () => {
    setNickname(member?.nickname ?? '')
    setBio(member?.bio ?? '')
    setEditing(true)
  }

  const saveEdit = async () => {
    if (updateProfile.isPending) return
    const session = captureAuthSession()
    setActionError('')
    try {
      await updateProfile.mutateAsync({ id: id!, req: { nickname, bio } })
      if (isCurrentAuthSession(session)) setEditing(false)
    } catch {
      console.error('Profile update failed')
      if (isCurrentAuthSession(session)) setActionError(t('common.actionFailed'))
    }
  }

  const handleLogout = async () => {
    const logout = authApi.logout()
    const session = captureAuthSession()
    try {
      await logout
    } finally {
      if (isCurrentAuthSession(session)) router.push('/login')
    }
  }

  // Apple 계정은 서버가 Apple 토큰을 폐기해야 탈퇴할 수 있어, 확인 패널에서 Apple 재인증을 받는다.
  const handleWithdraw = () => {
    if (withdrawing) return
    if (member?.loginProvider === 'APPLE') {
      setActionError('')
      setAppleWithdrawOpen(true)
      return
    }
    if (!confirm(t('profile.withdraw.confirm'))) return
    void performWithdraw()
  }

  const performWithdraw = async (appleCredential?: Promise<AppleCredential>) => {
    const session = captureAuthSession()
    setActionError('')
    setWithdrawing(true)
    try {
      const apple = appleCredential ? await appleCredential : null
      await memberApi.withdraw(id!, apple ? {
        apple: {
          identityToken: apple.identityToken,
          nonce: apple.nonce,
          authorizationCode: apple.authorizationCode,
          redirectUri: apple.redirectUri,
        },
      } : undefined)
      if (isCurrentAuthSession(session)) {
        clearAuth()
        router.push('/login')
      }
    } catch (error) {
      if (error instanceof AppleSignInCanceledError) return
      console.error('Account withdrawal failed')
      const serverMessage = (error as { response?: { data?: { error?: string } } }).response?.data?.error
      if (isCurrentAuthSession(session)) setActionError(serverMessage ?? t('common.actionFailed'))
    } finally {
      if (isCurrentAuthSession(session)) setWithdrawing(false)
    }
  }

  if (isLoading) return <p>{t('profile.loading')}</p>
  // 🔴 조회 실패를 아래 not-found 분기가 삼키면 '없다'는 **사실이 아닌 문장**이 뜬다 (GLOBAL-PIT-108).
  if (isError) return <QueryErrorNotice onRetry={() => void refetch()} />
  if (!member) return <p>{t('profile.notFound')}</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <p style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{isOwner && t('profile.photoFormats')}</p>
      {actionError && <p role="alert" style={{ color: C.danger }}>{actionError}</p>}
      <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
        {isOwner ? (
          <div {...getRootProps()} style={{ cursor: 'pointer', width: 80, height: 80, borderRadius: '50%', background: C.surfaceSoft, border: `1px solid ${C.border}`, color: C.textMuted, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <input {...getInputProps()} />
            <Avatar label={member.nickname} mediaId={member.profileImageMediaId} size="lg" />
            <span style={{ fontSize: 12 }}>{savingPhoto ? t('common.loading') : t('profile.changePhoto')}</span>
          </div>
        ) : (
          <Avatar label={member.nickname} mediaId={member.profileImageMediaId} size="lg" />
        )}

        <div style={{ flex: 1 }}>
          {editing ? (
            <>
              <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder={t('profile.nickname.placeholder')} />
              <textarea value={bio} onChange={(e) => setBio(e.target.value)} placeholder={t('profile.bio.placeholder')} rows={3} style={{ display: 'block', width: '100%' }} />
              <button onClick={saveEdit} disabled={updateProfile.isPending}>{t('profile.save')}</button>
              <button onClick={() => setEditing(false)}>{t('profile.cancel')}</button>
            </>
          ) : (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <h2 style={{ color: C.textPrimary, margin: 0 }}>{member.nickname}</h2>
                {isOwner && myVerification && <TrustBadge level={myVerification.trustLevel} />}
              </div>
              <p style={{ color: C.textMuted }}>{member.name}{member.email ? ` · ${member.email}` : ''}</p>
              {member.bio && <p style={{ color: C.textSec }}>{member.bio}</p>}
              <p style={{ color: C.textMuted }}>{t('profile.followers')} {followersError ? '—' : followers?.totalElements ?? '…'} · {t('profile.following')} {followingsError ? '—' : followings?.totalElements ?? '…'}</p>
              {(followersError || followingsError) && <p role="status">{t('common.actionFailed')}</p>}
              {isOwner ? (
                <button onClick={startEdit}>{t('profile.editBtn')}</button>
              ) : blockedByMe ? null : (
                isFollowing ? (
                  <button onClick={() => unfollowMutation.mutate(id!)}>{t('profile.unfollow')}</button>
                ) : (
                  <button onClick={() => followMutation.mutate(id!)}>{t('profile.follow')}</button>
                )
              )}
              {canModerate && (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
                  <button type="button" onClick={() => setReportOpen(true)}>{t('moderation.report.memberAction')}</button>
                  {blockStatus.isError ? (
                    <button type="button" onClick={() => void blockStatus.refetch()}>{t('moderation.block.statusRetry')}</button>
                  ) : blockedByMe ? (
                    <button
                      type="button"
                      disabled={unblockMutation.isPending}
                      onClick={() => unblockMutation.mutate(viewedMemberId!)}
                    >
                      {t('moderation.block.unblock')}
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={blockStatus.isPending}
                      onClick={() => setBlockConfirmOpen(true)}
                      style={{ color: C.danger, border: `1px solid ${C.danger}`, background: C.dangerSoft }}
                    >
                      {t('moderation.block.action')}
                    </button>
                  )}
                </div>
              )}
              {canModerate && blockedByMe && (
                <p role="status" style={{ color: C.textMuted, fontSize: 13, margin: '8px 0 0' }}>{t('moderation.block.youBlocked')}</p>
              )}
              {unblockMutation.isError && (
                <p role="alert" style={{ color: C.danger, fontSize: 13, margin: '8px 0 0' }}>{t('moderation.unblock.error')}</p>
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
        {myPostsError && <QueryErrorNotice onRetry={() => void refetchPosts()} />}
        {!myPostsLoading && !myPostsError && (myPostsData?.content ?? []).length === 0 && (
          <p style={{ color: C.textDim, fontSize: 14 }}>{t('profile.noPosts')}</p>
        )}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {(!myPostsError ? myPostsData?.content ?? [] : []).map((post) => (
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
              {uiLanguageOptions(language).map((lang) => (
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

          {/* 차단한 회원 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
            <h3 style={{ margin: '0 0 4px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
              {t('moderation.blocks.title')}
            </h3>
            <p style={{ margin: '0 0 12px', fontSize: 12, color: C.textDim }}>{t('moderation.blocks.settingsHint')}</p>
            <Link href="/blocks" style={{ color: C.purple, fontSize: 14, fontWeight: 600 }}>{t('moderation.blocks.manage')}</Link>
          </div>

          {/* 로그아웃 / 탈퇴 */}
          <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}`, display: 'flex', gap: 12 }}>
            <button onClick={handleLogout}>{t('profile.logout')}</button>
            <button
              onClick={handleWithdraw}
              disabled={withdrawing}
              style={{ color: C.danger, border: `1px solid ${C.danger}`, background: C.dangerSoft }}
            >
              {t('profile.withdraw')}
            </button>
          </div>
          {appleWithdrawOpen && (
            <AppleWithdrawPanel
              pending={withdrawing}
              error={actionError}
              onConfirm={(credential) => void performWithdraw(credential)}
              onCancel={() => setAppleWithdrawOpen(false)}
            />
          )}
        </>
      )}
      {reportOpen && viewedMemberId !== null && (
        <ReportDialog target={{ type: 'MEMBER', id: viewedMemberId }} onClose={() => setReportOpen(false)} />
      )}
      {blockConfirmOpen && viewedMemberId !== null && (
        <BlockConfirmDialog memberId={viewedMemberId} nickname={member.nickname} onClose={() => setBlockConfirmOpen(false)} />
      )}
    </div>
  )
}
