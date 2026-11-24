// Cache only content-addressed build assets. Documents, API responses and avatars
// can contain account data even without an Authorization request header.
const CACHE_NAME = 'ovlo-static-v2'

self.addEventListener('install', (event) => {
  event.waitUntil(self.skipWaiting())
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k.startsWith('ovlo-') && k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return
  const url = new URL(event.request.url)
  if (url.origin !== location.origin) return
  if (!url.pathname.startsWith('/_next/static/') || event.request.headers.has('Authorization')) return

  const response = fetch(event.request).catch(async (error) => {
    const cached = await caches.match(event.request)
    if (cached) return cached
    throw error
  })
  event.waitUntil(response.then(async (res) => {
    if (res.ok) {
      const clone = res.clone()
      const cache = await caches.open(CACHE_NAME)
      await cache.put(event.request, clone)
    }
  }).catch(() => {}))
  event.respondWith(response)
})
