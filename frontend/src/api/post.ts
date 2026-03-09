import apiClient from '../utils/axios'
import type { Post, Comment, UpdatePostRequest, CreateCommentRequest } from '../types'

interface PostPageResult {
  content: Post[]
  totalElements: number
  page: number
  size: number
}

export const postApi = {
  listByBoard: (boardId: string, page = 0, size = 20) =>
    apiClient
      .get<PostPageResult>('/posts', { params: { boardId, page, size } })
      .then((r) => r.data.content),

  getById: (id: string) =>
    apiClient.get<Post>(`/posts/${id}`).then((r) => r.data),

  create: (req: { boardId: number; title: string; content: string }) =>
    apiClient.post<Post>('/posts', req).then((r) => r.data),

  update: (id: string, req: UpdatePostRequest) =>
    apiClient.put<Post>(`/posts/${id}`, req).then((r) => r.data),

  delete: (id: string) => apiClient.delete(`/posts/${id}`),

  addComment: (postId: string, req: CreateCommentRequest) =>
    apiClient.post<Comment>(`/posts/${postId}/comments`, req).then((r) => r.data),

  deleteComment: (postId: string, commentId: string) =>
    apiClient.delete(`/posts/${postId}/comments/${commentId}`),

  react: (postId: string, reactionType: 'LIKE' | 'DISLIKE') =>
    apiClient.post(`/posts/${postId}/reactions`, { reactionType }),

  unreact: (postId: string) => apiClient.delete(`/posts/${postId}/reactions`),
}
