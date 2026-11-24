const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const clients = []
class Client {
  constructor(config) { Object.assign(this, config); this.connected = false; clients.push(this) }
  activate() {}
  deactivate() { this.connected = false }
  publish() { this.sent = true }
}
const ctx = { exports: {}, require: name => name === '@stomp/stompjs' ? { Client } : name === 'sockjs-client' ? { default: class {} } : { useAuthStore: { getState: () => ({ accessToken: 'current' }) } } }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/utils/stomp.ts','utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, ctx)
async function main() {
  const api = ctx.exports.stompClient
  let connected = 0, disconnected = 0
  const pending = api.connect('initial', { onConnect: () => connected++, onDisconnect: () => disconnected++ })
  const c = clients[0]
  c.connected = true; c.onConnect(); await pending
  c.connected = false; c.onWebSocketClose?.()
  assert.equal(disconnected, 1, 'transport loss is visible')
  assert.equal(api.publish('1', 'keep draft'), false)
  c.connected = true; c.beforeConnect(); c.onConnect()
  assert.equal(connected, 2, 'subscription callback runs again after reconnect')
  assert.equal(c.connectHeaders.Authorization, 'Bearer current')
  assert.equal(api.publish('1','hello'), true)
  api.disconnect()
  c.onConnect()
  assert.equal(connected, 2, 'late callbacks from disposed connection ignored')
  console.log('Chat reconnect tests passed')
}
main().catch(e => { console.error(e); process.exitCode = 1 })
