// 약관·개인정보처리방침에 외부 영상 링크와 YouTube 고지가 ko·en 에 들어 있고, 약관 번역이 정본과 같은 구성이며, 링크가 실제 앵커로 그려지는지 검사한다.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
const root = path.resolve(__dirname, '..')
const loaded = new Map()
// 상대 경로 import 는 같은 방식으로 TS 원본을 읽어 온다(legalContent → legalConstants, terms/*).
function resolveLocal(fromFile, name) {
  const base = path.join(path.dirname(fromFile), name)
  const found = ['.ts', '.tsx'].map(ext => base + ext).find(f => fs.existsSync(path.join(root, f)))
  if (!found) throw new Error(`Cannot resolve ${name} from ${fromFile}`)
  return found
}
function load(file, overrides = {}) {
  if (!Object.keys(overrides).length && loaded.has(file)) return loaded.get(file)
  const code = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true },
  }).outputText
  const m = { exports: {} }
  if (!Object.keys(overrides).length) loaded.set(file, m.exports)
  const requireFrom = name => {
    if (name in overrides) return overrides[name]
    if (name.startsWith('./') || name.startsWith('../')) return load(resolveLocal(file, name))
    return require(name)
  }
  new Function('require', 'module', 'exports', code)(requireFrom, m, m.exports)
  if (!Object.keys(overrides).length) loaded.set(file, m.exports)
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
// 이용약관 번역은 정본(ko)과 같은 버전·시행일·구성·링크이고, 연락처 자리도 그대로 남아 있어야 한다
const { LEGAL_CONTACT_PLACEHOLDER } = legal
const TERMS_LANGUAGES = ['ko', 'en', 'ja', 'zh', 'es', 'fr', 'de', 'pt', 'ru']
for (const lang of TERMS_LANGUAGES) {
  const doc = legal.TERMS_OF_SERVICE[lang]
  assert.ok(doc, `terms translation exists: ${lang}`)
  assert.equal(doc.version, legal.TERMS_OF_SERVICE.ko.version, `${lang} terms version`)
  assert.equal(doc.updatedAt, legal.TERMS_OF_SERVICE.ko.updatedAt, `${lang} terms date`)
  assert.equal(doc.sections.length, legal.TERMS_OF_SERVICE.ko.sections.length, `${lang} terms section count`)
  assert.deepEqual(hrefsOf(doc), hrefsOf(legal.TERMS_OF_SERVICE.ko), `${lang} terms links`)
  doc.sections.forEach((s, i) => {
    assert.ok(s.heading.startsWith(`${i + 1}. `), `${lang} terms: ${s.heading}`)
    const ko = legal.TERMS_OF_SERVICE.ko.sections[i]
    assert.equal(s.paragraphs.length, ko.paragraphs.length, `${lang} terms ${i + 1}: paragraphs`)
    assert.equal((s.bullets ?? []).length, (ko.bullets ?? []).length, `${lang} terms ${i + 1}: bullets`)
  })
  const placeholders = JSON.stringify(doc).split(LEGAL_CONTACT_PLACEHOLDER).length - 1
  assert.equal(placeholders, JSON.stringify(legal.TERMS_OF_SERVICE.ko).split(LEGAL_CONTACT_PLACEHOLDER).length - 1, `${lang} terms contact placeholder`)
  assert.match(JSON.stringify(doc), /24/, `${lang} terms: 24-hour review`)
}

// 약관 8조가 말하는 제휴 표시는 화면의 제휴 배지 문구와 같은 단어여야 한다
const { messages } = load('src/i18n/messages.ts')
for (const lang of TERMS_LANGUAGES) {
  const badge = messages[lang]['student.arrival.disclosure'].split(/\s*[·・]\s*/)[0]
  const partnerSection = JSON.stringify(legal.TERMS_OF_SERVICE[lang].sections[7])
  assert.ok(badge && partnerSection.includes(badge), `${lang}: terms section 8 uses the badge word "${badge}"`)
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
  '@/i18n/I18nProvider': { useI18n: () => ({ language: 'ja', t: key => key }) },
  './legalContent': legal,
}).default
const html = renderToStaticMarkup(React.createElement(Page, {
  documents: legal.TERMS_OF_SERVICE, related: { href: '/privacy', labelKey: 'legal.viewPrivacy' },
}))
const anchor = new RegExp(`<a href="${YOUTUBE_TERMS}"[^>]*>`).exec(html)
assert.ok(anchor, 'YouTube terms rendered as an anchor')
assert.match(anchor[0], /target="_blank"/)
assert.match(anchor[0], /rel="noopener noreferrer"/)
// UI 언어의 번역을 고르고, 번역이 없는 문서는 영어로 보여 준다
assert.ok(html.includes(legal.TERMS_OF_SERVICE.ja.title), 'ja UI shows the ja terms')
const privacyHtml = renderToStaticMarkup(React.createElement(Page, {
  documents: legal.PRIVACY_POLICY, related: { href: '/terms', labelKey: 'legal.viewTerms' },
}))
assert.ok(privacyHtml.includes(legal.PRIVACY_POLICY.en.title), 'missing translation falls back to en')

console.log('legal notice tests passed')
