import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'

let client: Client | null = null

export const stompClient = {
  connect(token: string, callbacks: { onDisconnect?: () => void } = {}): Promise<void> {
    return new Promise((resolve, reject) => {
      let resolved = false
      client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 25000,
        heartbeatOutgoing: 25000,
        // 자동 재연결 포함 매 CONNECT 직전에 최신 토큰으로 헤더 갱신
        beforeConnect: () => {
          const freshToken = useAuthStore.getState().accessToken
          if (freshToken && client) {
            client.connectHeaders = { Authorization: `Bearer ${freshToken}` }
          }
        },
        onConnect: () => {
          resolved = true
          resolve()
        },
        // 초기 연결 실패 → Promise reject / 이후 오류 → onDisconnect 처리
        onStompError: (frame) => {
          if (!resolved) reject(frame)
          else callbacks.onDisconnect?.()
        },
        onDisconnect: () => callbacks.onDisconnect?.(),
      })
      client.activate()
    })
  },

  disconnect(): void {
    client?.deactivate()
    client = null
  },

  subscribe(
    roomId: string,
    callback: (message: { messageId: number; senderId: number; content: string; sentAt: string }) => void
  ): StompSubscription | null {
    if (!client?.connected) return null
    return client.subscribe(`/topic/chat/${roomId}`, (frame) => {
      callback(JSON.parse(frame.body))
    })
  },

  subscribeRead(
    roomId: string,
    callback: (event: { memberId: number; lastReadAt: string }) => void
  ): StompSubscription | null {
    if (!client?.connected) return null
    return client.subscribe(`/topic/chat/${roomId}/read`, (frame) => {
      callback(JSON.parse(frame.body))
    })
  },

  publish(roomId: string, content: string): void {
    if (!client?.connected) return
    client.publish({
      destination: `/app/chat/${roomId}`,
      body: JSON.stringify({ content }),
    })
  },

  isConnected(): boolean {
    return client?.connected ?? false
  },
}
