import { useMutation } from '@tanstack/react-query'
import { mediaApi } from '../api/media'

export function useUploadMedia() {
  return useMutation({
    mutationFn: (file: File) => mediaApi.upload(file),
  })
}

export { mediaApi }
