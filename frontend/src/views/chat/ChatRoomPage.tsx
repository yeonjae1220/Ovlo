'use client'

import { useEffect, useRef, useState, useCallback } from 'react'
import { useParams } from 'next/navigation'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { useChatRoom, useChatMessages, useMarkRead } from '../../hooks/useChat'
import { useAuthStore } from '../../store/authStore'
import { useBreakpoint } from '../../hooks/useBreakpoint'
import { stompClient } from '../../utils/stomp'
import type { HistoryMessage, Message } from '../../types'
import { useI18n } from '../../i18n/I18nProvider'
import { Avatar, Badge, Button, Card, QueryErrorNotice, TextField } from '../../components/ui'
import { useBlockStatus, useUnblockMember } from '../../hooks/useModeration'
import { ReportDialog, type ReportTarget } from '../../components/moderation/ReportDialog'
import { BlockConfirmDialog } from '../../components/moderation/BlockConfirmDialog'

const PAGE_SIZE = 50
/**
 * 서버가 거절한 전송은 /user/queue/errors 로 곧바로 알려 온다.
 * 그 알림도 방송도 이 시간 안에 오지 않으면(연결이 끊겼거나 구버전 서버) '전달되지 않음'으로 표시한다.
 */
const SEND_ACK_TIMEOUT_MS = 8000

