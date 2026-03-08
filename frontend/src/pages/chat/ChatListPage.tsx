import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useChatRooms, useCreateChatRoom } from '../../hooks/useChat'
import { useMemberSearch } from '../../hooks/useMember'
import { useAuthStore } from '../../store/authStore'
import type { ChatRoomType, Member } from '../../types'

export default function ChatListPage() {
  const { data: rooms, isLoading } = useChatRooms()
  const { currentUser } = useAuthStore()
  const createRoom = useCreateChatRoom()

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
    if (!selectedMember) {
      alert('채팅 상대를 검색하여 선택해주세요.')
      return
    }
    createRoom.mutate(
      { type, name: roomName || undefined, participantIds: [Number(selectedMember.id)] },
      {
        onSuccess: () => {
          setShowForm(false)
          setSelectedMember(null)
          setNicknameQuery('')
          setRoomName('')
        }
      }
    )
  }

  const resetForm = () => {
    setShowForm(false)
    setSelectedMember(null)
    setNicknameQuery('')
    setRoomName('')
  }

  if (isLoading) return <p>로딩 중...</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>채팅</h1>
        <button onClick={() => setShowForm(!showForm)}>+ 채팅방 만들기</button>
      </div>

      {showForm && (
        <div style={{ border: '1px solid #ddd', padding: 16, marginBottom: 16, borderRadius: 8 }}>
          <select value={type} onChange={(e) => setType(e.target.value as ChatRoomType)} style={{ marginBottom: 8 }}>
            <option value="DM">DM</option>
            <option value="GROUP">그룹</option>
          </select>

          {type === 'GROUP' && (
            <input
              placeholder="채팅방 이름"
              value={roomName}
              onChange={(e) => setRoomName(e.target.value)}
              style={{ display: 'block', width: '100%', marginBottom: 8 }}
            />
          )}

          <div style={{ position: 'relative', marginBottom: 8 }}>
            <input
              placeholder="닉네임으로 검색..."
              value={nicknameQuery}
              onChange={(e) => { setNicknameQuery(e.target.value); setSelectedMember(null) }}
              style={{ width: '100%' }}
            />
            {nicknameQuery.length >= 1 && !selectedMember && filteredResults.length > 0 && (
              <ul style={{
                position: 'absolute', top: '100%', left: 0, right: 0,
                listStyle: 'none', margin: 0, padding: 0,
                border: '1px solid #ddd', borderRadius: 4, background: '#fff',
                zIndex: 10, maxHeight: 200, overflowY: 'auto',
              }}>
                {filteredResults.map((m) => (
                  <li
                    key={m.id}
                    onClick={() => handleSelectMember(m)}
                    style={{ padding: '8px 12px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0' }}
                  >
                    <strong>@{m.nickname}</strong>
                    <span style={{ color: '#888', fontSize: 12, marginLeft: 8 }}>{m.name}</span>
                  </li>
                ))}
              </ul>
            )}
            {nicknameQuery.length >= 1 && !selectedMember && filteredResults.length === 0 && (
              <div style={{ padding: '6px 0', fontSize: 13, color: '#888' }}>검색 결과 없음</div>
            )}
          </div>

          {selectedMember && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, padding: '6px 10px', background: '#f0f7ff', borderRadius: 4 }}>
              <span>선택됨: <strong>@{selectedMember.nickname}</strong> ({selectedMember.name})</span>
              <button type="button" onClick={() => { setSelectedMember(null); setNicknameQuery('') }} style={{ fontSize: 12 }}>×</button>
            </div>
          )}

          <button onClick={handleCreate} disabled={createRoom.isPending || !selectedMember}>생성</button>
          <button onClick={resetForm} style={{ marginLeft: 8 }}>취소</button>
          {createRoom.isError && (
            <p style={{ color: 'red', marginTop: 8, fontSize: 13 }}>생성 실패. 다시 시도해주세요.</p>
          )}
        </div>
      )}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {rooms?.map((room) => {
          const otherParticipants = room.participantIds.filter((p) => p !== currentUserId)
          const displayName = room.name ?? otherParticipants
            .map((id) => `#${id} ${room.participantNicknames?.[id] ?? ''}`.trim())
            .join(', ')

          return (
            <li key={room.chatRoomId} style={{ borderBottom: '1px solid #eee' }}>
              <Link to={`/chat/${room.chatRoomId}`} style={{ display: 'block', padding: '12px 0', textDecoration: 'none', color: 'inherit' }}>
                <div style={{ fontWeight: 'bold' }}>{displayName || '채팅방'}</div>
              </Link>
            </li>
          )
        })}
      </ul>

      {rooms?.length === 0 && <p>채팅방이 없습니다.</p>}
    </div>
  )
}
