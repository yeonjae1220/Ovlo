import apiClient from '../utils/axios'
import type { ChatRoom, CreateChatRoomRequest, HistoryMessage } from '../types'

export const chatApi = {
  list: () => apiClient.get<ChatRoom[]>('/chat/rooms').then((r) => r.data),

  getRoom: (id: string) =>
    apiClient.get<ChatRoom>(`/chat/rooms/${id}`).then((r) => r.data),

  createRoom: (req: CreateChatRoomRequest) =>
    apiClient.post<ChatRoom>('/chat/rooms', req).then((r) => r.data),

  getMessages: (roomId: string, page = 0, size = 50) =>
    apiClient
      .get<HistoryMessage[]>(`/chat/rooms/${roomId}/messages`, { params: { page, size } })
      .then((r) => r.data),
}
