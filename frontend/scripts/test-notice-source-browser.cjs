// Deterministic browser contract test. No real source approval, student data or notifications.
// NOTICE_SOURCE_URL=http://localhost:3347 PLAYWRIGHT_MODULE=/path/to/playwright-core node scripts/test-notice-source-browser.cjs
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const url = process.env.NOTICE_SOURCE_URL || 'http://localhost:3347'
;(async () => {
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] })
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'ko-KR', serviceWorkers: 'block' })
    await context.addCookies([{ name: 'ovlo_lang', value: 'ko', url }])
    await context.addInitScript(() => localStorage.setItem('ovlo-auth', JSON.stringify({ state: { currentUser: { id: '42', nickname: '담당자', status: 'ACTIVE', role: 'MEMBER' } }, version: 4 })))
    let n = { id: 77, officeId: 5, category: 'HOUSING', title: '기숙사 FAQ', body: '기숙사 신청 안내', sourceLang: 'KO', sourceUrl: null,
      attachmentMediaIds: [], term: '2027-1', effectiveFrom: '2026-11-01', expiresAt: '2027-03-01', status: 'DRAFT', approvedBy: null,
      approvedAt: null, version: 1, revision: 0, activeToday: false, translations: [], missingLangs: [], source: null,
      review: { sourceVersion: 0, verifiedOn: null, nextReviewOn: null, holdReason: '출처 검토 필요' } }
    const actions = [], errors = []
    await context.route('**/api/v1/**', async route => {
      const r = route.request(), path = new URL(r.url()).pathname
      const json = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
      if (path.endsWith('/auth/refresh')) return json({ accessToken: 'fixture' })
      if (path.endsWith('/office/me')) return json({ officeId: 5, officeName: '테스트 국제처', universityId: 1, status: 'ACTIVE', role: 'OFFICE_ADMIN', verifiedStudent: true })
      if (path === '/api/v1/office-admin/5') return json({ staff: [{ memberId: 42, nickname: '담당자', role: 'OFFICE_ADMIN', status: 'ACTIVE' }] })
      if (path.endsWith('/source-scopes') || path.endsWith('/history') || path.endsWith('/review-queue')) return json([])
      if (path.endsWith('/notices/77') && r.method() === 'GET') return json(n)
      if (path.endsWith('/source') || path.endsWith('/source-review') || path.endsWith('/approve')) {
        const body = r.postDataJSON(); actions.push({ path, body })
        if (body.revision !== n.revision) return json({ message: '자료가 변경되었습니다' }, 409)
        if (path.endsWith('/source')) { n.source = body.source; n.version++; n.revision++ }
        if (path.endsWith('/source-review')) {
          n.revision++
          n.review = { sourceVersion: n.version, verifiedOn: '2026-11-13', nextReviewOn: '2026-11-20', holdReason: body.outcome === 'CONFLICT' ? 'CONFLICT: 상충 확인' : null }
          if (body.outcome === 'CONFLICT') n.status = 'DRAFT'
        }
        if (path.endsWith('/approve')) { n.revision++; n.status = 'APPROVED'; n.activeToday = true }
        return json(n)
      }
      if (path.endsWith('/conversation')) return json({ turns: [{ question: '기숙사 신청?', result: { inquiryId: 900, outcome: 'ANSWERED', lang: 'KO', answerText: '과거 안내입니다. [1]', canEscalate: true,
        citations: [{ index: 1, noticeId: 77, title: '당시 FAQ', sourceUrl: null, expiresAt: '2026-11-01', sourceType: 'REVIEWED_STAFF_QA', department: '국제처', verifiedOn: '2026-10-01', noticeVersion: 1, versionRecorded: true, currentlyValid: false }] } }], consultationId: null, canContinue: true })
      return json({})
    })
    const page = await context.newPage(); page.on('pageerror', e => errors.push(e.message))
    await page.goto(`${url}/office/notices/77`)
    await page.getByRole('heading', { name: '출처·재검토 관리' }).waitFor()
    assert.equal(await page.getByRole('button', { name: '승인', exact: true }).count(), 0)
    await page.getByLabel('출처 종류', { exact: false }).selectOption('REVIEWED_STAFF_QA')
    await page.getByLabel('발행·담당 부서', { exact: true }).last().fill('국제처')
    await page.getByLabel('원문 식별자·비공개 확인 경로 (담당자 전용)').fill('내부 담당자 회신 123')
    await page.getByLabel('적용 대상', { exact: true }).fill('교환학생')
    await page.getByLabel('적용 캠퍼스', { exact: true }).fill('서울')
    await page.getByLabel('자료 확보일', { exact: true }).fill('2026-11-13')
    await page.getByLabel('갱신 담당자', { exact: false }).selectOption('42')
    await page.getByLabel('마감·금액·자격·제출 요건 포함:', { exact: false }).check()
    await page.getByLabel('공개 원문 (비공개면 본문의 공개 출처 URL을 비워 주세요)').uncheck()
    const sourceReload = page.waitForResponse(r => r.url().endsWith('/notices/77') && r.request().method() === 'GET')
    await page.getByRole('button', { name: '출처 저장 · 재승인 필요' }).click()
    await sourceReload
    await page.waitForFunction(() => document.querySelector('select') !== null)
    await page.getByRole('button', { name: '검토 결과 기록' }).waitFor({ state: 'visible' })
    await page.getByLabel('담당 부서와 공식성을 확인했습니다').check()
    await page.getByLabel('대상·학기·기간·첨부를 현재 원문과 대조했습니다').check()
    await page.getByLabel('Q&A의 개인정보를 제거하고 재사용 권한을 확인했습니다').check()
    await page.getByLabel('Q&A가 개인 예외가 아닌 일반 안내임을 확인했습니다').check()
    await page.getByLabel('확인 근거·사유 (담당자 전용)').fill('담당 부서 확인 완료')
    await page.getByRole('button', { name: '검토 결과 기록' }).click()
    await page.getByRole('button', { name: '승인', exact: true }).waitFor()
    await page.getByRole('button', { name: '승인', exact: true }).click()
    await page.getByRole('button', { name: '승인', exact: true }).waitFor({ state: 'detached' })
    assert.equal(actions.find(a => a.path.endsWith('/approve')).body.revision, 2)
    assert.equal(actions.find(a => a.path.endsWith('/source-review')).body.privacyChecked, true)
    await page.getByLabel('확인 근거·사유 (담당자 전용)').fill('충돌 시 보존할 입력')
    n.revision++
    await page.getByRole('button', { name: '검토 결과 기록' }).click()
    await page.getByRole('alert').filter({ hasText: '저장하지 못했습니다' }).waitFor()
    assert.equal(await page.getByLabel('확인 근거·사유 (담당자 전용)').inputValue(), '충돌 시 보존할 입력')
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
    fs.mkdirSync('/tmp/ovlo-notice-source-browser', { recursive: true })
    await page.screenshot({ path: '/tmp/ovlo-notice-source-browser/mobile-source.png', fullPage: true })
    await page.goto(`${url}/my-university/ask?inquiry=900`)
    await page.getByText('이 답변의 자료가 변경되었거나 더 이상 유효하지 않습니다.', { exact: false }).waitFor()
    assert.equal(await page.getByText('내부 담당자 회신 123').count(), 0)
    await page.screenshot({ path: '/tmp/ovlo-notice-source-browser/stale-citation.png', fullPage: true })
    assert.deepEqual(errors, [])
    console.log(JSON.stringify({ passed: true, checks: ['source entry', 'Q&A review', 'approval revision', '409 preserves draft', 'mobile overflow', 'stale citation', 'private reference excluded'] }))
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exitCode = 1 })
