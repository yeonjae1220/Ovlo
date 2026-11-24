// 약관·개인정보처리방침에 외부 영상 링크와 YouTube 고지가 양쪽 언어로 들어 있고, 링크가 실제 앵커로 그려지는지 검사한다.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
const root = path.resolve(__dirname, '..')
function load(file, overrides = {}) {
  const code = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true },
  }).outputText
  const m = { exports: {} }
  new Function('require', 'module', 'exports', code)(name => name in overrides ? overrides[name] : require(name), m, m.exports)
  return m.exports
}

const YOUTUBE_TERMS = 'https://www.youtube.com/t/terms'
const GOOGLE_PRIVACY = 'https://policies.google.com/privacy'
const legal = load('src/views/legal/legalContent.ts')
const linksOf = doc => doc.sections.flatMap(s => s.links ?? [])
const hrefsOf = doc => linksOf(doc).map(l => l.href)

for (const lang of ['ko', 'en']) {
  const terms = legal.TERMS_OF_SERVICE[lang]
  const privacy = legal.PRIVACY_POLICY[lang]
  // YouTube API 정책 III.A: 약관에 YouTube 약관 링크와 동의 문구, 처리방침에 API 사용 사실과 Google 개인정보처리방침 링크
  assert.ok(hrefsOf(terms).includes(YOUTUBE_TERMS), `${lang} terms: YouTube terms link`)
  assert.ok(hrefsOf(privacy).includes(GOOGLE_PRIVACY), `${lang} privacy: Google privacy link`)
  const termsText = JSON.stringify(terms)
  const privacyText = JSON.stringify(privacy)
  assert.match(termsText, /YouTube/, `${lang} terms mentions YouTube`)
  assert.match(privacyText, /YouTube API/, `${lang} privacy states use of YouTube API Services`)
  // 삭제 요청 창구가 약관에 있다
  assert.ok(/요청: |Requests: /.test(termsText), `${lang} terms: takedown contact`)
  for (const doc of [terms, privacy]) {
    for (const link of linksOf(doc)) {
      assert.match(link.href, /^https:\/\/[a-z0-9.-]+\//, `https only: ${link.href}`)
      assert.ok(link.label.trim().length > 0)
    }
    // 번호가 1부터 빠짐없이 이어진다
    doc.sections.forEach((s, i) => assert.ok(s.heading.startsWith(`${i + 1}. `), `${lang} ${doc.title}: ${s.heading}`))
  }
}
// 두 언어는 같은 버전·시행일·구성이어야 한다
for (const docs of [legal.TERMS_OF_SERVICE, legal.PRIVACY_POLICY]) {
  assert.equal(docs.ko.version, docs.en.version)
  assert.equal(docs.ko.updatedAt, docs.en.updatedAt)
  assert.equal(docs.ko.sections.length, docs.en.sections.length)
  assert.deepEqual(hrefsOf(docs.ko), hrefsOf(docs.en))
}

const Page = load('src/views/legal/LegalDocumentPage.tsx', {
  'next/link': { __esModule: true, default: ({ href, children, ...rest }) => React.createElement('a', { href, ...rest }, children) },
  '@/i18n/I18nProvider': { useI18n: () => ({ language: 'ko' }) },
  './legalContent': legal,
}).default
const html = renderToStaticMarkup(React.createElement(Page, {
  documents: legal.TERMS_OF_SERVICE, related: { href: '/privacy', label: { ko: '개인정보처리방침', en: 'Privacy Policy' } },
}))
const anchor = new RegExp(`<a href="${YOUTUBE_TERMS}"[^>]*>`).exec(html)
assert.ok(anchor, 'YouTube terms rendered as an anchor')
assert.match(anchor[0], /target="_blank"/)
assert.match(anchor[0], /rel="noopener noreferrer"/)

console.log('legal notice tests passed')
