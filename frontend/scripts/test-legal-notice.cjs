// 약관·개인정보처리방침에 외부 영상 링크와 YouTube 고지가 들어 있고, 모든 번역이 정본과 같은 구성이며, 링크가 실제 앵커로 그려지는지 검사한다.
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
// 번역은 정본(ko)과 같은 버전·시행일·구성·링크이고, 연락처 자리도 그대로 남아 있어야 한다
const { LEGAL_CONTACT_PLACEHOLDER } = legal
const TERMS_LANGUAGES = ['ko', 'en', 'ja', 'zh', 'es', 'fr', 'de', 'pt', 'ru']
const countPlaceholders = doc => JSON.stringify(doc).split(LEGAL_CONTACT_PLACEHOLDER).length - 1
for (const [name, docs] of [['terms', legal.TERMS_OF_SERVICE], ['privacy', legal.PRIVACY_POLICY]]) {
  for (const lang of TERMS_LANGUAGES) {
    const doc = docs[lang]
    assert.ok(doc, `${name} translation exists: ${lang}`)
    assert.equal(doc.version, docs.ko.version, `${lang} ${name} version`)
    assert.equal(doc.updatedAt, docs.ko.updatedAt, `${lang} ${name} date`)
    assert.equal(doc.sections.length, docs.ko.sections.length, `${lang} ${name} section count`)
    assert.deepEqual(hrefsOf(doc), hrefsOf(docs.ko), `${lang} ${name} links`)
    doc.sections.forEach((s, i) => {
      assert.ok(s.heading.startsWith(`${i + 1}. `), `${lang} ${name}: ${s.heading}`)
      const ko = docs.ko.sections[i]
      assert.equal(s.paragraphs.length, ko.paragraphs.length, `${lang} ${name} ${i + 1}: paragraphs`)
      assert.equal((s.bullets ?? []).length, (ko.bullets ?? []).length, `${lang} ${name} ${i + 1}: bullets`)
    })
    assert.equal(countPlaceholders(doc), countPlaceholders(docs.ko), `${lang} ${name} contact placeholder`)
  }
}
// 모든 언어의 두 문서는 마지막 조항에서 한국어 원문이 정본이고 우선한다고 밝힌다
const KOREAN_WORD = { ko: /한국어/g, en: /Korean/g, ja: /韓国語/g, zh: /韩文/g, es: /coreano/g, fr: /coréen/g, de: /koreanisch/gi, pt: /coreano/g, ru: /корейск/gi }
for (const lang of TERMS_LANGUAGES) {
  for (const docs of [legal.TERMS_OF_SERVICE, legal.PRIVACY_POLICY]) {
    const last = JSON.stringify(docs[lang].sections.at(-1))
    assert.ok((last.match(KOREAN_WORD[lang]) ?? []).length >= 2, `${lang} ${docs[lang].title}: last clause names the Korean original as prevailing`)
  }
}
for (const lang of TERMS_LANGUAGES) {
  assert.match(JSON.stringify(legal.TERMS_OF_SERVICE[lang]), /24/, `${lang} terms: 24-hour review`)
  // YouTube API 정책 III.A: 처리방침이 API 사용 사실을 밝힌다
  // 번역은 그 언어의 어순을 따른다(예: Servicios de API de YouTube)
  assert.match(JSON.stringify(legal.PRIVACY_POLICY[lang]), /YouTube.{0,3}API|API.{0,12}YouTube/, `${lang} privacy states use of YouTube API Services`)
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

const loadPage = language => load('src/views/legal/LegalDocumentPage.tsx', {
  'next/link': { __esModule: true, default: ({ href, children, ...rest }) => React.createElement('a', { href, ...rest }, children) },
  '@/i18n/I18nProvider': { useI18n: () => ({ language, t: key => key }) },
  './legalContent': legal,
}).default
const Page = loadPage('ja')
const html = renderToStaticMarkup(React.createElement(Page, {
  documents: legal.TERMS_OF_SERVICE, related: { href: '/privacy', labelKey: 'legal.viewPrivacy' }, path: '/terms',
}))
const anchor = new RegExp(`<a href="${YOUTUBE_TERMS}"[^>]*>`).exec(html)
assert.ok(anchor, 'YouTube terms rendered as an anchor')
assert.match(anchor[0], /target="_blank"/)
assert.match(anchor[0], /rel="noopener noreferrer"/)
// UI 언어의 번역을 고르고, 번역이 없는 문서는 영어로 보여 준다
assert.ok(html.includes(legal.TERMS_OF_SERVICE.ja.title), 'ja UI shows the ja terms')
const untranslated = { ko: legal.PRIVACY_POLICY.ko, en: legal.PRIVACY_POLICY.en }
const fallbackHtml = renderToStaticMarkup(React.createElement(Page, {
  documents: untranslated, related: { href: '/terms', labelKey: 'legal.viewTerms' }, path: '/privacy',
}))
assert.ok(fallbackHtml.includes(legal.PRIVACY_POLICY.en.title), 'missing translation falls back to en')

// 번역본은 한국어 원문으로 링크하고, 한국어 원문에는 그 링크가 없다
assert.match(fallbackHtml, /<a href="\/privacy\/ko" hrefLang="ko"[^>]*>legal\.viewKoreanOriginal<\/a>/, 'en fallback links to the Korean original')
for (const documents of [legal.TERMS_OF_SERVICE, legal.PRIVACY_POLICY]) {
  const koHtml = renderToStaticMarkup(React.createElement(loadPage('ko'), {
    documents, related: { href: '/terms', labelKey: 'legal.viewTerms' }, path: '/privacy',
  }))
  assert.ok(koHtml.includes(documents.ko.title), 'ko UI shows the Korean original')
  assert.ok(!koHtml.includes('legal.viewKoreanOriginal'), 'Korean original does not link to itself')
}
for (const [lang, dict] of Object.entries(messages)) {
  for (const key of ['legal.viewKoreanOriginal', 'legal.viewTranslation']) {
    assert.ok(dict[key], `${lang} has ${key}`)
  }
}

// 번역본은 한국어 원문 경로로 링크하고, 원문 페이지는 번역본으로 돌아가는 링크를 둔다
assert.match(html, /<a href="\/terms\/ko" hrefLang="ko"[^>]*>legal\.viewKoreanOriginal<\/a>/, 'translation links to the Korean original')
assert.match(html, /<article lang="ja"/, 'translated article is marked with its language')
assert.match(fallbackHtml, /<article lang="en"/, 'en fallback is marked as English')
const renderOriginal = language => renderToStaticMarkup(React.createElement(loadPage(language), {
  documents: legal.TERMS_OF_SERVICE, related: { href: '/privacy', labelKey: 'legal.viewPrivacy' }, path: '/terms', original: true,
}))
const originalForJa = renderOriginal('ja')
assert.ok(originalForJa.includes(legal.TERMS_OF_SERVICE.ko.title), 'original route shows the Korean terms in a ja UI')
assert.match(originalForJa, /<article lang="ko"/, 'Korean original is marked as Korean')
assert.ok(!originalForJa.includes('legal.viewKoreanOriginal'), 'Korean original does not link to itself')
assert.match(originalForJa, /<a href="\/terms"[^>]*>legal\.viewTranslation<\/a>/, 'original links back to the translation')
assert.ok(!renderOriginal('ko').includes('legal.viewTranslation'), 'ko UI has no translation to return to')
assert.match(originalForJa, /<a href="\/privacy\/ko"[^>]*>legal\.viewPrivacy<\/a>/, 'original links to the Korean original of the related document')
assert.match(html, /<a href="\/privacy"[^>]*>legal\.viewPrivacy<\/a>/, 'translation links to the translated related document')

console.log('legal notice tests passed')
