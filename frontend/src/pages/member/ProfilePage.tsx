import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useMember, useUpdateProfile } from '../../hooks/useMember'
import { useFollowers, useFollowings, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useDropzone } from 'react-dropzone'
import { useUploadMedia } from '../../hooks/useMedia'
import { memberApi } from '../../api/member'
import { authApi } from '../../api/auth'
import { useQueryClient } from '@tanstack/react-query'
import { usePostsByAuthor } from '../../hooks/usePost'

const C = {
  card:        '#1e2836',
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  activeText:  '#60a5fa',
  purple:      '#a78bfa',
}

export default function ProfilePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { currentUser, refreshToken, clearAuth } = useAuthStore()
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
      if (refreshToken) await authApi.logout(refreshToken)
    } finally {
      queryClient.clear()
      clearAuth()
      navigate('/login')
    }
  }

  const handleWithdraw = async () => {
    if (!confirm('정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return
    try {
      await memberApi.withdraw(id!)
    } finally {
      clearAuth()
      navigate('/login')
    }
  }

  if (isLoading) return <p>로딩 중...</p>
  if (!member) return <p>회원을 찾을 수 없습니다.</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
        {isOwner ? (
          <div {...getRootProps()} style={{ cursor: 'pointer', width: 80, height: 80, borderRadius: '50%', background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <input {...getInputProps()} />
            <span style={{ fontSize: 12 }}>사진 변경</span>
          </div>
        ) : (
          <div style={{ width: 80, height: 80, borderRadius: '50%', background: '#eee' }} />
        )}

        <div style={{ flex: 1 }}>
          {editing ? (
            <>
              <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="닉네임" />
              <textarea value={bio} onChange={(e) => setBio(e.target.value)} placeholder="소개" rows={3} style={{ display: 'block', width: '100%' }} />
              <button onClick={saveEdit}>저장</button>
              <button onClick={() => setEditing(false)}>취소</button>
            </>
          ) : (
            <>
              <h2>{member.nickname}</h2>
              <p>{member.name} · {member.email}</p>
              {member.bio && <p>{member.bio}</p>}
              <p>팔로워 {followers?.length ?? 0} · 팔로잉 {followings?.length ?? 0}</p>
              {isOwner ? (
                <button onClick={startEdit}>프로필 편집</button>
              ) : (
                isFollowing ? (
                  <button onClick={() => unfollowMutation.mutate(id!)}>언팔로우</button>
                ) : (
                  <button onClick={() => followMutation.mutate(id!)}>팔로우</button>
                )
              )}
            </>
          )}
        </div>
      </div>

      {/* 내가 쓴 글 */}
      <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}` }}>
        <h3 style={{ margin: '0 0 12px', fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
          {isOwner ? '내가 쓴 글' : '작성한 글'}
        </h3>
        {myPostsLoading && <p style={{ color: C.textMuted, fontSize: 14 }}>불러오는 중...</p>}
        {!myPostsLoading && (myPostsData?.content ?? []).length === 0 && (
          <p style={{ color: C.textDim, fontSize: 14 }}>아직 작성한 게시글이 없습니다.</p>
        )}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {(myPostsData?.content ?? []).map((post) => (
            <li key={post.id} style={{ borderBottom: `1px solid ${C.border}` }}>
              <Link
                to={`/posts/${post.id}`}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 4px', textDecoration: 'none', color: 'inherit' }}
              >
                <span style={{ fontSize: 14, color: C.textPrimary, fontWeight: 500 }}>
                  {post.deleted ? <span style={{ color: C.textDim }}>[삭제된 게시글]</span> : post.title}
                </span>
                {post.boardName && (
                  <span style={{ fontSize: 11, color: C.activeText, background: '#1e3a5f', padding: '2px 6px', borderRadius: 4, flexShrink: 0, marginLeft: 8 }}>
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
              style={{ padding: '6px 16px', borderRadius: 8, border: `1px solid ${C.borderLight}`, background: myPostsPage === 0 ? '#1a2234' : C.card, color: myPostsPage === 0 ? '#475569' : C.textSec, cursor: myPostsPage === 0 ? 'default' : 'pointer', fontSize: 13 }}
            >← 이전</button>
            <span style={{ color: C.textMuted, fontSize: 13 }}>{myPostsPage + 1}페이지</span>
            <button
              onClick={() => setMyPostsPage((p) => p + 1)}
              disabled={!(myPostsData?.hasNext)}
              style={{ padding: '6px 16px', borderRadius: 8, border: `1px solid ${C.borderLight}`, background: !(myPostsData?.hasNext) ? '#1a2234' : C.card, color: !(myPostsData?.hasNext) ? '#475569' : C.textSec, cursor: !(myPostsData?.hasNext) ? 'default' : 'pointer', fontSize: 13 }}
            >다음 →</button>
          </div>
        )}
      </div>

      {isOwner && (
        <div style={{ marginTop: 32, paddingTop: 24, borderTop: `1px solid ${C.border}`, display: 'flex', gap: 12 }}>
          <button onClick={handleLogout}>로그아웃</button>
          <button
            onClick={handleWithdraw}
            style={{ color: 'red', border: '1px solid red', background: 'transparent' }}
          >
            회원탈퇴
          </button>
        </div>
      )}
    </div>
  )
}
