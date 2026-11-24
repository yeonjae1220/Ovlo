// Actual browser SockJS/STOMP with a synthetic WebSocket peer; no live messages.
const { chromium }=require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert=require('node:assert/strict')
const url=process.env.AUDIT_URL || 'http://localhost:3357'
;(async()=>{
  const browser=await chromium.launch({headless:true,args:['--no-sandbox']})
  try {
    const context=await browser.newContext({serviceWorkers:'block'})
    await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
    await context.addInitScript(()=>localStorage.setItem('ovlo-auth',JSON.stringify({state:{currentUser:{id:'42',nickname:'Tester',status:'ACTIVE',role:'MEMBER'}},version:4})))
    const first={messageId:1,senderId:43,content:'BEFORE_DISCONNECT',sentAt:'2026-11-24T00:00:00Z'}
    const missed={messageId:2,senderId:43,content:'DURING_DISCONNECT',sentAt:'2026-11-24T00:01:00Z'}
    let history=[first], socket, connects=0, subscriptions=[], paused=false
    await context.route('**/api/v1/**',route=>{
      const path=new URL(route.request().url()).pathname
      const data=path.endsWith('/auth/refresh')?{accessToken:'fixture-token',memberId:42}:
        path.endsWith('/messages')?history:
        path.endsWith('/chat/rooms/7')?{chatRoomId:7,type:'DM',name:'Synthetic reconnect',participantIds:[42,43],participantNicknames:{42:'Tester',43:'Peer'},participantLastReadAt:{},unreadCount:0}:
        path.endsWith('/members/42')?{id:'42',nickname:'Tester',status:'ACTIVE',role:'MEMBER'}:[]
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)})
    })
    await context.route('**/ws/info*',route=>route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({websocket:true,cookie_needed:false,origins:['*:*'],entropy:1})}))
    await context.routeWebSocket('**/ws/**',ws=>{
      if(paused) {ws.close();return}
      socket=ws
      const send=frame=>ws.send('a'+JSON.stringify([frame]))
      ws.onMessage(raw=>{
        for(const frame of JSON.parse(String(raw))) {
          if(frame.startsWith('CONNECT')) {connects++;send('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0')}
          if(frame.startsWith('SUBSCRIBE')) {
            const destination=frame.match(/destination:([^\n]+)/)[1]
            const id=frame.match(/id:([^\n]+)/)[1]
            subscriptions.push({connects,destination})
            if(connects>1 && destination==='/topic/chat/7') send('MESSAGE\nsubscription:'+id+'\nmessage-id:2\ndestination:/topic/chat/7\ncontent-type:application/json\n\n'+JSON.stringify(missed)+'\0')
          }
        }
      })
      ws.send('o')
    })
    const page=await context.newPage()
    page.setDefaultTimeout(60000)
    await page.goto(url+'/chat/7')
    const input=page.locator('input').last()
    await input.waitFor()
    await input.fill('KEEP_MY_DRAFT')
    await page.getByText('BEFORE_DISCONNECT',{exact:false}).waitFor()
    paused=true
    await context.setOffline(true)
    socket.close({code:1001,reason:'synthetic network loss'})
    await page.waitForFunction(()=>document.querySelector('input')?.disabled===true)
    assert.equal(await input.inputValue(),'KEEP_MY_DRAFT')
    history=[first,missed]
    paused=false
    await context.setOffline(false)
    await page.getByText('DURING_DISCONNECT',{exact:false}).waitFor()
    assert.equal(await input.inputValue(),'KEEP_MY_DRAFT')
    assert.equal(await page.getByText('DURING_DISCONNECT',{exact:false}).count(),1)
    assert.ok(connects>=2)
    assert.equal(subscriptions.filter(s=>s.connects===connects).length,2)
    console.log(JSON.stringify({passed:true,connects,subscriptions:subscriptions.length,historyRecovered:true,draftPreserved:true,duplicates:0}))
  } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})
