// Called by ServiceJourneyIntegrationTest. All HTTP and STOMP use the real isolated backend.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const path=require('node:path')
const http=require('node:http')
const {spawn}=require('node:child_process')
const proxy=require('next/dist/compiled/http-proxy').createProxyServer({})
const output=path.resolve('build/reports/service-journey')
const frontend=path.resolve('frontend')
const checks=[]
const frames=[]
let next,server,browser,currentPage
const sockets=new Set()
const url='http://localhost:3358'
const delay=ms=>new Promise(r=>setTimeout(r,ms))
;(async()=>{
  assert.match(process.env.API_URL||'',/^http:\/\/127\.0\.0\.1:\d+$/,'isolated loopback backend required')
  fs.mkdirSync(output,{recursive:true})
  for(const name of ['browser.json','browser-failure.txt','failure-history.json','socket-frames.json','failure.png'])fs.rmSync(path.join(output,name),{force:true})
  const log=fs.openSync(path.join(output,'next.log'),'w')
  next=spawn(process.execPath,[require.resolve('next/dist/bin/next'),'dev','-p','3357','--hostname','127.0.0.1'],{cwd:frontend,env:process.env,stdio:['ignore',log,log],detached:true})
  proxy.on('error',(_e,_req,res)=>{if(res?.writeHead)res.writeHead(502).end();else res?.destroy()})
  server=http.createServer((req,res)=>proxy.web(req,res,{target:req.url.startsWith('/ws')?process.env.API_URL:'http://127.0.0.1:3357'}))
  server.on('upgrade',(req,socket,head)=>{
    sockets.add(socket);socket.on('close',()=>sockets.delete(socket))
    proxy.ws(req,socket,head,{target:req.url.startsWith('/ws')?process.env.API_URL:'http://127.0.0.1:3357'})
  })
  await new Promise(resolve=>server.listen(3358,'127.0.0.1',resolve))
  let ready=false
  for(let i=0;i<90;i++){try{if((await fetch(url+'/health/live',{signal:AbortSignal.timeout(3000)})).ok){ready=true;break}}catch{}await delay(1000)}
  assert.ok(ready,'Next ready')
  browser=await chromium.launch({headless:true,args:['--no-sandbox']})
  for(const mobile of [false,true]){
    const context=await browser.newContext({viewport:mobile?{width:390,height:844}:{width:1440,height:1000},serviceWorkers:'block'})
    await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
    const page=await context.newPage();currentPage=page;page.setDefaultTimeout(60000)
    page.on('websocket',ws=>{
      for(const direction of ['framesent','framereceived'])ws.on(direction,frame=>{
        const value=String(frame.payload)
        frames.push({direction,bytes:value.length,kind:['CONNECT','SUBSCRIBE','SEND','MESSAGE','ERROR'].find(k=>value.includes(k))||'other',browserMessage:value.includes('Browser chat')})
      })
    })
    const errors=[];page.on('pageerror',e=>errors.push(e.message))
    const reports=process.env.JOURNEY_REPORT_IDS.split(',')
    for(const lang of ['en','ko','ja','zh','es','fr','de','pt','ru']){
      await context.addCookies([{name:'ovlo_lang',value:lang,url}])
      await page.goto(url+'/university-reports/'+reports[0]+'?lang=en')
      const panel=page.locator('section[aria-label]').first()
      await panel.locator('summary').waitFor()
      assert.ok(!(await panel.innerText()).includes('report.evidence.'))
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,'report width '+lang)
      await panel.locator('summary').focus();await page.keyboard.press('Enter')
      assert.equal(await panel.getByRole('link').getAttribute('href'),'https://example.invalid/official')
      checks.push({check:'source panel and keyboard '+lang,mobile})
    }
    await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
    for(const [i,status] of [[1,'오래'],[2,'출처']]){
      await page.goto(url+'/university-reports/'+reports[i]+'?lang=en')
      const panel=page.locator('section[aria-label]').first();await panel.waitFor()
      assert.ok((await panel.innerText()).includes(status))
      if(i===2)assert.equal(await panel.getByRole('link').count(),0)
      checks.push({check:'evidence fallback '+i,mobile})
    }
    await page.goto(url+'/posts/'+process.env.JOURNEY_POST_ID+'?from=calendar')
    await page.waitForURL('**/login?next=*')
    await page.getByLabel('이메일',{exact:true}).fill('journey_alice@example.invalid')
    await page.getByLabel('비밀번호',{exact:true}).fill('JourneyTest1!')
    await page.getByRole('button',{name:'로그인',exact:true}).click()
    await page.waitForURL('**/posts/'+process.env.JOURNEY_POST_ID+'?from=calendar')
    await page.getByRole('heading',{name:'Journey first post',exact:true}).waitFor()
    await page.getByText('Journey reply',{exact:true}).waitFor()
    await page.reload()
    await page.getByRole('heading',{name:'Journey first post',exact:true}).waitFor()
    checks.push({check:'real login, return URL, Redis refresh, post/comment',mobile})
    await page.goto(url+'/boards/'+process.env.JOURNEY_BOARD_ID)
    await page.getByRole('link',{name:/글쓰기|글 쓰기/}).click()
    await page.locator('input[maxlength="80"]').fill('Browser activity '+mobile)
    await page.locator('textarea').fill('Created through real UI and backend')
    await page.getByRole('button',{name:'게시글 등록',exact:true}).click()
    await page.getByRole('heading',{name:'Browser activity '+mobile,exact:true}).waitFor()
    await page.reload();await page.getByRole('heading',{name:'Browser activity '+mobile,exact:true}).waitFor()
    checks.push({check:'post creation persists across reload',mobile})
    await page.goto(url+'/chat/'+process.env.JOURNEY_ROOM_ID)
    await page.getByText('First connected message',{exact:false}).waitFor()
    const input=page.locator('input[placeholder]').last()
    await input.fill('Browser chat '+mobile)
    await input.press('Enter')
    await page.getByText('Browser chat '+mobile,{exact:false}).waitFor()
    // Close only this test proxy's WebSocket transport; HTTP and production are unaffected.
    for(const socket of sockets)socket.destroy()
    await page.getByText('연결 중',{exact:false}).waitFor().catch(()=>{})
    await input.fill('After reconnect '+mobile)
    await input.press('Enter')
    await page.getByText('After reconnect '+mobile,{exact:false}).waitFor()
    await page.reload();await page.getByText('After reconnect '+mobile,{exact:false}).waitFor()
    checks.push({check:'actual STOMP, dropped transport, resubscribe, persisted history',mobile})
    await page.screenshot({path:path.join(output,(mobile?'mobile':'desktop')+'-chat.png'),fullPage:true})
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false)
    for(const route of ['/calendar','/marketplace','/my-university','/profile/'+process.env.JOURNEY_MEMBER_ID]){
      const failures=[]
      const listen=r=>{if(r.status()>=500)failures.push(r.status())}
      page.on('response',listen);await page.goto(url+route);await page.waitForLoadState('networkidle')
      assert.ok(!page.url().includes('/login'),'authenticated route '+route)
      assert.deepEqual(failures,[],route);page.off('response',listen)
      checks.push({check:'authenticated route '+route,mobile})
    }
    assert.deepEqual(errors,[])
    await context.close()
  }
  fs.writeFileSync(path.join(output,'browser.json'),JSON.stringify({passed:true,mockedApi:false,checks},null,2))
  console.log('Real browser journeys passed:',checks.length)
})().catch(async e=>{
  console.error(e)
  fs.writeFileSync(path.join(output,'browser-failure.txt'),String(e.stack||e))
  fs.writeFileSync(path.join(output,'socket-frames.json'),JSON.stringify(frames,null,2))
  try {
    const auth=await fetch(process.env.API_URL+'/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'journey_alice@example.invalid',password:'JourneyTest1!'})}).then(r=>r.json())
    const history=await fetch(process.env.API_URL+'/api/v1/chat/rooms/'+process.env.JOURNEY_ROOM_ID+'/messages',{headers:{Authorization:'Bearer '+auth.accessToken}}).then(r=>r.json())
    fs.writeFileSync(path.join(output,'failure-history.json'),JSON.stringify(history,null,2))
  } catch {}
  if(currentPage&&!currentPage.isClosed())await currentPage.screenshot({path:path.join(output,'failure.png'),fullPage:true}).catch(()=>{})
  process.exitCode=1
}).finally(async()=>{
  await browser?.close();for(const s of sockets)s.destroy();server?.close();proxy.close()
  if(next){try{process.kill(-next.pid,'SIGTERM')}catch{}}
  // http-proxy may retain an upstream upgraded socket after its peer closes.
  // The standalone harness has persisted all evidence and closed its browser/child process.
  process.exit(process.exitCode||0)
})
