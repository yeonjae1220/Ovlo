// Real Chromium + Next.js UI; all API responses are synthetic and no live account is modified.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const url = process.env.AUDIT_URL || 'http://localhost:3357'
const output = process.env.AUDIT_SCREENSHOTS || '/tmp/ovlo-project-audit-browser'
let currentPage
const user = { id:'42',nickname:'Tester',name:'Synthetic User',status:'ACTIVE',role:'MEMBER',email:'test@example.invalid' }
const board = id => ({id,name:'Board '+id,category:'GENERAL',scope:'GLOBAL',active:true,creatorId:42})
const post = id => ({id,boardId:21,authorId:42,title:'Post '+id,content:'Synthetic content',commentCount:3,comments:[],likeCount:0,dislikeCount:0,deleted:false})
;(async () => {
  fs.mkdirSync(output,{recursive:true})
  const browser = await chromium.launch({headless:true,args:['--no-sandbox']})
  const checks = []
  try {
    for (const mobile of [false,true]) {
      const context = await browser.newContext({serviceWorkers:'block',viewport:mobile?{width:390,height:844}:{width:1440,height:1000}})
      await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
      let authenticated = false, created, createdPost
      const errors=[]
      await context.route('**/api/v1/**',async route => {
        const req=route.request(), u=new URL(req.url()), p=u.pathname
        const json=(data,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(data)})
        if(p.endsWith('/auth/refresh')) return authenticated?json({accessToken:'fixture-token',memberId:42}):json({},401)
        if(p.endsWith('/auth/login')) { authenticated=true; return json({accessToken:'fixture-token',memberId:42}) }
        if(p.endsWith('/members/42')) return json(user)
        if(p.endsWith('/office/me')) return json({},404)
        if(p.endsWith('/boards') && req.method()==='POST') {created=req.postDataJSON();return json({...board(22),...created},201)}
        if(p.endsWith('/boards')) {
          if(u.searchParams.get('category'))return json({content:[board(1)],totalElements:1,page:0,size:20})
          const page=Number(u.searchParams.get('page')||0)
          return json({content:page?[board(21)]:Array.from({length:20},(_,i)=>board(i+1)),totalElements:21,page,size:20})
        }
        if(/\/boards\/\d+\/posts$/.test(p)) {
          const page=Number(u.searchParams.get('page')||0)
          return json({content:page?[post(999)]:Array.from({length:20},(_,i)=>post(i+1)),totalElements:21,totalPages:2,hasNext:!page,page,size:20})
        }
        if(/\/boards\/\d+$/.test(p))return json({...board(Number(p.split('/').pop())),...(created||{})})
        if(p.endsWith('/posts') && req.method()==='POST') { createdPost={...post(1001),...req.postDataJSON()};return json(createdPost,201) }
        if(/\/posts\/\d+$/.test(p))return json(Number(p.split('/').pop())===1001 ? createdPost : post(Number(p.split('/').pop())))
        if(p.endsWith('/global-universities'))return json({content:[{id:10,nameEn:'Synthetic University',countryCode:'KR'}]})
        if(p.endsWith('/members/check-nickname'))return json({available:true})
        return json({content:[],totalElements:0,totalPages:0,hasNext:false})
      })
      const page=await context.newPage()
      currentPage=page
      page.setDefaultTimeout(60000)
      page.on('pageerror',e=>errors.push(e.message))
      await page.goto(url+'/posts/501?from=calendar')
      await page.waitForURL('**/login?next=*')
      await page.getByLabel('이메일',{exact:true}).fill('test@example.invalid')
      await page.getByLabel('비밀번호',{exact:true}).fill('Synthetic1!')
      await page.getByRole('button',{name:'로그인',exact:true}).click()
      await page.waitForURL('**/posts/501?from=calendar')
      await page.getByRole('heading',{name:'Post 501',exact:true}).waitFor()
      checks.push('login return '+(mobile?'mobile':'desktop'))
      await page.goto(url+'/boards')
      console.log('board directory',mobile,await page.title())
      await page.getByRole('tab',{name:/게시판 찾기/}).click()
      await page.getByRole('link',{name:/Board 20/}).waitFor()
      await page.getByRole('button',{name:'다음 →',exact:true}).click()
      await page.getByRole('link',{name:/Board 21/}).click()
      await page.getByRole('heading',{name:'Board 21'}).waitFor()
      await page.getByRole('button',{name:'다음 →',exact:true}).click()
      await page.getByText('Post 999',{exact:true}).waitFor()
      assert.ok(await page.getByText('↳ 3',{exact:true}).count())
      await page.screenshot({path:output+'/'+(mobile?'mobile':'desktop')+'-pagination.png',fullPage:true})
      checks.push('21+ board/post pagination and counts '+(mobile?'mobile':'desktop'))
      await page.goto(url+'/boards')
      await page.getByRole('button',{name:/게시판 만들기|게시판 생성|새 게시판/}).first().click()
      await page.getByLabel('게시판 이름',{exact:true}).fill('New university board')
      await page.getByLabel('커뮤니티 범위').selectOption('UNIVERSITY')
      // Label wording is shared with onboarding; choose the searchable university input.
      const universityInput=page.locator('input[autocomplete="off"]')
      await universityInput.fill('Synthetic')
      await page.getByRole('button',{name:/Synthetic University/}).click()
      await page.getByRole('button',{name:'생성',exact:true}).click()
      await page.waitForURL('**/boards/22')
      assert.equal(created.universityId,10)
      await page.getByRole('link',{name:/글쓰기|글 쓰기/}).click()
      await page.waitForURL('**/posts/new?boardId=22')
      await page.locator('input[maxlength="80"]').fill('My first question')
      await page.locator('textarea').fill('Synthetic first post')
      await page.getByRole('button',{name:'게시글 등록',exact:true}).click()
      await page.waitForURL('**/posts/1001')
      await page.getByRole('heading',{name:'My first question',exact:true}).waitFor()
      assert.equal(createdPost.boardId,22)
      checks.push('school board creation to first post '+(mobile?'mobile':'desktop'))
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false,'no horizontal overflow')
      await page.goto(url+'/register/member')
      await page.getByLabel('닉네임',{exact:false}).fill('tester')
      await page.getByLabel('이메일',{exact:true}).fill('test@example.invalid')
      await page.getByLabel('비밀번호',{exact:true}).fill('weak')
      await page.getByRole('button',{name:'다음 →',exact:true}).click()
      await page.getByRole('alert').waitFor()
      assert.equal(await page.evaluate(()=>document.activeElement.id),'register-password')
      await page.getByLabel('비밀번호',{exact:true}).fill('ValidPassword1!')
      assert.equal(await page.getByLabel('비밀번호',{exact:true}).getAttribute('autocomplete'),'new-password')
      await page.screenshot({path:output+'/'+(mobile?'mobile':'desktop')+'-registration.png',fullPage:true})
      checks.push('registration labels '+(mobile?'mobile':'desktop'))
      assert.deepEqual(errors,[])
      await context.close()
    }
    console.log(JSON.stringify({passed:true,checks,output}))
  } finally { await browser.close() }
})().catch(async e=>{console.error(e); if(currentPage && !currentPage.isClosed()) {console.error(await currentPage.locator('body').innerText()); await currentPage.screenshot({path:output+'/failure.png',fullPage:true})} process.exitCode=1})
