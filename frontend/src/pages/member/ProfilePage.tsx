import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useMember, useUpdateProfile } from '../../hooks/useMember'
import { useFollowers, useFollowings, useFollow, useUnfollow } from '../../hooks/useFollow'
import { useAuthStore } from '../../store/authStore'
import { useDropzone } from 'react-dropzone'
import { useUploadMedia } from '../../hooks/useMedia'
import { memberApi } from '../../api/member'
import { authApi } from '../../api/auth'
import { useQueryClient } from '@tanstack/react-query'

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
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
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

      {isOwner && (
        <div style={{ marginTop: 32, paddingTop: 24, borderTop: '1px solid #eee', display: 'flex', gap: 12 }}>
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
