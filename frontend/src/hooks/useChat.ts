import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { chatApi } from '../api/chat'
import type { CreateChatRoomRequest } from '../types'

export function useChatRooms() {
  return useQuery({
    queryKey: ['chatRooms'],
    queryFn: chatApi.list,
  })
}

export function useChatRoom(id: string) {
  return useQuery({
    queryKey: ['chatRoom', id],
    queryFn: () => chatApi.getRoom(id),
    enabled: !!id,
  })
}

export function useCreateChatRoom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (req: CreateChatRoomRequest) => chatApi.createRoom(req),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chatRooms'] }),
  })
}
