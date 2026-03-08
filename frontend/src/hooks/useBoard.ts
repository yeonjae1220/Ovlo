import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { boardApi } from '../api/board'
import type { CreateBoardRequest } from '../types'

export function useBoards() {
  return useQuery({
    queryKey: ['boards'],
    queryFn: () => boardApi.list(),
  })
}

export function useBoard(id: string) {
  return useQuery({
    queryKey: ['board', id],
    queryFn: () => boardApi.getById(id),
    enabled: !!id,
  })
}

export function useCreateBoard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (req: CreateBoardRequest) => boardApi.create(req),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['boards'] }),
  })
}

export function useSubscribeBoard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (boardId: string) => boardApi.subscribe(boardId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['boards'] }),
  })
}

export function useUnsubscribeBoard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (boardId: string) => boardApi.unsubscribe(boardId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['boards'] }),
  })
}
