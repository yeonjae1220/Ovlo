import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { postApi } from '../api/post'
import type { CreatePostRequest, UpdatePostRequest, CreateCommentRequest, ReactionType } from '../types'


export function usePosts(boardId: string) {
  return useQuery({
    queryKey: ['posts', boardId],
    queryFn: () => postApi.listByBoard(boardId),
    enabled: !!boardId,
  })
}

export function useAllPosts(page = 0, size = 20) {
  return useQuery({
    queryKey: ['posts', 'all', page, size],
    queryFn: () => postApi.listAll(page, size),
  })
}

export function usePostsByAuthor(authorId: string | undefined, page = 0, size = 10) {
  return useQuery({
    queryKey: ['posts', 'author', authorId, page, size],
    queryFn: () => postApi.listByAuthor(Number(authorId), page, size),
    enabled: !!authorId,
  })
}

export function usePost(id: string) {
  return useQuery({
    queryKey: ['post', id],
    queryFn: () => postApi.getById(id),
    enabled: !!id,
  })
}

export function useCreatePost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (req: CreatePostRequest) => postApi.create(req),
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['posts', String(post.boardId)] })
      queryClient.invalidateQueries({ queryKey: ['posts', 'all'] })
    },
  })
}

export function useUpdatePost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdatePostRequest }) => postApi.update(id, req),
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['post', String(post.id)] })
      queryClient.invalidateQueries({ queryKey: ['posts', String(post.boardId)] })
    },
  })
}

export function useDeletePost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => postApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['posts'] }),
  })
}

export function useAddComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ postId, req }: { postId: string; req: CreateCommentRequest }) =>
      postApi.addComment(postId, req),
    onSuccess: (_, { postId }) => queryClient.invalidateQueries({ queryKey: ['post', postId] }),
  })
}

export function useDeleteComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ postId, commentId }: { postId: string; commentId: string }) =>
      postApi.deleteComment(postId, commentId),
    onSuccess: (_, { postId }) => queryClient.invalidateQueries({ queryKey: ['post', postId] }),
  })
}

export function useReact() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ postId, reactionType }: { postId: string; reactionType: ReactionType }) =>
      postApi.react(postId, reactionType),
    onSuccess: (_data, { postId }) =>
      queryClient.invalidateQueries({ queryKey: ['post', postId] }),
  })
}

export function useUnreact() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (postId: string) => postApi.unreact(postId),
    onSuccess: (_data, postId) =>
      queryClient.invalidateQueries({ queryKey: ['post', postId] }),
  })
}
