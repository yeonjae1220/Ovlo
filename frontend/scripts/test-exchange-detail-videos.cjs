// 교환대학 상세 화면: 분석 값이 없는 영상 행은 "후기 카드"가 아니라 영상 링크 구역으로 보여 준다.
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

const { hasReviewAnalysis } = load('src/utils/videoReview.ts')
const linkOnly = { id: 1, youtubeUrl: 'https://www.youtube.com/watch?v=aaaaaaaaaa1', title: '링크만 있는 영상', channel: '국제처', publishedAt: null, country: null, city: null, sourceLang: 'ko', direction: 'OUTBOUND', tags: [], overallRating: null, summary: null, dormAvailable: null }
assert.equal(hasReviewAnalysis(linkOnly), false)
assert.equal(hasReviewAnalysis({ ...linkOnly, summary: '  ' }), false)
for (const extra of [{ summary: '좋았다' }, { overallRating: 4 }, { recommend: false }, { dormAvailable: false }, { costRent: '500' }, { visaType: 'D-2' }, { difficulty: 3 }, { tags: ['교환'] }, { languageReq: 'IELTS 6.5' }]) {
  assert.equal(hasReviewAnalysis({ ...linkOnly, ...extra }), true, JSON.stringify(extra))
}

const { messages } = load('src/i18n/messages.ts')
// 개수 표시는 "후기"가 아니라 "영상"이라고 적는다
assert.equal(messages.ko['exch.reviews'], '영상')
assert.equal(messages.ko['catalog.badge.reviews'], '영상')
assert.equal(messages.en['exch.reviews'], 'videos')
assert.equal(messages.en['catalog.badge.reviews'], 'Videos')
for (const lang of Object.keys(messages)) {
  for (const key of ['exch.reviews', 'catalog.badge.reviews']) {
    if (messages[lang][key]) assert.doesNotMatch(messages[lang][key], /review|리뷰|후기|レビュー|评价|reseña|avis|Bewertung|avalia|отзыв/i, `${lang} ${key}`)
  }
}

// 안내 문구도 "영상 후기를 요약"·"후기 검색"이라고 하지 않는다 — 리포트는 공개 자료·공식 자료를 정리한 것이고 영상은 링크일 뿐이다
const WORDING_KEYS = ['landing.hero.desc', 'landing.feature1.desc', 'landing.feature2.title', 'landing.feature2.desc', 'univ.search.info',
  'univ.reports.aiDisclaimer', 'univ.detail.aiDisclaimer', 'exch.subtitle', 'exch.detail.aiDisclaimer']
for (const lang of Object.keys(messages)) {
  for (const key of WORDING_KEYS) {
    assert.ok(messages[lang][key], `${lang} ${key} exists`)
    assert.doesNotMatch(messages[lang][key], /review|리뷰|후기|レビュー|评价|reseña|avis\b|Bewertung|Erfahrungsbericht|Videobericht|avalia|отзыв|видеообзор/i, `${lang} ${key}`)
  }
  assert.equal(messages[lang]['univ.reports.aiDisclaimer'], messages[lang]['exch.detail.aiDisclaimer'], `${lang} disclaimers agree`)
}
assert.match(messages.ko['univ.reports.aiDisclaimer'], /공개 자료와 대학 공식 자료/)
assert.match(messages.ko['univ.detail.aiDisclaimer'], /공개 자료와 대학 공식 자료/)
assert.match(messages.en['univ.reports.aiDisclaimer'], /public and official university sources/)
assert.match(messages.ko['exch.subtitle'], /교환학생 영상/)

function render({ univ, reviews, direction }) {
  const sectionCalls = []
  const Page = load('src/views/university/ExchangeUniversityDetailPage.tsx', {
    react: { ...React, useState: init => [direction !== undefined && init === undefined ? direction : init, () => {}] },
    'react-markdown': { __esModule: true, default: () => null },
    'next/navigation': { useParams: () => ({ id: String(univ.id) }), useRouter: () => ({ back() {}, push() {} }) },
    '../../hooks/useUniversity': {
      useExchangeUniversity: () => ({ data: univ, isLoading: false, isError: false, refetch() {} }),
      useExchangeUniversityReviews: () => ({ data: { content: reviews, totalElements: reviews.length }, isLoading: false }),
      useUniversityReportByExchangeUniv: () => ({ data: null }),
    },
    '../../i18n/I18nProvider': { useI18n: () => ({ t: key => messages.ko[key] ?? messages.en[key] ?? key, language: 'ko' }) },
    '../../components/ui': { QueryErrorNotice: () => null },
    '../../utils/videoReview': { hasReviewAnalysis },
    './VideoLinkSection': { __esModule: true, default: props => { sectionCalls.push(props); return React.createElement('div', { 'data-video-links': props.globalUnivId }) } },
  }).default
  return { html: renderToStaticMarkup(React.createElement(Page)), sectionCalls }
}
const univ = { id: 8507, nameKo: null, nameEn: "King's College London", country: '영국', city: '', globalUnivId: 8394, reviewCount: 2 }

// 링크만 있는 행: 영상 링크 구역으로만 나오고, 후기 카드·방향 필터·"리뷰 (N)" 제목은 없다
const only = render({ univ, reviews: [linkOnly, { ...linkOnly, id: 2, title: '둘째 영상' }] })
assert.deepEqual(only.sectionCalls, [{ globalUnivId: 8394, locale: 'ko' }])
assert.match(only.html, /data-video-links="8394"/)
assert.doesNotMatch(only.html, /링크만 있는 영상|둘째 영상/)
assert.doesNotMatch(only.html, /리뷰/)
assert.doesNotMatch(only.html, /Outbound|해외 파견|✈️/)
assert.match(only.html, /2 영상/)
// 도시가 비어 있으면 "영국 · " 처럼 구분점만 남기지 않는다
assert.match(only.html, />영국</)
assert.doesNotMatch(only.html, /영국 ·/)

// 분석 값이 있는 후기는 예전처럼 카드로 나오고, 링크만 있는 행은 그 목록에 섞이지 않는다
const mixed = render({ univ, reviews: [linkOnly, { ...linkOnly, id: 3, title: '분석된 후기', summary: '수업이 좋았다', overallRating: 4 }] })
assert.match(mixed.html, /분석된 후기/)
assert.match(mixed.html, /수업이 좋았다/)
assert.doesNotMatch(mixed.html, /링크만 있는 영상/)
assert.match(mixed.html, /data-video-links="8394"/)

// 대표 대학에 연결되지 않은 행은 영상 링크 API 를 쓸 수 없으므로 예전 목록을 그대로 보여 준다
const orphan = render({ univ: { ...univ, globalUnivId: undefined, city: '런던' }, reviews: [linkOnly] })
assert.match(orphan.html, /링크만 있는 영상/)
assert.match(orphan.html, /영국 · 런던/)

// 영상도 후기도 없으면 빈 상태 문구
const empty = render({ univ: { ...univ, reviewCount: 0 }, reviews: [] })
assert.match(empty.html, new RegExp(messages.ko['exch.empty']))

console.log('exchange detail videos: ok')
