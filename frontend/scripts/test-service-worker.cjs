const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

async function main() {
  const handlers = {}, stored = new Map(), removed = []
  let offline = false
  const context = {
    URL, Request, Response,
    location: { origin: 'https://ovlo.example.test' },
    self: { addEventListener: (type, fn) => { handlers[type] = fn }, skipWaiting: async () => {}, clients: { claim: async () => {} } },
    caches: {
      keys: async () => ['ovlo-v1', 'unrelated-app'],
      delete: async name => { removed.push(name); return true },
      open: async () => ({ put: async (request, response) => stored.set(request.url, response), addAll: async () => {} }),
      match: async request => stored.get(request.url),
    },
    fetch: async () => { if (offline) throw new Error('offline'); return new Response('synthetic body') },
  }
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../public/sw.js'), 'utf8'), context)
  let activation
  handlers.activate({ waitUntil: p => { activation = p } })
  await activation
  assert.ok(removed.includes('ovlo-v1'), 'Remove previously cached private data')
  assert.ok(!removed.includes('unrelated-app'), 'Do not erase unrelated caches')
  async function request(url, headers = {}) {
    let response
    const pending = []
    handlers.fetch({ request: new Request(context.location.origin + url, { headers }),
      respondWith: p => { response = p }, waitUntil: p => pending.push(p) })
    const result = response ? await response : undefined
    await Promise.all(pending)
    return result
  }
  for (const url of ['/api/v1/calendar/events', '/api/v1/media/1/file', '/admin/calendar', '/boards', '/profile/1', '/']) {
    assert.equal(await request(url, { Authorization: 'Bearer synthetic-A' }), undefined, `Private request must bypass SW: ${url}`)
  }
  assert.equal(stored.size, 0)
  const asset = '/_next/static/chunks/test-hash.js'
  assert.equal((await request(asset)).status, 200)
  assert.equal(stored.size, 1)
  offline = true
  assert.equal(await (await request(asset)).text(), 'synthetic body')
  assert.equal(await request('/api/v1/calendar/events', { Authorization: 'Bearer synthetic-B' }), undefined)
  assert.equal(await request('/profile/1'), undefined)
  console.log('Service worker: legacy cleanup, private bypass, public static offline cache passed')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
