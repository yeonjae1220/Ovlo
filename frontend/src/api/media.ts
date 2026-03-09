import apiClient from '../utils/axios'
import type { MediaFile } from '../types'

function detectMediaType(file: File): string {
  const mime = file.type.toLowerCase()
  if (mime === 'image/jpeg' || mime === 'image/jpg') return 'IMAGE_JPEG'
  if (mime === 'image/png') return 'IMAGE_PNG'
  if (mime === 'image/heic' || mime === 'image/heif') return 'IMAGE_HEIC'
  if (mime === 'image/webp') return 'IMAGE_WEBP'
  if (mime === 'video/mp4') return 'VIDEO_MP4'
  const name = file.name.toLowerCase()
  if (name.endsWith('.jpg') || name.endsWith('.jpeg')) return 'IMAGE_JPEG'
  if (name.endsWith('.png')) return 'IMAGE_PNG'
  if (name.endsWith('.heic') || name.endsWith('.heif')) return 'IMAGE_HEIC'
  if (name.endsWith('.webp')) return 'IMAGE_WEBP'
  if (name.endsWith('.mp4')) return 'VIDEO_MP4'
  return 'IMAGE_JPEG'
}

export const mediaApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('mediaType', detectMediaType(file))
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
