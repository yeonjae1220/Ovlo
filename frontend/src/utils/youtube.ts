/** 유튜브 영상 주소에서 영상 ID 를 뽑는다. 유튜브 주소가 아니거나 형식이 다르면 null. */
export function youtubeVideoId(url: string | null | undefined): string | null {
  if (!url) return null
  let parsed: URL
  try {
    parsed = new URL(url)
  } catch {
    return null
  }
  if (parsed.protocol !== 'https:') return null
  const host = parsed.hostname.replace(/^(www|m)\./, '')
  let id: string | null = null
  if (host === 'youtu.be') id = parsed.pathname.slice(1)
  else if (host === 'youtube.com' && parsed.pathname === '/watch') id = parsed.searchParams.get('v')
  else if (host === 'youtube.com' && parsed.pathname.startsWith('/shorts/')) id = parsed.pathname.split('/')[2] ?? null
  return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null
}

/** 화면에 걸 링크와 썸네일. 유튜브 주소로 확인된 것만 돌려준다 — DB 값이 다른 사이트 주소여도 링크를 만들지 않는다. */
export function youtubeLink(url: string | null | undefined): { href: string; thumbnail: string } | null {
  const id = youtubeVideoId(url)
  if (!id) return null
  return {
    href: `https://www.youtube.com/watch?v=${id}`,
    thumbnail: `https://i.ytimg.com/vi/${id}/mqdefault.jpg`,
  }
}
