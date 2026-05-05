import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useChatRooms, useCreateChatRoom } from '../../hooks/useChat'
import { useMemberSearch } from '../../hooks/useMember'
import { useAuthStore } from '../../store/authStore'
import type { ChatRoomType, Member } from '../../types'

const C = {
  card:        '#1e2836',
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  activeBg:    '#1e3a5f',
  activeText:  '#60a5fa',
  purple:      '#a78bfa',
}

export default function ChatListPage() {
  const { data: rooms, isLoading } = useChatRooms()
  const { currentUser } = useAuthStore()
  const createRoom = useCreateChatRoom()
  const navigate = useNavigate()

  // ── 상단 사용자 검색 (DM 빠른 시작) ────────────────────────────
  const [dmQuery, setDmQuery] = useState('')
  const [dmDropdownOpen, setDmDropdownOpen] = useState(false)
  const { data: dmResults } = useMemberSearch(dmQuery)
  const filteredDmResults = dmResults?.filter((m) => m.id !== currentUser?.id) ?? []

  const handleStartDm = (member: Member) => {
    setDmQuery('')
    setDmDropdownOpen(false)

    // 이미 존재하는 DM 방 탐색
    const currentUserId = currentUser?.id ? Number(currentUser.id) : null
    const memberId = Number(member.id)
    const existingRoom = rooms?.find(
      (r) => r.type === 'DM' &&
        r.participantIds.includes(memberId) &&
        (currentUserId === null || r.participantIds.includes(currentUserId))
    )
    if (existingRoom) {
      navigate(`/chat/${existingRoom.chatRoomId}`)
      return
    }

    createRoom.mutate(
      { type: 'DM', participantIds: [memberId] },
      { onSuccess: (room) => navigate(`/chat/${room.chatRoomId}`) }
    )
  }

  // ── 채팅방 생성 폼 (GROUP 전용) ─────────────────────────────────
  const [showForm, setShowForm] = useState(false)
  const [type, setType] = useState<ChatRoomType>('DM')
  const [roomName, setRoomName] = useState('')
  const [nicknameQuery, setNicknameQuery] = useState('')
  const [selectedMember, setSelectedMember] = useState<Member | null>(null)

  const { data: searchResults } = useMemberSearch(nicknameQuery)
  const filteredResults = searchResults?.filter((m) => m.id !== currentUser?.id) ?? []

  const handleSelectMember = (member: Member) => {
    setSelectedMember(member)
    setNicknameQuery(member.nickname)
  }

  const handleCreate = () => {
    if (!selectedMember) { alert('채팅 상대를 검색하여 선택해주세요.'); return }
    createRoom.mutate(
      { type, name: roomName || undefined, participantIds: [Number(selectedMember.id)] },
      {
        onSuccess: () => {
          setShowForm(false)
          setSelectedMember(null)
          setNicknameQuery('')
          setRoomName('')
        },
      }
    )
  }

  const resetForm = () => {
    setShowForm(false)
    setSelectedMember(null)
    setNicknameQuery('')
    setRoomName('')
  }

  if (isLoading) return <p style={{ color: C.textMuted, padding: 24 }}>로딩 중...</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null

  return (
    <div style={{ maxWidth: 760, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h1 style={{ margin: 0, fontSize: 22, fontWeight: 800, color: C.textPrimary }}>채팅</h1>
        <button
          onClick={() => setShowForm((v) => !v)}
          style={{
            padding: '7px 14px', borderRadius: 8, fontSize: 13, cursor: 'pointer',
            border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.textMuted,
          }}
        >
          + 채팅방 만들기
        </button>
      </div>

      {/* ── 사용자 검색 (DM 빠른 시작) ─────────────────────────────── */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ position: 'relative' }}>
          <input
            placeholder="닉네임으로 사용자 검색하여 DM 시작..."
            value={dmQuery}
            onChange={(e) => { setDmQuery(e.target.value); setDmDropdownOpen(true) }}
            onFocus={() => setDmDropdownOpen(true)}
            onBlur={() => setTimeout(() => setDmDropdownOpen(false), 150)}
            style={{
              width: '100%', boxSizing: 'border-box',
              padding: '10px 14px 10px 38px',
              border: `1px solid ${C.borderLight}`, borderRadius: 10,
              background: C.card, color: C.textPrimary, fontSize: 14, outline: 'none',
            }}
          />
          <span style={{
            position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)',
            color: C.textDim, fontSize: 16, pointerEvents: 'none',
          }}>🔍</span>

          {dmDropdownOpen && dmQuery.length >= 1 && filteredDmResults.length > 0 && (
            <ul style={{
              position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 20,
              listStyle: 'none', padding: 0, margin: 0,
              border: `1px solid ${C.borderLight}`, borderRadius: 10,
              background: C.card, boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
              maxHeight: 240, overflowY: 'auto',
            }}>
              {filteredDmResults.slice(0, 8).map((m) => (
                <li
                  key={m.id}
                  onMouseDown={() => handleStartDm(m)}
                  style={{
                    padding: '10px 14px', cursor: 'pointer',
                    borderBottom: `1px solid ${C.border}`,
                    display: 'flex', alignItems: 'center', gap: 10,
                  }}
                  onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = C.activeBg }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
                >
                  <div style={{
                    width: 34, height: 34, borderRadius: '50%',
                    background: C.borderLight, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 14, color: C.purple, fontWeight: 700, flexShrink: 0,
                  }}>
                    {m.nickname?.[0]?.toUpperCase() ?? '?'}
                  </div>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 600, color: C.textPrimary }}>@{m.nickname}</div>
                    <div style={{ fontSize: 12, color: C.textDim }}>{m.name}</div>
                  </div>
                  <span style={{ marginLeft: 'auto', fontSize: 12, color: C.activeText }}>DM 시작 →</span>
                </li>
              ))}
            </ul>
          )}
          {dmDropdownOpen && dmQuery.length >= 1 && filteredDmResults.length === 0 && (
            <div style={{
              position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 20,
              padding: '12px 14px', border: `1px solid ${C.borderLight}`, borderRadius: 10,
              background: C.card, color: C.textDim, fontSize: 13,
            }}>
              검색 결과 없음
            </div>
          )}
        </div>
      </div>

      {/* ── 채팅방 생성 폼 ──────────────────────────────────────────── */}
      {showForm && (
        <div style={{ border: `1px solid ${C.borderLight}`, padding: 16, marginBottom: 16, borderRadius: 10, background: C.card }}>
          <select
            value={type}
            onChange={(e) => setType(e.target.value as ChatRoomType)}
            style={{ marginBottom: 8, padding: '7px 10px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: '#242424', color: C.textSec, fontSize: 13 }}
          >
            <option value="DM">DM</option>
            <option value="GROUP">그룹</option>
          </select>

          {type === 'GROUP' && (
            <input
              placeholder="채팅방 이름"
              value={roomName}
              onChange={(e) => setRoomName(e.target.value)}
              style={{ display: 'block', width: '100%', marginBottom: 8, padding: '8px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: '#242424', color: C.textPrimary, fontSize: 14, boxSizing: 'border-box' }}
            />
          )}

          <div style={{ position: 'relative', marginBottom: 8 }}>
            <input
              placeholder="닉네임으로 검색..."
              value={nicknameQuery}
              onChange={(e) => { setNicknameQuery(e.target.value); setSelectedMember(null) }}
              style={{ width: '100%', padding: '8px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: '#242424', color: C.textPrimary, fontSize: 14, boxSizing: 'border-box' }}
            />
            {nicknameQuery.length >= 1 && !selectedMember && filteredResults.length > 0 && (
              <ul style={{
                position: 'absolute', top: '100%', left: 0, right: 0,
                listStyle: 'none', margin: 0, padding: 0,
                border: `1px solid ${C.borderLight}`, borderRadius: 6, background: C.card,
                zIndex: 10, maxHeight: 200, overflowY: 'auto',
              }}>
                {filteredResults.map((m) => (
                  <li
                    key={m.id}
                    onClick={() => handleSelectMember(m)}
                    style={{ padding: '8px 12px', cursor: 'pointer', borderBottom: `1px solid ${C.border}`, color: C.textPrimary, fontSize: 14 }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = C.activeBg }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
                  >
                    <strong style={{ color: C.purple }}>@{m.nickname}</strong>
                    <span style={{ color: C.textDim, fontSize: 12, marginLeft: 8 }}>{m.name}</span>
                  </li>
                ))}
              </ul>
            )}
            {nicknameQuery.length >= 1 && !selectedMember && filteredResults.length === 0 && (
              <div style={{ padding: '6px 0', fontSize: 13, color: C.textDim }}>검색 결과 없음</div>
            )}
          </div>

          {selectedMember && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, padding: '6px 10px', background: C.activeBg, borderRadius: 6 }}>
              <span style={{ color: C.textSec, fontSize: 13 }}>선택됨: <strong style={{ color: C.purple }}>@{selectedMember.nickname}</strong> ({selectedMember.name})</span>
              <button
                type="button"
                onClick={() => { setSelectedMember(null); setNicknameQuery('') }}
                style={{ fontSize: 12, background: 'none', border: 'none', color: C.textMuted, cursor: 'pointer', marginLeft: 'auto' }}
              >
                ×
              </button>
            </div>
          )}

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={handleCreate}
              disabled={createRoom.isPending || !selectedMember}
              style={{
                padding: '7px 16px', borderRadius: 6, border: 'none',
                background: selectedMember ? C.purple : C.borderLight,
                color: '#fff', fontSize: 13, cursor: selectedMember ? 'pointer' : 'default', fontWeight: 600,
              }}
            >
              생성
            </button>
            <button
              onClick={resetForm}
              style={{ padding: '7px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.textMuted, fontSize: 13, cursor: 'pointer' }}
            >
              취소
            </button>
          </div>
          {createRoom.isError && (
            <p style={{ color: '#f87171', marginTop: 8, fontSize: 13 }}>생성 실패. 이미 DM이 존재하거나 오류가 발생했습니다.</p>
          )}
        </div>
      )}

      {/* ── 채팅방 목록 ─────────────────────────────────────────────── */}
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {rooms?.map((room) => {
          const otherParticipants = room.participantIds.filter((p) => p !== currentUserId)
          const displayName = room.name ?? otherParticipants
            .map((id) => `${room.participantNicknames?.[id] ?? `#${id}`}`)
            .join(', ')

          return (
            <li key={room.chatRoomId} style={{ borderBottom: `1px solid ${C.border}` }}>
              <Link
                to={`/chat/${room.chatRoomId}`}
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 4px', textDecoration: 'none', color: 'inherit' }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{
                    width: 40, height: 40, borderRadius: '50%', background: C.borderLight,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 15, color: C.purple, fontWeight: 700, flexShrink: 0,
                  }}>
                    {displayName?.[0]?.toUpperCase() ?? '?'}
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 14, color: C.textPrimary }}>{displayName || '채팅방'}</div>
                    <div style={{ fontSize: 12, color: C.textDim, marginTop: 2 }}>
                      {room.type === 'DM' ? 'DM' : '그룹'}
                    </div>
                  </div>
                </div>
                {room.unreadCount > 0 && (
                  <span style={{
                    background: '#ef4444', color: '#fff', borderRadius: 12,
                    padding: '2px 8px', fontSize: 12, fontWeight: 700, minWidth: 20, textAlign: 'center',
                  }}>
                    {room.unreadCount > 99 ? '99+' : room.unreadCount}
                  </span>
                )}
              </Link>
            </li>
          )
        })}
      </ul>

      {rooms?.length === 0 && (
        <p style={{ color: C.textDim, textAlign: 'center', paddingTop: 40 }}>
          채팅방이 없습니다. 위에서 사용자를 검색해 DM을 시작해보세요.
        </p>
      )}
    </div>
  )
}
