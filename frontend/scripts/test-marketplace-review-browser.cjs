// Regression checks for editing conflicts and durable block management. API fixtures; real browser UI.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert=require('node:assert/strict')
const url=process.env.MARKET_URL || 'http://localhost:3322'
const image=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=','base64')
const item={id:42,sellerId:1,sellerName:'Campus seller',universityName:'Paris University',status:'AVAILABLE',hidden:false,version:0,createdAt:'2026-11-11T00:00:00Z',details:{
 title:'귀국 정리 · 주방용품 묶음',description:'냄비와 접시를 함께 드려요. 사진 속 전자레인지는 제외입니다.',sourceLanguage:'ko',category:'HOUSEHOLD',condition:'GOOD',
 free:true,bundle:true,partialPickup:false,price:{amount:0,currency:'KRW'},location:{countryCode:'FR',city:'Paris',universityId:1,meetingPoint:'Campus gate'},pickupFrom:'2026-12-01',pickupUntil:'2026-12-05',tags:['RETURN_HOME'],photoIds:['11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222']}}

;(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH || undefined,args:['--no-sandbox']})
 const checks=[]
 try {
  for(const scenario of ['block-reload','edit-refresh','edit-save-conflict','edit-load-failure']) {
   const context=await browser.newContext({locale:'ko-KR',viewport:{width:390,height:844}})
   await context.addCookies([{name:'ovlo_lang',value:'ko',url}])
   await context.addInitScript(id=>localStorage.setItem('ovlo-auth',JSON.stringify({state:{currentUser:{id,nickname:'Tester',status:'ACTIVE',role:'MEMBER'}},version:4})),scenario==='block-reload'?'2':'1')
   let blocked=false,version=0,failGet=false,hidden=false,saved=null
   const writes=[]
   const current=()=>({...item,version,hidden,details:saved || {...item.details,title:version?'REMOTE EDIT':item.details.title}})
   await context.route('**/api/v1/**',async route=>{
    const request=route.request(),u=new URL(request.url()),path=u.pathname
    const json=(data,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(data)})
    if(path.endsWith('/auth/refresh'))return json({accessToken:'fixture-token'})
    if(path.endsWith('/marketplace/options'))return json({countries:['FR'],currencies:[{code:'KRW',digits:0}]})
    if(path.endsWith('/marketplace/blocks'))return json({content:blocked?[{memberId:1,nickname:'Campus seller'}]:[],nextBefore:null})
    if(path.includes('/marketplace/blocks/')){blocked=request.method()==='PUT';return route.fulfill({status:204})}
    if(path.includes('/marketplace/photos/'))return route.fulfill({contentType:'image/png',body:image})
    if(path.endsWith('/marketplace/42')){
     if(request.method()==='PUT') {
      writes.push({version:Number(u.searchParams.get('version')),details:request.postDataJSON()})
      if(Number(u.searchParams.get('version'))!==version)return json({message:'Listing changed. Reload before saving.'},409)
      saved=request.postDataJSON();version++;return json(current())
     }
     return blocked||failGet?json({message:'Listing not found'},404):json(current())
    }
    if(path.endsWith('/marketplace'))return json({content:blocked?[]:[current()],hasNext:false})
    return json({})
   })
   const page=await context.newPage(),errors=[]
   page.on('pageerror',e=>errors.push(e.message))
   let accept=true;page.on('dialog',d=>accept?d.accept():d.dismiss())
   const refresh=async()=>{
    await Promise.all([
     page.waitForResponse(r=>r.url().endsWith('/marketplace/42')),
     page.evaluate(()=>{const real=Date.now.bind(Date);Date.now=()=>real()+120000;window.dispatchEvent(new Event('visibilitychange'))})
    ])
   }
   if(scenario==='block-reload') {
    await page.goto(url+'/marketplace/42')
    await page.getByRole('button',{name:'판매자 차단',exact:true}).click()
    await page.getByRole('button',{name:'차단 취소',exact:true}).waitFor()
    await page.reload();await page.getByRole('alert').filter({hasText:'Listing not found'}).waitFor()
    await page.getByRole('link',{name:'차단한 회원',exact:true}).click()
    await page.getByText('Campus seller',{exact:true}).waitFor()
    await page.reload();await page.getByText('Campus seller',{exact:true}).waitFor()
    await page.getByRole('button',{name:'차단 취소',exact:true}).click()
    await page.getByText('차단한 회원이 없습니다.',{exact:true}).waitFor()
    await page.goto(url+'/marketplace/42');await page.getByRole('heading',{name:item.details.title}).waitFor()
   } else {
    await page.goto(url+'/marketplace/42/edit')
    const input=page.getByLabel('제목 *',{exact:true})
    await input.fill('UNSAVED USER DRAFT')
    if(scenario==='edit-load-failure') {
     failGet=true;await refresh()
     await page.getByRole('alert').filter({hasText:'Listing not found'}).waitFor()
     assert.equal(await input.inputValue(),'UNSAVED USER DRAFT')
     assert.equal(await page.getByRole('button',{name:'수정 저장',exact:true}).isDisabled(),true)
     failGet=false;hidden=true;version=1
     await page.getByRole('button',{name:'다시 시도',exact:true}).click()
     await page.getByText('운영자가 숨긴 게시글입니다.',{exact:true}).waitFor()
     assert.equal(await input.inputValue(),'UNSAVED USER DRAFT')
     assert.equal(writes.length,0)
    } else {
     version=1
     if(scenario==='edit-save-conflict') await page.getByRole('button',{name:'수정 저장',exact:true}).click()
     else await refresh()
     await page.getByText('다른 곳에서 게시글이 변경되었습니다. 작성 중인 내용은 유지했습니다.',{exact:true}).waitFor()
     assert.equal(await input.inputValue(),'UNSAVED USER DRAFT')
     assert.equal(await page.getByRole('button',{name:'수정 저장',exact:true}).isDisabled(),true)
     accept=false;await page.getByRole('button',{name:'최신 내용 불러오기',exact:true}).click()
     assert.equal(await input.inputValue(),'UNSAVED USER DRAFT')
     accept=true;await page.getByRole('button',{name:'최신 내용 불러오기',exact:true}).click()
     assert.equal(await input.inputValue(),'REMOTE EDIT')
     await input.fill('FINAL EDIT')
     await page.getByRole('button',{name:'수정 저장',exact:true}).click()
     await page.waitForURL('**/marketplace/42')
     assert.equal(writes.at(-1).version,1);assert.equal(writes.at(-1).details.title,'FINAL EDIT')
     if(scenario==='edit-save-conflict')assert.equal(writes[0].version,0)
    }
   }
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,'Mobile overflow')
   assert.deepEqual(errors,[]);checks.push(scenario);await context.close()
  }
  console.log(JSON.stringify({passed:true,checks}))
 } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})
