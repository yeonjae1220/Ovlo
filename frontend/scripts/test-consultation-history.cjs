// Render the real history/detail pages: current school credentials are not a prerequisite.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const deps = ''
const ts = require(deps + 'typescript')
const React = require(deps + 'react')
const { renderToStaticMarkup } = require(deps + 'react-dom/server')
let office = { officeId: 2, officeName: 'Office', verifiedStudent: true }
let calledWith
const query = { data: { totalPages: 1, content: [{ id: 1, question: 'EXISTING_CONSULTATION', openedAt: '2026-11-17', status: 'OPEN' }] } }
const mocks = {
  './officeFormatting': { formatDateTime: String },
  'next/link': { __esModule: true, default: ({children}) => React.createElement('a', null, children) },
  'next/navigation': { useRouter: () => ({ push() {} }) },
  '../../i18n/I18nProvider': { useI18n: () => ({ t: k => k }) },
  '../../hooks/useOffice': {
    useMyOffice: () => ({ data: office }),
    useOwnConsultations: page => { calledWith=page; return query },
  },
  '../../components/ui': {
    Card: ({children}) => React.createElement('section', null, children),
    EmptyState: ({title}) => React.createElement('p', null, title),
    PageHeader: () => null, QueryErrorNotice: () => null, SkeletonLines: () => null,
  },
  './consultationUi': { ConsultationStatusBadge: () => null },
}
const file = path.join(__dirname, '../src/views/office/MyConsultationsPage.tsx')
const code = ts.transpileModule(fs.readFileSync(file, 'utf8'), {compilerOptions: {
  module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
}}).outputText
const m = { exports: {} }
new Function('require', 'module', 'exports', code)(name => mocks[name] ?? require(deps+name), m, m.exports)
const render = () => renderToStaticMarkup(React.createElement(m.exports.default))
assert.match(render(), /EXISTING_CONSULTATION/)
assert.equal(calledWith, 0)
office = null // /office/me returns 404 after credential expiry; useMyOffice maps it to null.
assert.match(render(), /EXISTING_CONSULTATION/)
assert.equal(calledWith, 0, 'Own history remains available without an active school credential')
console.log('PASS: own consultation page remains reachable after school verification expires')

const consultation = { id: 1, officeId: 2, question: 'OWN_DETAIL', status: 'OPEN', lang: 'VI', openedAt: '2026-11-17', chatRoomId: 8 }
let detailResult = { data: consultation }
let transcriptOffice
const detailMocks = {
  ...mocks,
  './officeFormatting': { formatDateTime: String, noticeLangOf: () => 'vi' },
  'next/navigation': { useParams: () => ({ id: '1' }) },
  '../../store/authStore': { useAuthStore: () => ({ currentUser: { id: '42' } }) },
  '../../hooks/useOffice': {
    useMyOffice: () => ({ data: null, isError: true }),
    useOwnConsultation: () => detailResult,
    useStaffConsultation: () => ({}),
    useTranscript: officeId => { transcriptOffice=officeId; return {} },
    useAssignConsultation: () => ({}), useCloseConsultation: () => ({}), useResolveConsultation: () => ({}),
  },
  '../../components/ui': { ...mocks['../../components/ui'], Button: () => null, LinkButton: () => null,
    QueryErrorNotice: () => React.createElement('p', null, 'QUERY_ERROR') },
  './consultationUi': { ConsultationStatusBadge: () => null, Transcript: () => null },
}
const detailCode = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/views/office/ConsultationDetailPage.tsx'), 'utf8'), { compilerOptions: {
  module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
}}).outputText
const dm = { exports: {} }
new Function('require', 'module', 'exports', detailCode)(name => detailMocks[name] ?? require(name), dm, dm.exports)
const renderDetail = () => renderToStaticMarkup(React.createElement(dm.exports.default, { staff: false }))
assert.match(renderDetail(), /OWN_DETAIL/)
assert.equal(transcriptOffice, 2, 'Use the owned consultation school, not current school membership')
detailResult = { isError: true, data: consultation }
assert.match(renderDetail(), /QUERY_ERROR/)
assert.doesNotMatch(renderDetail(), /OWN_DETAIL/, 'A failed detail request must not render cached private content')
console.log('PASS: direct detail, derived office, unrelated office lookup failure, explicit detail error')
