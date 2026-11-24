const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const ctx = { exports: {}, URL }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/utils/returnPath.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, ctx)
const { safeReturnPath, loginPath } = ctx.exports
for (const value of ['//evil.test', '/%2fevil.test', '/%5cevil.test', 'https://evil.test', '/login?next=/chat', '/oauth/callback', '/%00evil', '/%ZZ', '/a/../login']) {
  assert.equal(safeReturnPath(value), '/boards', value)
}
assert.equal(safeReturnPath('/marketplace/42?currency=USD'), '/marketplace/42?currency=USD')
assert.equal(loginPath('/posts/new?boardId=22'), '/login?next=%2Fposts%2Fnew%3FboardId%3D22')
console.log('Return path tests passed')
