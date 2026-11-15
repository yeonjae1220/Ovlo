const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict')
const ts = require('typescript'), axios = require('axios')
const root = path.resolve(__dirname, '..'), modules = new Map(), storage = new Map()
global.window = { localStorage: { getItem: k => storage.get(k) ?? null, setItem: (k,v) => storage.set(k,v), removeItem: k => storage.delete(k) }, location: {} }
function load(file) {
  file = path.resolve(file)
  if (modules.has(file)) return modules.get(file).exports
  const m = { exports: {} }; modules.set(file,m)
  const code = ts.transpileModule(fs.readFileSync(file,'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } }).outputText
  new Function('require','module','exports',code)(name => {
    if (name.startsWith('.') || name.startsWith('@/')) return load((name.startsWith('@/') ? path.join(root,'src',name.slice(2)) : path.resolve(path.dirname(file),name)) + '.ts')
    return require(name)
  },m,m.exports)
  return m.exports
}
const { useAuthStore } = load(path.join(root,'src/store/authStore.ts'))
const session = load(path.join(root,'src/utils/authSession.ts'))
const { refreshAuth } = load(path.join(root,'src/utils/refreshAuth.ts'))
const { authApi } = load(path.join(root,'src/api/auth.ts'))
const api = load(path.join(root,'src/utils/axios.ts')).default
const state = () => useAuthStore.getState(), tick = () => new Promise(r => setImmediate(r))
let requests = []
axios.defaults.adapter = config => new Promise((resolve,reject) => requests.push({ config, ok: data => resolve({ data, config, status:200, headers:{} }), fail: status => reject(new axios.AxiosError('synthetic','ERR_BAD_RESPONSE',config,null,{status,config})) }))
api.defaults.adapter = axios.defaults.adapter
const observed = p => p.then(value => ({value}),error => ({error}))
function reset() { requests=[]; state().clearAuth(); state().setAuth('A',{id:'42'}) }
function unchangedB() { assert.equal(state().currentUser.id,'43'); assert.equal(state().accessToken,'B') }
;(async () => {
  for (const status of [200,401,403]) {
    reset(); const pending=observed(refreshAuth()); await tick()
    state().clearAuth(); state().setAuth('B',{id:'43'})
    status===200 ? requests[0].ok({accessToken:'late-A',memberId:42}) : requests[0].fail(status)
    assert.ok((await pending).error instanceof session.AuthSessionChangedError); unchangedB()
  }
  // Real login API queues behind refresh, including the response that sets the cookie.
  reset(); const old=observed(refreshAuth()); await tick()
  const login=authApi.login('b@example.invalid','synthetic'); await tick()
  assert.equal(requests.length,1)
  requests[0].ok({accessToken:'late-A',memberId:42}); await old; await tick()
  assert.equal(requests[1].config.url,'/api/v1/auth/login')
  requests[1].ok({accessToken:'B',memberId:43}); await tick()
  assert.equal(requests[2].config.headers.Authorization,'Bearer B')
  requests[2].ok({id:'43'}); const result=await login
  unchangedB(); assert.ok(session.isCurrentAuthSession(result.session))

  // A delayed profile response cannot commit or route after a newer auth intent.
  reset(); const a=observed(authApi.login('a@example.invalid','synthetic')); await tick()
  requests[0].ok({accessToken:'new-A',memberId:42}); await tick()
  const b=authApi.login('b@example.invalid','synthetic'); await tick()
  requests[1].ok({id:'42'}); assert.ok((await a).error instanceof session.AuthSessionChangedError); await tick()
  requests[2].ok({accessToken:'B',memberId:43}); await tick(); requests[3].ok({id:'43'}); await b; unchangedB()

  // Old HTTP successes and 401s cannot populate B's cache or replay A's mutation as B.
  for (const status of [200,401]) {
    reset(); const pending=observed(api.post('/private-action',{id:700})); await tick()
    state().clearAuth(); state().setAuth('B',{id:'43'})
    status===200 ? requests[0].ok({secret:'A'}) : requests[0].fail(401)
    assert.ok((await pending).error instanceof session.AuthSessionChangedError)
    assert.equal(requests.length,1); unchangedB()
  }
  reset(); const expired=observed(refreshAuth()); await tick()
  storage.set('ovlo-auth-session-change','other-tab-login')
  requests[0].ok({accessToken:'late-A',memberId:42})
  assert.ok((await expired).error instanceof session.AuthSessionChangedError)
  assert.notEqual(state().accessToken,'late-A')

  reset(); const one=refreshAuth(), two=refreshAuth(); assert.equal(one,two); await tick()
  requests[0].ok({accessToken:'rotated-A',memberId:42}); assert.equal(await one,'rotated-A')
  reset(); const mismatch=observed(refreshAuth()); await tick(); requests[0].ok({accessToken:'B',memberId:43})
  assert.ok((await mismatch).error); assert.equal(state().accessToken,null)
  reset(); const ended=session.captureAuthSession(); state().clearAuth(); assert.ok(session.isEndedAuthSession(ended))
  session.beginAuthChange(); assert.equal(session.isEndedAuthSession(ended),false,'A queued expiry callback must not redirect a new login')
  console.log('PASS: stale refresh success/failure, cookie ordering, delayed profile, stale HTTP success/401, cross-tab invalidation, refresh single-flight and member identity')
})().catch(e=>{console.error(e);process.exitCode=1})