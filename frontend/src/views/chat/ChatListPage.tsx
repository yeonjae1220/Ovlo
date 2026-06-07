'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useChatRooms, useCreateChatRoom } from '../../hooks/useChat'
import { useI18n } from '../../i18n/I18nProvider'
import { useMemberSearch } from '../../hooks/useMember'
import { useAuthStore } from '../../store/authStore'
import type { ChatRoomType, Member } from '../../types'
import { Avatar, Badge, Button, Card, EmptyState, PageHeader, SearchBox, SelectField, TextField } from '../../components/ui'

const C = {
  border: 'var(--color-border)',
  card: 'var(--color-surface)',
  hover: 'var(--color-surface-hover)',
  text: 'var(--color-text)',
  textSec: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent)',
  danger: 'var(--color-danger)',
}

export default function ChatListPage() {
  const { t } = useI18n()
  const { data: rooms, isLoading } = useChatRooms()
  const { currentUser } = useAuthStore()
  const createRoom = useCreateChatRoom()
  const router = useRouter()

  const [dmQuery, setDmQuery] = useState('')
  const [dmDropdownOpen, setDmDropdownOpen] = useState(false)
  const { data: dmResults } = useMemberSearch(dmQuery)
  const filteredDmResults = dmResults?.filter((m) => m.id !== currentUser?.id) ?? []

  const [showForm, setShowForm] = useState(false)
  const [type, setType] = useState<ChatRoomType>('DM')
  const [roomName, setRoomName] = useState('')
  const [nicknameQuery, setNicknameQuery] = useState('')
  const [selectedMember, setSelectedMember] = useState<Member | null>(null)

  const { data: searchResults } = useMemberSearch(nicknameQuery)
  const filteredResults = searchResults?.filter((m) => m.id !== currentUser?.id) ?? []

  const handleStartDm = (member: Member) => {
    setDmQuery('')
    setDmDropdownOpen(false)

    const currentUserId = currentUser?.id ? Number(currentUser.id) : null
    const memberId = Number(member.id)
    const existingRoom = rooms?.find(
      (room) => room.type === 'DM' &&
        room.participantIds.includes(memberId) &&
        (currentUserId === null || room.participantIds.includes(currentUserId))
    )
    if (existingRoom) {
      router.push(`/chat/${existingRoom.chatRoomId}`)
      return
    }

    createRoom.mutate(
      { type: 'DM', participantIds: [memberId] },
      { onSuccess: (room) => router.push(`/chat/${room.chatRoomId}`) }
    )
  }

  const handleSelectMember = (member: Member) => {
    setSelectedMember(member)
    setNicknameQuery(member.nickname)
  }

  const handleCreate = () => {
    if (!selectedMember) {
      window.alert(t('chat.form.selectAlert'))
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

  if (isLoading) return <p style={{ color: C.muted, padding: 24 }}>{t('chat.room.loading')}</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null

  return (
    <div style={{ maxWidth: 780, margin: '0 auto' }}>
      <PageHeader
        title={t('chat.title')}
        description={t('chat.description')}
        actions={
          <Button onClick={() => setShowForm((v) => !v)} variant="secondary" icon="+">
            {t('chat.createRoom').replace('+ ', '')}
          </Button>
        }
      />

      <Card style={{ padding: 16, marginBottom: 16 }}>
        <div style={{ position: 'relative' }}>
          <SearchBox
            placeholder={t('chat.dm.searchPlaceholder')}
            value={dmQuery}
            onChange={(event) => { setDmQuery(event.target.value); setDmDropdownOpen(true) }}
            onFocus={() => setDmDropdownOpen(true)}
            onBlur={() => setTimeout(() => setDmDropdownOpen(false), 150)}
          />

          {dmDropdownOpen && dmQuery.length >= 1 && filteredDmResults.length > 0 && (
            <ul style={{
              position: 'absolute',
              top: 'calc(100% + 6px)',
              left: 0,
              right: 0,
              zIndex: 20,
              listStyle: 'none',
              padding: 0,
              margin: 0,
              border: `1px solid ${C.border}`,
              borderRadius: 10,
              background: C.card,
              boxShadow: 'var(--shadow-soft)',
              maxHeight: 260,
              overflowY: 'auto',
            }}>
              {filteredDmResults.slice(0, 8).map((member) => (
                <li
                  key={member.id}
                  onMouseDown={() => handleStartDm(member)}
                  style={{
                    padding: '11px 14px',
                    cursor: 'pointer',
                    borderBottom: `1px solid ${C.border}`,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                  }}
                  onMouseEnter={(event) => { event.currentTarget.style.background = C.hover }}
                  onMouseLeave={(event) => { event.currentTarget.style.background = 'transparent' }}
                >
                  <Avatar label={member.nickname} />
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 850, color: C.text }}>@{member.nickname}</div>
                    <div style={{ fontSize: 12, color: C.dim }}>{member.name}</div>
                  </div>
                  <span style={{ marginLeft: 'auto', fontSize: 12, color: C.accent, fontWeight: 850 }}>{t('chat.dm.start')}</span>
                </li>
              ))}
            </ul>
          )}
          {dmDropdownOpen && dmQuery.length >= 1 && filteredDmResults.length === 0 && (
            <div style={{
              position: 'absolute',
              top: 'calc(100% + 6px)',
              left: 0,
              right: 0,
              zIndex: 20,
              padding: '12px 14px',
              border: `1px solid ${C.border}`,
              borderRadius: 10,
              background: C.card,
              color: C.dim,
              fontSize: 13,
            }}>
              {t('chat.noResults')}
            </div>
          )}
        </div>
      </Card>

      {showForm && (
        <Card style={{ padding: 16, marginBottom: 16 }}>
          <div style={{ display: 'grid', gap: 10 }}>
            <SelectField value={type} onChange={(event) => setType(event.target.value as ChatRoomType)}>
              <option value="DM">DM</option>
              <option value="GROUP">{t('chat.form.group')}</option>
            </SelectField>

            {type === 'GROUP' && (
              <TextField
                placeholder={t('chat.form.roomName')}
                value={roomName}
                onChange={(event) => setRoomName(event.target.value)}
              />
            )}

            <div style={{ position: 'relative' }}>
              <TextField
                placeholder={t('chat.form.searchMember')}
                value={nicknameQuery}
                onChange={(event) => { setNicknameQuery(event.target.value); setSelectedMember(null) }}
              />
              {nicknameQuery.length >= 1 && !selectedMember && filteredResults.length > 0 && (
                <ul style={{
                  position: 'absolute',
                  top: 'calc(100% + 6px)',
                  left: 0,
                  right: 0,
                  listStyle: 'none',
                  margin: 0,
                  padding: 0,
                  border: `1px solid ${C.border}`,
                  borderRadius: 10,
                  background: C.card,
                  zIndex: 10,
                  maxHeight: 220,
                  overflowY: 'auto',
                }}>
                  {filteredResults.map((member) => (
                    <li
                      key={member.id}
                      onClick={() => handleSelectMember(member)}
                      style={{ padding: '10px 12px', cursor: 'pointer', borderBottom: `1px solid ${C.border}`, color: C.text, fontSize: 14 }}
                      onMouseEnter={(event) => { event.currentTarget.style.background = C.hover }}
                      onMouseLeave={(event) => { event.currentTarget.style.background = 'transparent' }}
                    >
                      <strong style={{ color: C.accent }}>@{member.nickname}</strong>
                      <span style={{ color: C.dim, fontSize: 12, marginLeft: 8 }}>{member.name}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {selectedMember && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 10px', background: 'var(--color-accent-subtle)', borderRadius: 8 }}>
                <Avatar label={selectedMember.nickname} size="sm" />
                <span style={{ color: C.textSec, fontSize: 13 }}>{t('chat.form.selected')} <strong style={{ color: C.accent }}>@{selectedMember.nickname}</strong></span>
                <button
                  type="button"
                  onClick={() => { setSelectedMember(null); setNicknameQuery('') }}
                  style={{ fontSize: 16, background: 'none', border: 'none', color: C.muted, cursor: 'pointer', marginLeft: 'auto', padding: 4 }}
                >
                  ×
                </button>
              </div>
            )}

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <Button onClick={handleCreate} disabled={createRoom.isPending || !selectedMember} variant="primary">
                {t('chat.form.create')}
              </Button>
              <Button onClick={resetForm} variant="ghost">
                {t('chat.form.cancel')}
              </Button>
            </div>
            {createRoom.isError && (
              <p style={{ color: C.danger, margin: 0, fontSize: 13 }}>{t('chat.form.error')}</p>
            )}
          </div>
        </Card>
      )}

      <div style={{ display: 'grid', gap: 10 }}>
        {rooms?.map((room) => {
          const otherParticipants = room.participantIds.filter((participantId) => participantId !== currentUserId)
          const displayName = room.name ?? otherParticipants
            .map((id) => `${room.participantNicknames?.[id] ?? `#${id}`}`)
            .join(', ')
          const firstOtherId = otherParticipants[0]
          const mediaId = firstOtherId ? room.participantProfileImageMediaIds?.[firstOtherId] : null
          const avatarUrl = mediaId ? `/api/v1/media/${mediaId}/file` : null

          return (
            <Link key={room.chatRoomId} href={`/chat/${room.chatRoomId}`} style={{ color: 'inherit', textDecoration: 'none' }}>
              <Card interactive style={{ padding: 14 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <Avatar label={displayName || t('chat.room.defaultName')} imageUrl={avatarUrl} />
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 3 }}>
                      <div style={{ fontWeight: 900, fontSize: 15, color: C.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {displayName || t('chat.room.defaultName')}
                      </div>
                      <Badge tone={room.type === 'DM' ? 'accent' : 'info'}>{room.type === 'DM' ? 'DM' : t('chat.type.group')}</Badge>
                    </div>
                    <div style={{ fontSize: 13, color: C.dim, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {t('chat.participants', { count: room.participantIds.length })}
                    </div>
                  </div>
                  {room.unreadCount > 0 && (
                    <span style={{
                      background: C.danger,
                      color: '#fff',
                      borderRadius: 999,
                      padding: '4px 9px',
                      fontSize: 12,
                      fontWeight: 900,
                      minWidth: 26,
                      textAlign: 'center',
                    }}>
                      {room.unreadCount > 99 ? '99+' : room.unreadCount}
                    </span>
                  )}
                </div>
              </Card>
            </Link>
          )
        })}
      </div>

      {rooms?.length === 0 && (
        <EmptyState
          icon="⌕"
          title={t('chat.empty')}
          description={t('chat.dm.searchPlaceholder')}
        />
      )}
    </div>
  )
}
