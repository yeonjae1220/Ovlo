'use client'

import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { mediaApi } from '../api/media'
import { useAuthStore } from '../store/authStore'

/** Fetch protected images through the authenticated transport; never put tokens in URLs. */
export function useMediaUrl(mediaId?: string | null) {
  const sessionVersion = useAuthStore((state) => state.sessionVersion)
  const authenticated = useAuthStore((state) => !!state.accessToken)
  const { data } = useQuery({
    queryKey: ['media-file', sessionVersion, mediaId],
    queryFn: ({ signal }) => mediaApi.download(mediaId!, signal),
    enabled: authenticated && !!mediaId,
    gcTime: 0,
    retry: false,
  })
  const [resource, setResource] = useState<{ blob: Blob; url: string; session: number }>()
  useEffect(() => {
    if (!data) return
    const url = URL.createObjectURL(data)
    // eslint-disable-next-line react-hooks/set-state-in-effect -- Own and revoke a browser resource.
    setResource({ blob: data, url, session: sessionVersion })
    return () => URL.revokeObjectURL(url)
  }, [data, sessionVersion])
  return authenticated && resource?.blob === data && resource?.session === sessionVersion ? resource.url : undefined
}
