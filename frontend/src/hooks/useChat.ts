import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { chatApi } from '../api/chat'
import type { CreateChatRoomRequest, HistoryMessage } from '../types'

export function useMarkRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (roomId: string) => chatApi.markRead(roomId),
    onSuccess: (_, roomId) => {
      queryClient.invalidateQueries({ queryKey: ['chatRooms'] })
      queryClient.invalidateQueries({ queryKey: ['chatRoom', roomId] })
    },
  })
}

export function useChatRooms() {
  return useQuery({
    queryKey: ['chatRooms'],
    queryFn: chatApi.list,
    refetchInterval: 5_000,
  })
}

export function useChatRoom(id: string) {
  return useQuery({
    queryKey: ['chatRoom', id],
    queryFn: () => chatApi.getRoom(id),
    enabled: !!id,
  })
}

export function useChatMessages(roomId: string, page = 0, size = 50) {
  return useQuery<HistoryMessage[]>({
    queryKey: ['chatMessages', roomId, page],
    queryFn: () => chatApi.getMessages(roomId, page, size),
    enabled: !!roomId,
  })
}

export function useCreateChatRoom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (req: CreateChatRoomRequest) => chatApi.createRoom(req),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chatRooms'] }),
  })
}
