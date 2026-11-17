// Render the real student notice page: scoped source data is visible and escaped, including translated notices.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
let notice = { id: 1, title: 'Training', body: 'The training lasts 1.5 hours.', lang: 'EN', machineTranslated: true,
  term: '2026-2', effectiveFrom: '2026-11-17', expiresAt: '2026-12-17',
  audience: '교환학생만 신청 가능 <img src=x onerror=alert(1)>', campus: '서울캠퍼스 한정',
  reference: 'PRIVATE_REFERENCE_SHOULD_NOT_RENDER', ownerMemberId: 987654 }
const mocks = {
  'next/navigation': { useParams: () => ({ id: '1' }) },
  '@tanstack/react-query': { useQuery: () => ({ data: notice }) },
  '../../api/office': { officeApi: {} },
  '../../hooks/useOffice': { useMyOffice: () => ({ data: { officeId: 2, verifiedStudent: true } }) },
  '../../i18n/I18nProvider': { useI18n: () => ({ language: 'en', t: k => k }) },
  '../../components/ui': { Card: ({children}) => React.createElement('section', null, children),
    EmptyState: () => null, QueryErrorNotice: () => null, SkeletonLines: () => null },
}
const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/views/office/StudentNoticeDetailPage.tsx'), 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText
const m = { exports: {} }
new Function('require', 'module', 'exports', code)(name => mocks[name] ?? require(name), m, m.exports)
const render = () => renderToStaticMarkup(React.createElement(m.exports.default))
const html = render()
assert.match(html, /교환학생만 신청 가능/)
assert.match(html, /서울캠퍼스 한정/)
assert.match(html, /office.notice.applicabilityOriginal/)
assert.match(html, /&lt;img/)
assert.doesNotMatch(html, /<img|PRIVATE_REFERENCE_SHOULD_NOT_RENDER|987654/)
notice = {...notice, audience: null, campus: null}
assert.doesNotMatch(render(), /office.notice.applicabilityOriginal|office.knowledge.audience|office.knowledge.campus/)
console.log('PASS: translated notice retains applicability, escapes source text, excludes internal fields and handles missing metadata')
