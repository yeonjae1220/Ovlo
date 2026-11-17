// Render the real staff form; exercise its body change and submit handlers without a browser.
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
const { messages } = load('src/i18n/messages.ts')
const i18n = { useI18n: () => ({ t: key => messages.ko[key] ?? messages.en[key] }) }
const ui = load('src/components/ui/index.tsx', { '../../i18n/I18nProvider': i18n })
const limits = load('src/views/office/noticeLimits.ts')
const javaLimits = fs.readFileSync(path.join(root, '../src/main/java/me/yeonjae/ovlo/application/service/office/NoticeAnswerLimits.java'), 'utf8')
assert.equal(Number(javaLimits.match(/MAX_BODY_CHARS = ([\d_]+)/)[1].replaceAll('_', '')), limits.NOTICE_ANSWER_BODY_CHARS)
let state
const Form = load('src/views/office/NoticeForm.tsx', {
  react: { ...React, useId: () => 'long-body-warning', useState: initial => { state ??= initial; return [state, next => { state = next(state) }] } },
  '../../i18n/I18nProvider': i18n,
  '../../api/office': { NOTICE_CATEGORIES: ['HOUSING'] },
  '../../components/ui': ui,
  './noticeLimits': limits,
}).default
function find(node, predicate) {
  if (!node || typeof node !== 'object') return undefined
  if (predicate(node)) return node
  return React.Children.toArray(node.props?.children).map(child => find(child, predicate)).find(Boolean)
}
const valid = { category: 'HOUSING', title: '공지', body: '본문', term: '2027-1', effectiveFrom: '2027-02-01', expiresAt: '2027-03-01' }
for (const editing of [false, true]) {
  state = undefined
  let submitted
  const props = { initial: editing ? { ...valid, body: '가'.repeat(3001) } : undefined, pending: false, onSubmit: req => { submitted = req }, onCancel: () => {} }
  let tree = Form(props)
  assert.equal(renderToStaticMarkup(tree).includes(messages.ko['office.notice.longBodyWarning']), editing)
  if (!editing) {
    // Fill the real create form through its field change handlers.
    const fields = []
    function collect(node) {
      if (!node || typeof node !== 'object') return
      if ([ui.TextField, ui.TextAreaField].includes(node.type)) fields.push(node)
      React.Children.toArray(node.props?.children).forEach(collect)
    }
    collect(tree)
    ;[valid.title, valid.body, '', valid.term, valid.effectiveFrom, valid.expiresAt].forEach((value, i) => fields[i].props.onChange({ target: { value } }))
  }
  for (const [body, expected] of [
    ['가'.repeat(2999), false], ['가'.repeat(3000), false], ['가'.repeat(3001), true],
    [' \n' + '가'.repeat(3000) + '\t ', false], ['\u00a0' + '가'.repeat(3000), true],
    ['😀'.repeat(1500), false], ['😀'.repeat(1500) + '가', true], ['가'.repeat(20000), true], ['짧은 공지', false],
  ]) {
    tree = Form(props)
    find(tree, n => n.type === ui.TextAreaField).props.onChange({ target: { value: body } })
    tree = Form(props)
    const html = renderToStaticMarkup(tree)
    assert.equal(html.includes(messages.ko['office.notice.longBodyWarning']), expected)
    assert.equal(find(tree, n => n.type === ui.TextAreaField).props['aria-describedby'], expected ? 'long-body-warning' : undefined)
    assert.equal(find(tree, n => n.type === ui.Button && n.props.type === 'submit').props.disabled, false)
    tree.props.onSubmit({ preventDefault() {} })
    assert.equal(submitted.body, body, 'Warning must never truncate or block saving the notice')
  }
  assert.equal(find(Form({ ...props, pending: true }), n => n.type === ui.Button && n.props.type === 'submit').props.disabled, true)
}
console.log('PASS: create/edit warning, trim/UTF-16 boundaries, accessible hint, nonblocking full-body submission, Java limit parity')
