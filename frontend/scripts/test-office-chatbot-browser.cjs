// Browser contract checks with deterministic API fixtures; no real student data, LLM calls or staff notifications.
// OFFICE_CHAT_URL=http://localhost:3341 PLAYWRIGHT_MODULE=/path/to/playwright node scripts/test-office-chatbot-browser.cjs
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const url = process.env.OFFICE_CHAT_URL || 'http://localhost:3341'
const output = process.env.OFFICE_CHAT_SCREENSHOTS || '/tmp/ovlo-office-chatbot-browser'

;(async () => {
  fs.mkdirSync(output, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_PATH || undefined, args: ['--no-sandbox'] })
  try {
    // Service-worker fetches bypass Playwright routing; all API requests in this test must use fixtures.
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'ko-KR', serviceWorkers: 'block' })
    const member = { id: '42', nickname: '학생', name: '학생', status: 'ACTIVE', role: 'MEMBER' }
    await context.addCookies([{ name: 'ovlo_lang', value: 'ko', url }])
    await context.addInitScript(member => {
      localStorage.setItem('ovlo-auth', JSON.stringify({ state: { currentUser: member }, version: 4 }))
    }, member)
    const turns = [], questions = [], handoffs = [], errors = []
    let consultation = null
    await context.route('**/api/v1/**', async route => {
      const request = route.request(), path = new URL(request.url()).pathname
      const json = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
      if (path.endsWith('/auth/refresh')) return json({ accessToken: 'browser-fixture-token' })
      if (path.endsWith('/office/me')) return json({ officeId: 5, officeName: '테스트대학교 국제처', universityId: 1, status: 'ACTIVE', role: null, verifiedStudent: true, pendingInvitation: false })
      if (path === '/api/v1/office/5/inquiries' && request.method() === 'POST') {
        const body = request.postDataJSON()
        if (body.question === '실패 테스트') return json({ message: '테스트 요청 실패' }, 503)
        questions.push(body)
        const result = {
          inquiryId: 900 + turns.length, outcome: turns.length === 2 ? 'NO_GROUNDS' : 'ANSWERED', lang: 'KO', canEscalate: true,
          answerText: turns.length === 2 ? null : turns.length === 0 ? '기숙사 신청은 2월 10일까지입니다. [1]' : '학교 포털에서 신청합니다. [1]',
          citations: turns.length === 2 ? [] : [{ index: 1, noticeId: 77, title: '기숙사 안내', sourceUrl: 'https://example.test/dorm', expiresAt: '2027-06-30' }],
        }
        turns.push({ question: body.question, result })
        return json(result, 201)
      }
      if (path.endsWith('/conversation')) return json({ turns, consultationId: consultation?.id ?? null, canContinue: !consultation })
      if (path === '/api/v1/office/5/consultations' && request.method() === 'POST') {
        handoffs.push(request.postDataJSON())
        consultation = {
          id: 700, officeId: 5, status: 'OPEN', inquiryId: 902, question: turns[2].question, answerText: null, lang: 'KO',
          verifiedStudent: true, chatRoomId: 300, assignedStaffMemberId: null, assignedStaffNickname: null,
          consentVersion: 'v2', openedAt: '2026-11-10T15:00:00Z', firstStaffReplyAt: null, resolvedAt: null, closedAt: null,
          conversationText: turns.map(t => `${t.question}\n${t.result.answerText ?? 'NO_GROUNDS'}`).join('\n\n'),
        }
        return json(consultation, 201)
      }
      if (path === '/api/v1/office/5/consultations/700') return json(consultation)
      if (path.endsWith('/messages')) return json([])
      return json({})
    })
    const page = await context.newPage()
    // Hold the initial route transition: another question must stay blocked until
    // the saved conversation (and its parent inquiry ID) replaces the empty route.
    let releaseSavedRoute, savedRouteRequested
    const savedRouteGate = new Promise(resolve => { releaseSavedRoute = resolve })
    const savedRouteSeen = new Promise(resolve => { savedRouteRequested = resolve })
    await page.route('**/my-university/ask?inquiry=900*', async route => {
      savedRouteRequested()
      await savedRouteGate
      await route.continue()
    })
    page.on('pageerror', e => errors.push(e.message))
    await page.goto(`${url}/my-university/ask`)
    const input = page.getByRole('textbox', { name: '질문하기' })
    await input.fill('기숙사 신청은 언제까지야?')
    await input.dispatchEvent('keydown', { key: 'Enter', code: 'Enter', isComposing: true, keyCode: 229 })
    assert.equal(questions.length, 0, 'IME commit must not submit')
    await page.getByRole('button', { name: '질문하기', exact: true }).click()
    await savedRouteSeen
    assert.equal(await input.isDisabled(), true, 'First response keeps input locked until saved route mounts')
    releaseSavedRoute()
    await page.waitForURL('**/ask?inquiry=900')
    await input.fill('어디서 신청해?')
    await input.press('Enter')
    await page.getByText('학교 포털에서 신청합니다.', { exact: false }).waitFor()
    assert.equal(questions[1].previousInquiryId, 900)
    await page.reload()
    await page.getByText('어디서 신청해?', { exact: true }).waitFor()
    await page.getByText('기숙사 신청은 언제까지야?', { exact: true }).waitFor()
    await input.fill('마감 뒤에 예외로 신청해도 돼?')
    await input.press('Enter')
    await page.getByText('담당자의 확인이 필요한 질문이에요.', { exact: false }).waitFor()
    assert.equal(questions[2].previousInquiryId, 901)
    assert.equal(handoffs.length, 0, 'No handoff without consent')
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
    await page.screenshot({ path: `${output}/mobile-conversation.png`, fullPage: true })
    await page.getByRole('button', { name: '담당자에게 전달', exact: true }).click()
    const dialog = page.getByRole('dialog')
    for (const turn of turns) await dialog.getByText(turn.question, { exact: true }).waitFor()
    await page.keyboard.press('Tab')
    assert.equal(await page.evaluate(() => document.querySelector('[role="dialog"]').contains(document.activeElement)), true)
    await dialog.getByRole('button', { name: '전달하지 않기' }).click()
    assert.equal(handoffs.length, 0)
    await page.getByRole('button', { name: '담당자에게 전달', exact: true }).click()
    await dialog.getByRole('button', { name: '동의하고 전달' }).scrollIntoViewIfNeeded()
    await page.screenshot({ path: `${output}/mobile-consent.png`, fullPage: true })
    await dialog.getByRole('button', { name: '동의하고 전달' }).click()
    await page.waitForURL('**/consultations/700')
    assert.deepEqual(handoffs, [{ inquiryId: 902, consentVersion: 'v2' }])
    await page.getByText('마감 뒤에 예외로 신청해도 돼?', { exact: true }).waitFor()
    await page.getByText('담당자에게 전달한 대화', { exact: true }).click()
    await page.getByText('기숙사 신청은 언제까지야?', { exact: false }).waitFor()
    await page.goto(`${url}/my-university/ask?inquiry=900`)
    await page.getByRole('link', { name: '학교 담당자와 이어서 상담' }).waitFor()
    assert.equal(await input.count(), 0)
    await page.getByRole('button', { name: '새 대화', exact: true }).click()
    await input.waitFor()
    assert.equal(await page.getByText('어디서 신청해?', { exact: true }).count(), 0)
    await input.fill('실패 테스트')
    await input.press('Enter')
    await page.getByRole('alert').filter({ hasText: '테스트 요청 실패' }).waitFor()
    assert.equal(await input.inputValue(), '실패 테스트', 'Failed request preserves the draft')
    assert.deepEqual(errors, [])
    console.log(JSON.stringify({ passed: true, checks: ['first response locks input until saved route mounts', 'follow-up context IDs', 'reload history', 'IME guard', 'no-grounds handoff', 'consent preview/cancel/focus', 'v2 handoff', 'staff transcript', 'resume consultation', 'new conversation', 'failed request preserves draft'], screenshots: output }))
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exitCode = 1 })
