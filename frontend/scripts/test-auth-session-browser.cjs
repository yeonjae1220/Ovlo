// Real browser Web Locks + storage events; synthetic HTTP responses and cookies only.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const url = process.env.OFFICE_CHAT_URL || 'http://localhost:3351'
const output = process.env.OFFICE_CHAT_SCREENSHOTS || '/tmp/ovlo-auth-race-browser'
;(async()=>{
  fs.mkdirSync(output,{recursive:true})
  const browser=await chromium.launch({headless:true,args:['--no-sandbox']})
  try {
    for(const status of [200,401]) {
      const context=await browser.newContext({serviceWorkers:'block',viewport:{width:1440,height:1000}})
      await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
      let held, loginRequests=0, refreshRequests=0
      const a={id:'42',nickname:'A',status:'ACTIVE',role:'MEMBER'}, b={...a,id:'43',nickname:'B'}
      await context.addInitScript(a=>{
        if(!localStorage.getItem('race-seeded')) {
          localStorage.setItem('ovlo-auth',JSON.stringify({state:{currentUser:a},version:4}))
          localStorage.setItem('race-seeded','1')
        }
      },a)
      await context.route('**/api/v1/**',async route=>{
        const p=new URL(route.request().url()).pathname
        const json=(data,headers={})=>route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data),headers})
        if(p.endsWith('/auth/refresh')) {refreshRequests++;held=route;return}
        if(p.endsWith('/auth/login')) {loginRequests++;return json({accessToken:'fixture-B',memberId:43},{'set-cookie':'refresh_token=B; Path=/; HttpOnly; SameSite=Lax'})}
        if(p.endsWith('/members/43'))return json(b)
        if(p.endsWith('/office/me'))return json({officeId:5,officeName:'Synthetic office',universityId:1,status:'ACTIVE',verifiedStudent:true,role:null})
        if(p.endsWith('/consultations')) {
          assert.equal(route.request().headers().authorization,'Bearer fixture-B')
          return json([{id:700,question:'B_ONLY_PRIVATE_CONSULTATION',status:'OPEN',openedAt:'2026-11-15T00:00:00Z'}])
        }
        if(p.endsWith('/notices')||p.endsWith('/partners')||p.includes('/following-status'))return json([])
        return json({content:[],totalElements:0,totalPages:0})
      })
      const pageA=await context.newPage(),pageB=await context.newPage()
      await pageA.goto(url+'/my-university/consultations')
      await pageA.waitForTimeout(100)
      assert.ok(held,'A refresh must be in flight')
      await pageB.goto(url+'/login')
      await pageB.getByRole('textbox',{name:'이메일',exact:true}).fill('b@example.invalid')
      await pageB.getByLabel('비밀번호',{exact:true}).fill('synthetic-password')
      await pageB.getByRole('button',{name:'로그인',exact:true}).click()
      await pageB.waitForTimeout(300)
      assert.equal(loginRequests,0,'B cookie mutation must wait for A refresh across tabs')
      await held.fulfill({status,contentType:'application/json',body:JSON.stringify(status===200?{accessToken:'late-A',memberId:42}:{}),
        headers:status===200?{'set-cookie':'refresh_token=A; Path=/; HttpOnly; SameSite=Lax'}:{}})
      await pageB.waitForURL('**/boards')
      await pageB.locator('a[href="/my-university"]:visible').first().click()
      await pageB.locator('a[href="/my-university/consultations"]').click()
      await pageB.getByText('B_ONLY_PRIVATE_CONSULTATION',{exact:true}).waitFor()
      assert.equal(loginRequests,1);assert.equal(refreshRequests,1)
      assert.equal((await context.cookies()).find(x=>x.name==='refresh_token').value,'B')
      assert.equal(await pageB.evaluate(()=>JSON.parse(localStorage.getItem('ovlo-auth')).state.currentUser.id),'43')
      await pageB.screenshot({path:output+'/late-'+status+'.png',fullPage:true})
      await context.close()
    }
    console.log(JSON.stringify({passed:true,cases:['cross-tab stale refresh success','cross-tab stale refresh 401'],cookieOrder:true,output}))
  } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})