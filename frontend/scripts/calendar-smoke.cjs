// UI smoke test with isolated API fixtures. Run against `next start` with playwright-core available.
const { chromium } = require('playwright-core')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const base = process.env.CALENDAR_QA_URL || 'http://127.0.0.1:3108'
const out = process.env.CALENDAR_QA_OUTPUT || '/tmp/ovlo-calendar-ui'
fs.mkdirSync(out, { recursive: true })
const school = { id: 1, name: 'Calendar University', countryCode: 'KR', city: 'Seoul' }
const member = { id: 42, nickname: 'calendar.staff', name: 'Calendar Staff', email: 'staff@school.edu', status: 'ACTIVE', role: 'UNIV', homeUniversityId: 1, major: null, universityExperiences: [], languageSkills: [], contactInfos: [] }
const token = 'header.' + Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url') + '.signature'
const period = { allDay: true, startDate: '2026-11-10', endDate: '2026-11-16', startsAt: null, endsAt: null, timeZone: 'Asia/Seoul' }
let schoolEvents = [{ id: 1, kind: 'SCHOOL', universityId: 1, universityName: school.name, details: { title: '비자 연장 서류 접수', description: '국제처에 신청 서류를 제출해 주세요.', category: 'ADMINISTRATION', period, location: '국제처', actionUrl: 'https://example.edu/apply', deadlineAt: null, audience: '교환학생' }, origin: 'OVLO_CURATED', status: 'PUBLISHED', source: { url: 'https://example.edu/notice', checkedOn: '2026-11-01' }, version: 0 }]
let personal = []
let subscriptions = [school]
let registration
let applications = []
function fromInput(input, kind, id) {
  return { id, kind, universityId: kind === 'SCHOOL' ? 1 : null, universityName: kind === 'SCHOOL' ? school.name : null,
    details: { title: input.title, description: input.description, category: input.category, period: { allDay: input.allDay, startDate: input.startDate, endDate: input.endDate, startsAt: input.startsAt, endsAt: input.endsAt, timeZone: input.timeZone }, location: input.location, actionUrl: input.actionUrl, deadlineAt: input.deadlineAt, audience: input.audience }, origin: kind === 'SCHOOL' ? 'UNIVERSITY' : null, status: kind === 'SCHOOL' ? 'DRAFT' : null, source: null, version: 0 }
}
async function routes(context) {
  await context.route('**/api/v1/**', async route => {
    const req = route.request(); const url = new URL(req.url()); const p = url.pathname; const method = req.method()
    const json = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
    if (p.endsWith('/auth/refresh')) return json({ accessToken: token })
    if (p.endsWith('/global-universities')) return json({ content: [{ id: 1, nameEn: school.name, countryCode: 'KR', city: 'Seoul' }] })
    if (p.endsWith('/members/university-registration')) { registration = req.postDataJSON(); return json({ ...member, role: 'MEMBER' }, 201) }
    if (p.endsWith('/calendar/options')) return json({ role: 'UNIV', subscriptions, suggestions: [school], managedSchools: [school] })
    if (p.endsWith('/calendar/applications')) {
      if (method === 'POST') { const a = req.postDataJSON(); applications.push({ ...a, id: 1, memberId: 42, applicantName: member.name, universityName: school.name, status: 'PENDING', createdAt: new Date().toISOString(), version: 0 }); return json(applications[0], 201) }
      return json(applications)
    }
    if (p.includes('/calendar/subscriptions/')) { subscriptions = method === 'DELETE' ? [] : [school]; return route.fulfill({ status: 204 }) }
    if (p.endsWith('/calendar')) return json([...schoolEvents.filter(e => subscriptions.length && e.status !== 'DRAFT'), ...personal])
    if (p.endsWith('/calendar/schools/1/events')) {
      if (method === 'GET') return json(schoolEvents)
      const e = fromInput(req.postDataJSON(), 'SCHOOL', schoolEvents.length + 1); schoolEvents.push(e); return json(e, 201)
    }
    if (p.includes('/calendar/school-events/') && p.endsWith('/status')) {
      const e = schoolEvents.find(e => e.id === Number(p.split('/').at(-2))); e.status = req.postDataJSON().action === 'publish' ? 'PUBLISHED' : 'CANCELLED'; e.version++; return json(e)
    }
    if (p.endsWith('/calendar/personal') && method === 'POST') { const e = fromInput(req.postDataJSON(), 'PERSONAL', personal.length + 1); personal.push(e); return json(e, 201) }
    if (p.includes('/calendar/personal/')) {
      const id = Number(p.split('/').at(-1))
      if (method === 'DELETE') { personal = personal.filter(e => e.id !== id); return route.fulfill({ status: 204 }) }
      const e = fromInput(req.postDataJSON(), 'PERSONAL', id); e.version++; personal = personal.map(v => v.id === id ? e : v); return json(e)
    }
    return json({ message: 'Unhandled QA fixture: ' + p }, 501)
  })
}
;(async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1365, height: 1000 }, locale: 'ko-KR', timezoneId: 'Asia/Seoul' })
    await context.addCookies([{ name: 'ovlo_lang', value: 'ko', url: base }])
    await routes(context)
    const page = await context.newPage(); const errors = []; page.on('pageerror', e => errors.push(e.message))
    await page.goto(base + '/register')
    await page.getByRole('link', { name: /학교 담당자 가입/ }).click()
    await page.getByLabel('담당자 이름', { exact: true }).fill('Calendar Staff')
    await page.getByLabel('닉네임', { exact: false }).fill('calendar.staff')
    await page.getByLabel('학교 업무 이메일').fill('staff@school.edu')
    await page.getByLabel('비밀번호', { exact: false }).fill('Strong!123')
    await page.getByLabel('학교 검색').fill('Calendar')
    await page.getByRole('button', { name: /Calendar University/ }).click()
    await page.getByLabel('담당 부서').fill('International Office')
    await page.getByLabel('담당 업무와 확인 가능한 공식 연락처·안내').fill('Official directory: https://school.edu/staff')
    await page.getByRole('button', { name: '계정 만들고 담당자 신청' }).click()
    await page.getByRole('heading', { name: '신청을 접수했습니다.' }).waitFor()
    assert.equal(registration.universityId, 1); assert.equal(registration.role, undefined)
    await context.addInitScript(member => { localStorage.setItem('ovlo-auth', JSON.stringify({ version: 4, state: { currentUser: member } })) }, member)
    await page.goto(base + '/calendar')
    await page.locator('input[type=month]').fill('2026-11')
    await page.getByRole('button', { name: /2026-11-10.*비자 연장 서류 접수/ }).waitFor()
    await page.getByRole('button', { name: '+ 개인 일정' }).click()
    await page.getByLabel('제목', { exact: true }).fill('나의 대사관 방문')
    await page.getByLabel('시작일', { exact: true }).fill('2026-11-12')
    await page.getByLabel('마지막 날 (포함)').fill('2026-11-12')
    await page.getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('button', { name: /2026-11-12.*나의 대사관 방문/ }).waitFor()
    assert.equal(personal[0].details.period.endDate, '2026-11-13')
    await page.getByRole('button', { name: /2026-11-10.*비자 연장 서류 접수/ }).click()
    await page.getByRole('link', { name: '학교 공식 출처 ↗' }).waitFor()
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(out, 'calendar-desktop.png'), fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: path.join(out, 'calendar-mobile.png'), fullPage: true })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'mobile page overflows')
    await page.setViewportSize({ width: 1365, height: 1000 })
    await page.goto(base + '/calendar/manage')
    await page.locator('input[type=month]').fill('2026-11')
    await page.getByRole('button', { name: '+ 학교 일정' }).click()
    await page.getByLabel('제목', { exact: true }).fill('새 학기 오리엔테이션')
    await page.getByLabel('시작일', { exact: true }).fill('2026-11-20')
    await page.getByLabel('마지막 날 (포함)').fill('2026-11-20')
    await page.getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('button', { name: /2026-11-20.*새 학기 오리엔테이션/ }).click()
    assert.equal(schoolEvents[1].status, 'DRAFT')
    await page.getByRole('button', { name: '게시', exact: true }).click()
    await page.getByRole('button', { name: '행사 취소', exact: true }).waitFor()
    assert.equal(schoolEvents[1].status, 'PUBLISHED')
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(out, 'calendar-management.png'), fullPage: true })
    await page.goto(base + '/register'); await page.screenshot({ path: path.join(out, 'signup-choice.png'), fullPage: true })
    assert.deepEqual(errors, [])
    console.log('UI smoke passed: signup, personal event, provenance, mobile layout, school draft/publish; screenshots: ' + out)
    await context.close()
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exitCode = 1 })
