import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useChatRoom, useChatMessages } from '../../hooks/useChat'
import { useAuthStore } from '../../store/authStore'
import { stompClient } from '../../utils/stomp'
import type { HistoryMessage, Message } from '../../types'

const PAGE_SIZE = 50

function toMessage(m: HistoryMessage, chatRoomId: string): Message {
  return {
    id: String(m.messageId),
    chatRoomId,
    senderId: String(m.senderId),
    content: m.content,
    sentAt: m.sentAt,
  }
}

export default function ChatRoomPage() {
  const { id } = useParams<{ id: string }>()
  const { data: room, isLoading } = useChatRoom(id!)
  const { currentUser, accessToken } = useAuthStore()

  const [messages, setMessages] = useState<Message[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [input, setInput] = useState('')
  const [connected, setConnected] = useState(false)
  const [wsCount, setWsCount] = useState(0)

  const bottomRef = useRef<HTMLDivElement>(null)
  const historyLoadedRef = useRef(false)
  const wsBufferRef = useRef<Message[]>([])
  // Dedup by DB messageId (WebSocket도 MessageResult를 브로드캐스트하므로 동일 ID 사용)
  const seenIdsRef = useRef(new Set<string>())

  const { data: history, isFetching: loadingHistory } = useChatMessages(id!, page, PAGE_SIZE)

  // 채팅방 변경 시 상태 초기화
  useEffect(() => {
    setMessages([])
    setPage(0)
    setHasMore(true)
    historyLoadedRef.current = false
    wsBufferRef.current = []
    seenIdsRef.current = new Set()
  }, [id])

  // 히스토리 로드 → 메시지 상태에 병합
  useEffect(() => {
    if (!history) return

    const historyMsgs = history.map((m) => toMessage(m, id!))
    historyMsgs.forEach((m) => seenIdsRef.current.add(m.id))

    setMessages((prev) => {
      if (page === 0) {
        // 첫 페이지: 히스토리 + 히스토리 로드 전 도착한 WS 버퍼 메시지
        const buffered = wsBufferRef.current.filter((m) => !seenIdsRef.current.has(m.id))
        wsBufferRef.current = []
        return [...historyMsgs, ...buffered]
      }
      // 이전 페이지: 앞에 추가 (이전 메시지 불러오기)
      return [...historyMsgs, ...prev]
    })

    setHasMore(history.length === PAGE_SIZE)
    historyLoadedRef.current = true
  }, [history, page, id])

  // STOMP WebSocket 연결
  useEffect(() => {
    if (!accessToken || !id) return

    const connect = async () => {
      try {
        await stompClient.connect(accessToken)
        setConnected(true)
        stompClient.subscribe(id, (incoming) => {
          const msgId = String(incoming.messageId)
          if (seenIdsRef.current.has(msgId)) return // 이미 히스토리에 있는 메시지
          seenIdsRef.current.add(msgId)

          const msg = toMessage(incoming, id)

          if (!historyLoadedRef.current) {
            wsBufferRef.current.push(msg)
          } else {
            setMessages((prev) => [...prev, msg])
            setWsCount((c) => c + 1)
          }
        })
      } catch {
        setConnected(false)
      }
    }

    connect()
    return () => stompClient.disconnect()
  }, [id, accessToken])

  // 신규 WS 메시지 도착 시에만 자동 스크롤 (이전 메시지 로드 시 스크롤 유지)
  useEffect(() => {
    if (wsCount > 0) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [wsCount])

  const sendMessage = () => {
    if (!input.trim() || !connected) return
    stompClient.publish(id!, input)
    setInput('')
  }

  if (isLoading) return <p>로딩 중...</p>
  if (!room) return <p>채팅방을 찾을 수 없습니다.</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null
  const otherParticipants = room.participantIds.filter((p) => p !== currentUserId)
  const roomTitle =
    room.name ??
    otherParticipants
      .map((pid) => `${room.participantNicknames?.[pid] ?? `#${pid}`}`)
      .join(', ') ||
    '채팅방'

  return (
    <div style={{ maxWidth: 700, margin: '0 auto', display: 'flex', flexDirection: 'column', height: '80vh' }}>
      <h2 style={{ borderBottom: '1px solid #eee', paddingBottom: 12 }}>
        {roomTitle}
        {!connected && <span style={{ color: 'red', fontSize: 13, marginLeft: 8 }}>(연결 중...)</span>}
      </h2>

      {hasMore && (
        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={loadingHistory}
          style={{ margin: '4px 0 8px', fontSize: 13, padding: '4px 12px', alignSelf: 'flex-start' }}
        >
          {loadingHistory ? '불러오는 중...' : '이전 메시지 불러오기'}
        </button>
      )}

      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
        {messages.map((msg) => {
          const isMine = msg.senderId === currentUser?.id
          return (
            <div
              key={msg.id}
              style={{ display: 'flex', justifyContent: isMine ? 'flex-end' : 'flex-start', marginBottom: 8 }}
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
                {!isMine && (
                  <div style={{ fontSize: 11, marginBottom: 2, color: '#888' }}>
                    {room.participantNicknames?.[Number(msg.senderId)] ?? `#${msg.senderId}`}
                  </div>
                )}
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
