// Actual auth actions and office query options with real QueryClient; transport is synthetic.
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { QueryClient } = require('@tanstack/react-query')
const root = path.resolve(__dirname, '..')
global.window = { localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} } }
function load(file, overrides = {}) {
  const code = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true },
  }).outputText
  const m = { exports: {} }
  new Function('require', 'module', 'exports', code)(
    name => name in overrides ? overrides[name] : require(name), m, m.exports)
  return m.exports
}
const caches = load('src/utils/authQueryCache.ts')
const { useAuthStore } = load('src/store/authStore.ts', { '../utils/authQueryCache': caches })
const store = useAuthStore.getState
const qc = new QueryClient({ defaultOptions: { queries: { staleTime: 60000, retry: false } } })
const unregister = caches.registerAuthQueryClient(qc)
let calls = 0, pendingResolve, observedSignal
const officeApi = {
  me: async () => { calls++; return { member: store().currentUser.id, officeId: 5 } },
  myConsultations: async () => { calls++; return [{ id: 700, privateText: 'account-' + store().currentUser.id }] },
  myTranscript: (office, id, lang, signal) => {
    observedSignal = signal
    return new Promise(resolve => { pendingResolve = resolve })
  },
}
const hooks = load('src/hooks/useOffice.ts', {
  '@tanstack/react-query': { useQuery: x => x, useMutation: x => x, useQueryClient: () => qc },
  '../store/authStore': { useAuthStore: selector => selector(store()) },
  '../api/office': { officeApi },
})
async function withdrawThroughProfile() {
  const file = ts.createSourceFile('ProfilePage.tsx',
    fs.readFileSync(path.join(root, 'src/views/member/ProfilePage.tsx'), 'utf8'),
    ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX)
  let expression
  function visit(node) {
    if (ts.isVariableDeclaration(node) && node.name.getText(file) === 'handleWithdraw') expression = node.initializer.getText(file)
    ts.forEachChild(node, visit)
  }
  visit(file)
  assert.ok(expression)
  const code = ts.transpileModule('module.exports = ' + expression,
    { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS } }).outputText
  const m = { exports: {} }
  new Function('memberApi', 'id', 'clearAuth', 'router', 'confirm', 't', 'module', code)(
    { withdraw: async () => {} }, 42, store().clearAuth, { push: () => {} }, () => true, x => x, m)
  await m.exports()
}
;(async () => {
  const a = { id: '42' }, b = { id: '43' }
  store().setAuth('A', a)
  const keyA = hooks.useMyConsultations(5).queryKey
  await qc.fetchQuery(hooks.useMyOffice())
  assert.equal((await qc.fetchQuery(hooks.useMyConsultations(5)))[0].privateText, 'account-42')
  await withdrawThroughProfile()
  assert.equal(qc.getQueryCache().getAll().length, 0, 'Withdrawal must clear all private caches')
  assert.equal(hooks.useMyOffice().enabled, false)
  store().setAuth('B', b)
  assert.notDeepEqual(hooks.useMyConsultations(5).queryKey, keyA)
  assert.equal((await qc.fetchQuery(hooks.useMyOffice())).member, '43')
  assert.equal((await qc.fetchQuery(hooks.useMyConsultations(5)))[0].privateText, 'account-43')
  assert.equal(calls, 4, 'New account must fetch its own data')

  // Token rotation and same-member profile updates must not discard useful caches.
  store().setAccessToken('rotated-B')
  store().setCurrentUser({ ...b, nickname: 'updated' })
  assert.equal(qc.getQueryCache().getAll().length, 2)

  // Direct account switches also clear; no reliance on a particular logout screen.
  store().setAuth('A-again', a)
  assert.equal(qc.getQueryCache().getAll().length, 0)
  const transcriptA = hooks.useTranscript(5, 700, 'ko', false, true)
  const pending = qc.fetchQuery(transcriptA).catch(() => null)
  await Promise.resolve()
  assert.ok(observedSignal)
  store().setAuth('B-again', b)
  assert.equal(observedSignal.aborted, true, 'Account switch aborts in-flight transcript requests')
  pendingResolve([{ privateText: 'late account-42 response' }])
  await pending
  assert.equal(qc.getQueryData(transcriptA.queryKey), undefined, 'Late responses cannot repopulate old cache')
  assert.notDeepEqual(hooks.useTranscript(5, 700, 'ko', false, true).queryKey, transcriptA.queryKey)

  // Expiration follows the same central cleanup path.
  await qc.fetchQuery(hooks.useMyOffice())
  const axios = { post: async () => { throw { response: { status: 401 } } }, isAxiosError: () => true }
  const { refreshAuth } = load('src/utils/refreshAuth.ts', { axios, '@/store/authStore': { useAuthStore } })
  assert.equal(await refreshAuth(), null)
  assert.equal(qc.getQueryCache().getAll().length, 0)
  console.log('PASS: withdrawal, account switch, expiration, scoped keys, late response abort, same-account refresh')
})().catch(e => { console.error(e); process.exitCode = 1 }).finally(() => { unregister(); qc.clear() })
