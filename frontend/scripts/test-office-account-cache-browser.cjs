// Same-document account switch against actual Next.js pages. Every API response is synthetic.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const url = process.env.OFFICE_CHAT_URL || 'http://localhost:3351'
const output = process.env.OFFICE_CHAT_SCREENSHOTS || '/tmp/ovlo-account-cache-browser'
;(async () => {
  fs.mkdirSync(output, {recursive:true})
  const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH || undefined,args:['--no-sandbox']})
  try {
    const context=await browser.newContext({viewport:{width:1440,height:1000},serviceWorkers:'block'})
    const a={id:'42',nickname:'검증 A',name:'검증 A',status:'ACTIVE',role:'MEMBER'}
    const b={...a,id:'43',nickname:'검증 B',name:'검증 B'}
    let current=a, withdrawn=false, listCallsB=0
    await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
    await context.addInitScript(a=>{
      if(!sessionStorage.getItem('cache-test-initialized')){
        localStorage.setItem('ovlo-auth',JSON.stringify({state:{currentUser:a},version:4}))
        sessionStorage.setItem('cache-test-initialized','1')
      }
      window.__cacheTestDocument=Math.random()
    },a)
    await context.route('**/api/v1/**',async route=>{
      const req=route.request(),p=new URL(req.url()).pathname
      const json=(body,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)})
      if(p.endsWith('/auth/login')) {current=b;return json({accessToken:'fixture-B',memberId:43})}
      if(p.endsWith('/auth/refresh')) return withdrawn?json({},401):json({accessToken:'fixture-A'})
      if(p==='/api/v1/members/42'&&req.method()==='DELETE') {withdrawn=true;return route.fulfill({status:204})}
      if(p==='/api/v1/members/42') return json(a)
      if(p==='/api/v1/members/43') return json(b)
      if(p.endsWith('/office/me')) return json({officeId:5,officeName:'검증 국제처',status:'ACTIVE',role:null,verifiedStudent:true,universityId:1})
      if(p==='/api/v1/office/5/consultations') {
        if(current===b)listCallsB++
        return json([{id:700,question:current===a?'A_ONLY_PRIVATE_CONSULTATION':'B_ONLY_PRIVATE_CONSULTATION',
          status:'OPEN',openedAt:'2026-11-15T00:00:00Z',assignedStaffNickname:null}])
      }
      if(p.endsWith('/notices')||p.endsWith('/partners')) return json([])
      if(p.includes('/verification'))return json({trustLevel:'UNVERIFIED',verifiedUniversities:[]})
      if(p.includes('/following-status'))return json([])
      return json({content:[],totalElements:0,totalPages:0})
    })
    const page=await context.newPage(),errors=[]
    page.on('pageerror',e=>errors.push(e.message))
    page.on('dialog',dialog=>dialog.accept())
    await page.goto(url+'/my-university/consultations')
    await page.getByText('A_ONLY_PRIVATE_CONSULTATION',{exact:true}).waitFor()
    const doc=await page.evaluate(()=>window.__cacheTestDocument)
    await page.locator('a[href="/profile/42"]:visible').first().click()
    await page.getByRole('button',{name:'회원 탈퇴',exact:true}).click()
    await page.waitForURL('**/login')
    await page.getByRole('textbox',{name:'이메일',exact:true}).fill('cache-b@example.invalid')
    await page.getByLabel('비밀번호',{exact:true}).fill('synthetic-only-password')
    await page.getByRole('button',{name:'로그인',exact:true}).click()
    await page.waitForURL('**/boards')
    await page.locator('a[href="/my-university"]:visible').first().click()
    await page.locator('a[href="/my-university/consultations"]').click()
    await page.getByText('B_ONLY_PRIVATE_CONSULTATION',{exact:true}).waitFor()
    assert.equal(await page.getByText('A_ONLY_PRIVATE_CONSULTATION',{exact:true}).count(),0)
    assert.equal(await page.evaluate(()=>window.__cacheTestDocument),doc,'No document reload may mask a cache leak')
    assert.ok(listCallsB>0)
    assert.deepEqual(errors,[])
    await page.screenshot({path:output+'/account-b-consultations.png',fullPage:true})
    console.log(JSON.stringify({passed:true,sameDocument:true,listCallsB,screenshot:output+'/account-b-consultations.png'}))
  } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})
