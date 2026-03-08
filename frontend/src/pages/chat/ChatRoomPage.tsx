import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useChatRoom } from '../../hooks/useChat'
import { useAuthStore } from '../../store/authStore'
import { stompClient } from '../../utils/stomp'
import type { Message } from '../../types'

export default function ChatRoomPage() {
  const { id } = useParams<{ id: string }>()
  const { data: room, isLoading } = useChatRoom(id!)
  const { currentUser, accessToken } = useAuthStore()

  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [connected, setConnected] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  // Connect STOMP
  useEffect(() => {
    if (!accessToken || !id) return

    const connect = async () => {
      try {
        await stompClient.connect(accessToken)
        setConnected(true)
        stompClient.subscribe(id, (msg) => {
          setMessages((prev) => [
            ...prev,
            {
              id: Date.now().toString(),
              chatRoomId: id,
              senderId: msg.senderId,
              content: msg.content,
              sentAt: msg.sentAt,
            },
          ])
        })
      } catch {
        setConnected(false)
      }
    }

    connect()
    return () => stompClient.disconnect()
  }, [id, accessToken])

  // Auto-scroll
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const sendMessage = () => {
    if (!input.trim() || !connected) return
    stompClient.publish(id!, input)
    setInput('')
  }

  if (isLoading) return <p>로딩 중...</p>
  if (!room) return <p>채팅방을 찾을 수 없습니다.</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null
  const otherParticipants = room.participantIds.filter((p) => p !== currentUserId)
  const roomTitle = room.name ?? (otherParticipants.join(', ') || '채팅방')

  return (
    <div style={{ maxWidth: 700, margin: '0 auto', display: 'flex', flexDirection: 'column', height: '80vh' }}>
      <h2 style={{ borderBottom: '1px solid #eee', paddingBottom: 12 }}>
        {roomTitle}
        {!connected && <span style={{ color: 'red', fontSize: 13, marginLeft: 8 }}>(연결 중...)</span>}
      </h2>

      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
        {messages.map((msg) => {
          const isMine = msg.senderId === currentUser?.id
          return (
            <div
              key={msg.id}
              style={{
                display: 'flex',
                justifyContent: isMine ? 'flex-end' : 'flex-start',
                marginBottom: 8,
              }}
            >
              <div
                style={{
                  maxWidth: '70%',
                  padding: '8px 12px',
                  borderRadius: 12,
                  background: isMine ? '#007bff' : '#f0f0f0',
                  color: isMine ? '#fff' : '#000',
                }}
              >
                {!isMine && <div style={{ fontSize: 11, marginBottom: 2, color: '#888' }}>{msg.senderId}</div>}
                {msg.content}
                <div style={{ fontSize: 10, color: isMine ? 'rgba(255,255,255,0.7)' : '#aaa', marginTop: 4, textAlign: 'right' }}>
                  {new Date(msg.sentAt).toLocaleTimeString()}
                </div>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <div style={{ display: 'flex', gap: 8, paddingTop: 12, borderTop: '1px solid #eee' }}>
        <input
          placeholder="메시지를 입력하세요..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
          style={{ flex: 1, padding: 8 }}
          disabled={!connected}
        />
        <button onClick={sendMessage} disabled={!connected || !input.trim()}>전송</button>
      </div>
    </div>
  )
}
