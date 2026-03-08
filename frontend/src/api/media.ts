import apiClient from '../utils/axios'
import type { MediaFile } from '../types'

export const mediaApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<MediaFile>('/media', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  getById: (id: string) =>
    apiClient.get<MediaFile>(`/media/${id}`).then((r) => r.data),

  getFileUrl: (id: string) => `/api/v1/media/${id}/file`,

  delete: (id: string) => apiClient.delete(`/media/${id}`),
}