type PendingSend = { clientId: string; content: string; timer: ReturnType<typeof setTimeout> | undefined }
const C = {
  border: 'var(--color-border)',
  surface: 'var(--color-surface)',
  surfaceSoft: 'var(--color-surface-soft)',
  text: 'var(--color-text)',
  textSecondary: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent-strong)',
  onAccent: 'var(--color-on-accent)',
  danger: 'var(--color-danger)',
}

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
  const { t } = useI18n()
  const params = useParams()
  const id = params?.id as string | undefined
  const router = useRouter()
  const queryClient = useQueryClient()
  const { data: room, isLoading, isError, refetch } = useChatRoom(id!)
  const { currentUser, accessToken } = useAuthStore()
  const { isMobile } = useBreakpoint()
  const markRead = useMarkRead()

  const [messages, setMessages] = useState<Message[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [input, setInput] = useState('')
  const [connected, setConnected] = useState(false)
  const [wsCount, setWsCount] = useState(0)
  const [reconnectVersion, setReconnectVersion] = useState(0)
  const [sendFailed, setSendFailed] = useState(false)
  const [reportTarget, setReportTarget] = useState<ReportTarget | null>(null)
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false)

  const bottomRef = useRef<HTMLDivElement>(null)
  const historyLoadedRef = useRef(false)
  const wsBufferRef = useRef<Message[]>([])
  const seenIdsRef = useRef(new Set<string>())
  // 보냈지만 아직 서버 방송으로 되돌아오지 않은 내 메시지(보낸 순서)
  // 같은 배열을 제자리에서 고친다 — 소켓 콜백과 타이머가 서로 다른 렌더에서 같은 목록을 봐야 한다.
  const pendingSendsRef = useRef<PendingSend[]>([])
  // 소켓 콜백(doConnect)은 첫 렌더의 클로저를 쥐므로 최신 실패 처리기를 ref 로 넘긴다.
  const markSendFailedRef = useRef<(entry: PendingSend) => void>(() => {})
  const currentUserIdRef = useRef(currentUser?.id)
  useEffect(() => { currentUserIdRef.current = currentUser?.id }, [currentUser?.id])

  // 최신 accessToken/id를 ref에 유지 — visibilitychange 핸들러의 stale closure 방지
  const accessTokenRef = useRef(accessToken)
  const idRef = useRef(id)
  useEffect(() => { accessTokenRef.current = accessToken }, [accessToken])
  useEffect(() => { idRef.current = id }, [id])

  const { data: history, isFetching: loadingHistory } = useChatMessages(id!, page, PAGE_SIZE)

  // 채팅방 변경 시 상태 초기화 — id(라우트)라는 외부 입력에 맞춰 상태를 리셋하는 의도된 동기화
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    setMessages([])
    setPage(0)
    setHasMore(true)
    historyLoadedRef.current = false
    wsBufferRef.current = []
    seenIdsRef.current = new Set()
    setSendFailed(false)
  }, [id])
  /* eslint-enable react-hooks/set-state-in-effect */

  // 방을 떠나면 대기 중인 전달 확인 타이머를 정리한다
  useEffect(() => {
    const pending = pendingSendsRef.current
    return () => {
      pending.forEach((p) => clearTimeout(p.timer))
      pending.splice(0, pending.length)
    }
  }, [id])

  // 히스토리 로드 → 메시지 상태에 병합
  useEffect(() => {
    if (!history) return

    const historyMsgs = history.map((m) => toMessage(m, id!))
    const historyIdSet = new Set(historyMsgs.map((m) => m.id))
    historyMsgs.forEach((m) => seenIdsRef.current.add(m.id))

    if (page === 0) {
      // 히스토리에 없는 버퍼 메시지 flush (seenIds 대신 historyIdSet으로 판별)
      const buffered = wsBufferRef.current.filter((m) => !historyIdSet.has(m.id))
      wsBufferRef.current = []
      setMessages((prev) => {
        // 이미 화면에 있는 WS 메시지 중 히스토리에 없는 것도 유지 (refetch 직후 도착한 메시지)
        const stateExtra = prev.filter((m) => !historyIdSet.has(m.id))
        return [...new Map([...historyMsgs, ...buffered, ...stateExtra].map(m => [m.id, m])).values()].sort((a, b) => a.sentAt.localeCompare(b.sentAt) || Number(a.id) - Number(b.id))
      })
    } else {
      // 이전 페이지: 앞에 추가 (이전 메시지 불러오기)
      setMessages((prev) => [...historyMsgs.filter(m => !prev.some(p => p.id === m.id)), ...prev])
    }

    setHasMore(history.length === PAGE_SIZE)
    historyLoadedRef.current = true
  // reconnectVersion: doConnect 후 history 데이터가 바뀌지 않아도 effect 재실행 보장
  }, [history, page, id, reconnectVersion])

  // connect + subscribe 로직을 콜백으로 추출 — 메인 effect와 visibilitychange 핸들러에서 공유
  const doConnect = useCallback(async (token: string, roomId: string) => {
    try {
      await stompClient.connect(token, {
        onDisconnect: () => setConnected(false),
        onConnect: () => {
      setConnected(true)

      // 재연결 시 오프라인 중 수신된 메시지 재조회
      setPage(0)
      historyLoadedRef.current = false
      wsBufferRef.current = []
      seenIdsRef.current = new Set()
      queryClient.invalidateQueries({ queryKey: ['chatMessages', roomId, 0] })
      setReconnectVersion((v) => v + 1)

      stompClient.subscribeRead(roomId, () => {
        queryClient.invalidateQueries({ queryKey: ['chatRoom', roomId] })
      })
      stompClient.subscribeErrors((error) => {
        if (String(error.roomId) !== roomId) return
        const pending = pendingSendsRef.current
        const failed = error.clientMessageId === null
          ? pending[0]
          : pending.find((p) => p.clientId === error.clientMessageId)
        if (failed) markSendFailedRef.current(failed)
      })
      stompClient.subscribe(roomId, (incoming) => {
        if (String(incoming.senderId) === String(currentUserIdRef.current)) {
          const acknowledged = pendingSendsRef.current.shift()
          if (acknowledged) clearTimeout(acknowledged.timer)
        }
        const msgId = String(incoming.messageId)
        if (seenIdsRef.current.has(msgId)) return
        seenIdsRef.current.add(msgId)

        const msg = toMessage(incoming, roomId)
        if (!historyLoadedRef.current) {
          wsBufferRef.current.push(msg)
        } else {
          setMessages((prev) => [...prev, msg])
          setWsCount((c) => c + 1)
          queryClient.invalidateQueries({ queryKey: ['chatRooms'] })
        }
      })
        },
      })
    } catch {
      setConnected(false)
    }
  }, [queryClient])

  // STOMP WebSocket 연결 (accessToken 갱신 시 재연결 포함)
  useEffect(() => {
    if (!accessToken || !id) return
    // doConnect는 WebSocket(외부 시스템) 연결 콜백 — 내부 setState는 의도된 동기화
    // eslint-disable-next-line react-hooks/set-state-in-effect
    doConnect(accessToken, id)
    return () => stompClient.disconnect()
  }, [id, accessToken, doConnect])

  // 탭 복귀 시 WebSocket 재연결 (모바일 백그라운드 처리)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState !== 'visible') return
      if (stompClient.isConnected()) return
      const token = accessTokenRef.current
      const roomId = idRef.current
      if (!token || !roomId) return
      stompClient.disconnect() // 기존 재연결 루프 정리
      doConnect(token, roomId)
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [doConnect])

  // 채팅방 입장 + 새 WS 메시지 도착 시 읽음 처리
  useEffect(() => {
    if (id) markRead.mutate(id)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, wsCount])

  // 신규 WS 메시지 도착 시에만 자동 스크롤 (이전 메시지 로드 시 스크롤 유지)
  useEffect(() => {
    if (wsCount > 0) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [wsCount])

  const myId = currentUser?.id ? Number(currentUser.id) : null
  const dmPartnerIds = room && room.type === 'DM' ? room.participantIds.filter((p) => p !== myId) : []
  const dmPartnerId = dmPartnerIds.length === 1 ? dmPartnerIds[0] : null
  const blockStatus = useBlockStatus(dmPartnerId, dmPartnerId !== null && !!currentUser)
  const blockedByMe = blockStatus.data?.blockedByMe ?? false
  const unblock = useUnblockMember()

  const markSendFailed = (entry: PendingSend) => {
    const pending = pendingSendsRef.current
    const index = pending.indexOf(entry)
    if (index < 0) return
    pending.splice(index, 1)
    clearTimeout(entry.timer)
    const content = entry.content
    setSendFailed(true)
    setInput((current) => (current.trim() ? current : content))
    if (dmPartnerId !== null) void blockStatus.refetch()
  }

  useEffect(() => { markSendFailedRef.current = markSendFailed })

  const sendMessage = () => {
    if (!input.trim() || !connected || blockedByMe) return
    const content = input
    const clientId = crypto.randomUUID()
    if (stompClient.publish(id!, content, clientId)) {
      setInput('')
      setSendFailed(false)
      const entry: PendingSend = { clientId, content, timer: undefined }
      entry.timer = setTimeout(() => markSendFailedRef.current(entry), SEND_ACK_TIMEOUT_MS)
      pendingSendsRef.current.push(entry)
    } else {
      setConnected(false)
    }
  }

  if (isLoading) return <p style={{ color: C.muted }}>{t('chat.room.loading')}</p>
  // 🔴 조회 실패를 아래 not-found 분기가 삼키면 '없다'는 **사실이 아닌 문장**이 뜬다 (GLOBAL-PIT-108).
  if (isError) return <QueryErrorNotice onRetry={() => void refetch()} />
  if (!room) return <p style={{ color: C.muted }}>{t('chat.room.notFound')}</p>

  const currentUserId = currentUser?.id ? Number(currentUser.id) : null
  const otherParticipants = room.participantIds.filter((p) => p !== currentUserId)

  // 내 메시지의 읽음 여부: 상대방 전원의 lastReadAt >= sentAt 이면 ✓✓
  const getReadStatus = (sentAt: string): '✓✓' | '✓' => {
    const allRead = otherParticipants.every((pid) => {
      const lastReadStr = room.participantLastReadAt?.[pid]
      if (!lastReadStr) return false
      return new Date(lastReadStr) >= new Date(sentAt)
    })
    return allRead ? '✓✓' : '✓'
  }
  const roomTitle =
    room.name ??
    (otherParticipants
      .map((pid) => `${room.participantNicknames?.[pid] ?? `#${pid}`}`)
      .join(', ') ||
    t('chat.room.defaultName'))
  const containerHeight = isMobile ? 'calc(100dvh - 150px)' : '80vh'

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', height: containerHeight, minHeight: isMobile ? 420 : 520 }}>
      <Card style={{ padding: isMobile ? 12 : 14, marginBottom: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Avatar label={roomTitle} />
          <div style={{ minWidth: 0, flex: 1 }}>
            <h2 style={{ color: C.text, margin: 0, fontSize: isMobile ? 18 : 21, lineHeight: 1.25, overflowWrap: 'anywhere' }}>
              {roomTitle}
            </h2>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 5, flexWrap: 'wrap' }}>
              <Badge tone={connected ? 'success' : 'danger'}>{connected ? t('chat.online') : t('chat.room.connecting')}</Badge>
              <span style={{ color: C.dim, fontSize: 12 }}>{t('chat.participants', { count: room.participantIds.length })}</span>
            </div>
          </div>
        </div>
        {dmPartnerId !== null && (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
            <Button variant="ghost" icon="⚑" style={{ minHeight: 34 }} onClick={() => setReportTarget({ type: 'MEMBER', id: dmPartnerId })}>
              {t('moderation.report.memberAction')}
            </Button>
            {blockStatus.isError ? (
              <Button variant="ghost" style={{ minHeight: 34 }} onClick={() => void blockStatus.refetch()}>
                {t('moderation.block.statusRetry')}
              </Button>
            ) : blockedByMe ? (
              <Button variant="secondary" style={{ minHeight: 34 }} disabled={unblock.isPending} onClick={() => unblock.mutate(dmPartnerId)}>
                {t('moderation.block.unblock')}
              </Button>
            ) : (
              <Button variant="danger" style={{ minHeight: 34 }} disabled={blockStatus.isPending} onClick={() => setBlockConfirmOpen(true)}>
                {t('moderation.block.action')}
              </Button>
            )}
          </div>
        )}
        {unblock.isError && (
          <p role="alert" style={{ color: C.danger, fontSize: 13, margin: '8px 0 0' }}>{t('moderation.unblock.error')}</p>
        )}
      </Card>

      {hasMore && (
        <Button
          onClick={() => setPage((p) => p + 1)}
          disabled={loadingHistory}
          variant="ghost"
          style={{ margin: '0 0 8px', alignSelf: 'flex-start', minHeight: 34 }}
        >
          {loadingHistory ? t('chat.room.loading') : t('chat.room.loadMore')}
        </Button>
      )}

      <div style={{ flex: 1, overflowY: 'auto', padding: isMobile ? '8px 2px' : '10px 4px' }}>
        {messages.map((msg) => {
          const isMine = String(msg.senderId) === String(currentUser?.id)
          const senderIdNum = Number(msg.senderId)
          const nickname = room.participantNicknames?.[senderIdNum] ?? `#${msg.senderId}`
          const mediaId = room.participantProfileImageMediaIds?.[senderIdNum]

          return (
            <div
              key={msg.id}
              style={{ display: 'flex', justifyContent: isMine ? 'flex-end' : 'flex-start', alignItems: 'flex-end', gap: 8, marginBottom: 8 }}
            >
              {!isMine && (
                <div
                  onClick={() => router.push(`/profile/${msg.senderId}`)}
                  style={{ cursor: 'pointer', flexShrink: 0 }}
                  title={nickname}
                >
                  <Avatar label={nickname} mediaId={mediaId} size="sm" />
                </div>
              )}
              <div
                style={{
                  maxWidth: isMobile ? '82%' : '70%',
                  padding: isMobile ? '11px 13px' : '10px 13px',
                  borderRadius: isMine ? '14px 14px 4px 14px' : '14px 14px 14px 4px',
                  background: isMine ? C.accent : C.surface,
                  color: isMine ? C.onAccent : C.text,
                  border: isMine ? 'none' : `1px solid ${C.border}`,
                  overflowWrap: 'anywhere',
                  wordBreak: 'break-word',
                  boxShadow: isMine ? '0 10px 24px rgb(13 107 96 / 0.18)' : 'none',
                }}
              >
                {!isMine && (
                  <div style={{ fontSize: 11, marginBottom: 2, color: C.muted }}>{nickname}</div>
                )}
                {msg.content}
                <div style={{ fontSize: 10, color: isMine ? 'rgba(255,255,255,0.7)' : C.dim, marginTop: 4, textAlign: 'right', display: 'flex', justifyContent: 'flex-end', gap: 4, alignItems: 'center' }}>
                  {!isMine && currentUser && /^[1-9][0-9]*$/.test(msg.id) && (
                    <button
                      type="button"
                      onClick={() => setReportTarget({ type: 'CHAT_MESSAGE', id: Number(msg.id) })}
                      aria-label={t('moderation.report.messageAria', { name: nickname })}
                      title={t('moderation.report.title.CHAT_MESSAGE')}
                      style={{ marginRight: 'auto', color: C.dim, fontSize: 11, background: 'none', border: 'none', cursor: 'pointer', padding: '2px 4px 2px 0', minHeight: 24 }}
                    >
                      {t('moderation.report.action')}
                    </button>
                  )}
                  {isMine && (
                    <span style={{ opacity: 0.8 }}>{getReadStatus(msg.sentAt)}</span>
                  )}
                  {new Date(msg.sentAt).toLocaleTimeString()}
                </div>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <Card style={{ padding: 10, marginTop: 10 }}>
        {blockedByMe && (
          <p role="status" style={{ margin: '0 0 8px', fontSize: 13, color: C.textSecondary }}>{t('chat.blocked.byMe')}</p>
        )}
        {sendFailed && (
          <p role="alert" style={{ margin: '0 0 8px', fontSize: 13, color: C.danger }}>
            {dmPartnerId !== null ? t('chat.send.notDeliveredDm') : t('chat.send.notDelivered')}
          </p>
        )}
        <div style={{ display: 'flex', gap: 8, alignItems: isMobile ? 'stretch' : 'center', flexDirection: isMobile ? 'column' : 'row' }}>
          <TextField
            placeholder={t('chat.room.msgPlaceholder')}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            style={{ flex: 1, fontSize: 16 }}
            disabled={!connected || blockedByMe}
          />
          <Button
            onClick={sendMessage}
            disabled={!connected || blockedByMe || !input.trim()}
            variant="primary"
            style={{ width: isMobile ? '100%' : 'auto', minHeight: isMobile ? 44 : undefined }}
          >
            {t('chat.room.send')}
          </Button>
        </div>
      </Card>
      {reportTarget && (
        <ReportDialog key={`${reportTarget.type}-${reportTarget.id}`} target={reportTarget} onClose={() => setReportTarget(null)} />
      )}
      {blockConfirmOpen && dmPartnerId !== null && (
        <BlockConfirmDialog
          memberId={dmPartnerId}
          nickname={room.participantNicknames?.[dmPartnerId] ?? `#${dmPartnerId}`}
          onClose={() => setBlockConfirmOpen(false)}
        />
      )}
    </div>
  )
}
