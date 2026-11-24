// 유튜브 주소 판별과 영상 링크 구역 렌더링을 브라우저 없이 검사한다.
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

const youtube = load('src/utils/youtube.ts')
const ID = 'abcDEF_-123'
assert.equal(youtube.youtubeVideoId(`https://www.youtube.com/watch?v=${ID}`), ID)
assert.equal(youtube.youtubeVideoId(`https://youtube.com/watch?v=${ID}&t=10s`), ID)
assert.equal(youtube.youtubeVideoId(`https://m.youtube.com/watch?v=${ID}`), ID)
assert.equal(youtube.youtubeVideoId(`https://youtu.be/${ID}`), ID)
assert.equal(youtube.youtubeVideoId(`https://www.youtube.com/shorts/${ID}`), ID)
// 유튜브가 아니거나 형식이 다른 주소는 링크로 만들지 않는다
for (const bad of [null, '', 'not a url', `http://www.youtube.com/watch?v=${ID}`, `https://evil.example/watch?v=${ID}`,
  `https://www.youtube.com.evil.example/watch?v=${ID}`, 'https://www.youtube.com/watch?v=short', `javascript:alert('${ID}')`,
  `https://www.youtube.com/watch?v=${ID}"onmouseover="x`, `https://music.youtube.com/watch?v=${ID}`,
  `https://www.youtube.com/embed/${ID}`, `https://youtube.com./watch?v=${ID}`, `https://www.youtube.com/watch?v=${ID}%20`]) {
  assert.equal(youtube.youtubeVideoId(bad), null, String(bad))
  assert.equal(youtube.youtubeLink(bad), null, String(bad))
}
assert.deepEqual(youtube.youtubeLink(`https://youtu.be/${ID}?si=tracking`), {
  href: `https://www.youtube.com/watch?v=${ID}`,
  thumbnail: `https://i.ytimg.com/vi/${ID}/mqdefault.jpg`,
})

// 원본 주소의 계정 정보·추적 값은 새로 만든 링크에 실리지 않는다
assert.equal(youtube.youtubeLink(`https://user:pw@www.youtube.com/watch?v=${ID}&list=x#frag`).href, `https://www.youtube.com/watch?v=${ID}`)

const { messages } = load('src/i18n/messages.ts')
for (const lang of Object.keys(messages)) {
  for (const key of ['report.videos.title', 'report.videos.note', 'report.videos.more', 'report.videos.less']) {
    assert.ok(messages[lang][key], `${lang} ${key}`)
  }
}

function render(content, expanded = false, locale = 'ko') {
  const Section = load('src/views/university/VideoLinkSection.tsx', {
    react: { ...React, useState: () => [expanded, () => {}] },
    '../../i18n/I18nProvider': { useI18n: () => ({ t: key => messages.ko[key] }) },
    '../../hooks/useUniversity': { useUniversityVideoLinks: (id, size) => { assert.equal(id, 7); assert.equal(size, 12); return { data: content && { content } } } },
    '../../utils/youtube': youtube,
  }).default
  return renderToStaticMarkup(React.createElement(Section, { globalUnivId: 7, locale }))
}
const link = (n, extra = {}) => ({ id: n, youtubeUrl: `https://www.youtube.com/watch?v=${String(n).padStart(11, 'a')}`, title: `영상 ${n}`, channel: '국제교류팀', publishedAt: '2026-03-01T00:00:00Z', ...extra })

// 게시일: 잘못된 로캘(주소창의 lang 값)이나 읽을 수 없는 날짜가 화면을 깨뜨리지 않는다
const { formatPublishedDate } = load('src/views/university/VideoLinkSection.tsx', {
  '../../i18n/I18nProvider': {}, '../../hooks/useUniversity': {}, '../../utils/youtube': youtube,
})
assert.equal(formatPublishedDate('2026-03-01T00:00:00Z', 'en-US'), '3/1/2026')
assert.equal(formatPublishedDate('2026-03-01T23:30:00-08:00', 'en-US'), '3/2/2026') // 보는 곳의 시간대가 아니라 UTC 날짜
assert.doesNotThrow(() => formatPublishedDate('2026-03-01T00:00:00Z', 'en_US'))
assert.match(formatPublishedDate('2026-03-01T00:00:00Z', 'en_US'), /2026/)
assert.equal(formatPublishedDate('not a date', 'ko'), null)
assert.equal(formatPublishedDate(null, 'ko'), null)

// 영상이 없거나 아직 못 받았으면 구역 자체를 그리지 않는다
assert.equal(render(undefined), '')
assert.equal(render([]), '')
// 유튜브 주소가 아닌 행만 있으면 그리지 않는다
assert.equal(render([link(1, { youtubeUrl: 'https://evil.example/x' })]), '')

const few = render([link(1), link(2, { youtubeUrl: 'https://evil.example/x' }), link(3, { title: '<img src=x onerror=alert(1)>', channel: null, publishedAt: null })])
assert.match(few, /교환학생 영상/)
assert.match(few, /Ovlo가 내용을 확인한 것은 아닙니다/)
assert.equal((few.match(/<li>/g) ?? []).length, 2)
assert.match(few, /href="https:\/\/www\.youtube\.com\/watch\?v=aaaaaaaaaa1" target="_blank" rel="noopener noreferrer"/)
assert.match(few, /src="https:\/\/i\.ytimg\.com\/vi\/aaaaaaaaaa1\/mqdefault\.jpg"/)
assert.doesNotMatch(few, /evil\.example/)
assert.doesNotMatch(few, /<img src=x/) // 제목은 텍스트로만 들어간다
assert.match(few, /&lt;img src=x onerror=alert\(1\)&gt;/)
assert.doesNotMatch(few, /<button/) // 접을 것이 없으면 버튼도 없다
assert.doesNotMatch(few, /Invalid Date/)
// 제목이 없으면 긴 주소 대신 짧은 이름을 쓴다
assert.match(render([link(1, { title: null })]), />YouTube</)

// 주소창에서 온 잘못된 lang 값으로도 구역이 그려진다(리포트 화면 전체가 깨지지 않는다)
assert.match(render([link(1)], false, 'en_US'), /영상 1/)
assert.match(render([link(1, { publishedAt: 'garbage' })]), /국제교류팀</)

const many = Array.from({ length: 7 }, (_, i) => link(i + 1))
const collapsed = render(many)
assert.equal((collapsed.match(/<li>/g) ?? []).length, 4)
assert.match(collapsed, /aria-expanded="false" aria-controls="video-links-list"[^>]*>더 보기</)
const open = render(many, true)
assert.equal((open.match(/<li>/g) ?? []).length, 7)
assert.match(open, /aria-expanded="true"[^>]*>접기</)

console.log('video links: ok')
