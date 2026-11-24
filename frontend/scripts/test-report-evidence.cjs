const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')
const ts = require('typescript')
const filename = path.join(__dirname, '../src/utils/reportEvidence.ts')
const compiled = ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } })
const loaded = new Module(filename, module)
loaded._compile(compiled.outputText, filename)
const { reportEvidence } = loaded.exports
const now = new Date('2026-11-24T12:00:00Z')
const source = { url: 'https://university.example/exchange', title: 'Exchange office', type: 'official' }
assert.equal(reportEvidence(null, now).status, 'missing')
assert.equal(reportEvidence({ as_of: '2026-10' }, now).status, 'missing')
assert.equal(reportEvidence({ sources: [source] }, now).status, 'undated')
assert.equal(reportEvidence({ as_of: '2026-10', sources: [source] }, now).status, 'checkSource')
assert.equal(reportEvidence({ as_of: '2025-10', sources: [source] }, now).status, 'old')
for (const as_of of ['2026-02-30', '2026-13', '2027-01', 'yesterday', 123]) {
  assert.equal(reportEvidence({ as_of, sources: [source] }, now).status, 'undated')
}
assert.equal(reportEvidence({ as_of: '2024-02-29', sources: [source] }, now).asOf, '2024-02-29')
const unsafe = ['javascript:alert(1)', 'data:text/html,hi', '//evil.example', 'https://user:pass@university.example/', 'not a URL']
assert.equal(reportEvidence({ sources: unsafe.map(url => ({ url })) }, now).sources.length, 0)
const result = reportEvidence({ sources: [source, source, null, 123, { url: 'https://university.example/fees', title: 123 }] }, now)
assert.equal(result.sources.length, 2)
assert.equal(result.sources[1].title, 'university.example')
assert.deepEqual(reportEvidence({ sources: 'invalid' }, now).sources, [])
assert.equal(reportEvidence({ as_of: '2026-10', sources: [source] }, now).verified, false)
console.log('Report evidence checks passed: dates, missing evidence, stale information, unsafe links, deduplication; no inferred verification')
