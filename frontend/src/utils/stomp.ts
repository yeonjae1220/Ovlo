import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client: Client | null = null

export const stompClient = {
  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => resolve(),
        onStompError: (frame) => reject(frame),
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
    callback: (message: { senderId: string; content: string; sentAt: string }) => void
  ): StompSubscription | null {
    if (!client?.connected) return null
    return client.subscribe(`/topic/chat/${roomId}`, (frame) => {
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
