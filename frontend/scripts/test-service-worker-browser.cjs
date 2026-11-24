// Real Chromium executes the production service worker against synthetic private responses.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const http = require('node:http')
const path = require('node:path')
const worker = fs.readFileSync(path.join(__dirname, '../public/sw.js'))

;(async () => {
  const server = http.createServer((req, res) => {
    if (req.url === '/sw.js') {
      res.writeHead(200, { 'Content-Type': 'application/javascript', 'Cache-Control': 'no-cache' })
      return res.end(worker)
    }
    if (req.url === '/api/v1/calendar/events') {
      const account = req.headers.authorization?.replace('Bearer ', '')
      res.writeHead(account ? 200 : 401, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' })
      return res.end(JSON.stringify(account ? { privateOwner: account } : { error: 'unauthenticated' }))
    }
    if (req.url === '/_next/static/chunk.js') {
      res.writeHead(200, { 'Content-Type': 'application/javascript' })
      return res.end('/* synthetic public static asset */')
    }
    res.writeHead(200, { 'Content-Type': 'text/html' })
    res.end('<!doctype html><title>Private cache regression</title>')
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] })
  try {
    const context = await browser.newContext()
    const page = await context.newPage()
    await page.goto('http://127.0.0.1:' + server.address().port)
    await page.evaluate(async () => {
      const old = await caches.open('ovlo-v1')
      await old.put('/api/v1/calendar/events', new Response('PRIVATE_A_LEGACY'))
      await caches.open('unrelated-cache')
      await navigator.serviceWorker.register('/sw.js')
      await navigator.serviceWorker.ready
    })
    await page.waitForFunction(() => !!navigator.serviceWorker.controller)
    assert.deepEqual(await page.evaluate(() => caches.keys()), ['unrelated-cache'])
    const fetchAs = account => page.evaluate(async account => {
      const response = await fetch('/api/v1/calendar/events', {
        headers: account ? { Authorization: 'Bearer ' + account } : {},
      })
      return { status: response.status, data: await response.json() }
    }, account)
    assert.equal((await fetchAs('A')).data.privateOwner, 'A')
    assert.equal((await fetchAs(null)).status, 401)
    assert.equal((await fetchAs('B')).data.privateOwner, 'B')
    await page.evaluate(() => fetch('/_next/static/chunk.js').then(r => r.text()))
    await page.waitForFunction(async () => !!await caches.match('/_next/static/chunk.js'))
    await context.setOffline(true)
    await assert.rejects(fetchAs('B'), /Failed to fetch|NetworkError/)
    assert.equal(await page.evaluate(async () => !!await caches.match('/api/v1/calendar/events')), false)
    assert.match(await page.evaluate(() => fetch('/_next/static/chunk.js').then(r => r.text())), /public static asset/)
    console.log(JSON.stringify({ passed: true, legacyPrivateCacheDeleted: true, privateAccountSwitchOfflineFallback: false, publicStaticOffline: true }))
    await context.close()
  } finally {
    await browser.close()
    await new Promise(resolve => server.close(resolve))
  }
})().catch(error => { console.error(error); process.exitCode = 1 })
