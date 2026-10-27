import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('../src/utils/ai-chat-session.js', import.meta.url), 'utf8')
const session = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))

function installUniStub(token = 'token-a') {
  const storage = new Map()
  if (token) storage.set('token', token)
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key)
  }
  return storage
}

const lateNight = new Date('2026-10-26T23:59:00+08:00')
const nextDay = new Date('2026-10-27T00:00:00+08:00')
const messages = [
  { id: 1, role: 'assistant', content: '你好' },
  { id: 2, role: 'user', content: '帮我推荐茶' }
]

test('当天保存的 AI 聊天只会恢复给同一账号', () => {
  installUniStub('token-a')
  session.writeAiChatSession({ messages, now: lateNight })

  assert.deepEqual(session.readAiChatSession({ now: lateNight }), messages)

  uni.setStorageSync('token', 'token-b')
  assert.deepEqual(session.readAiChatSession({ now: lateNight }), [])
})

test('跨过凌晨的 AI 聊天缓存会自动删除', () => {
  const storage = installUniStub('token-a')
  session.writeAiChatSession({ messages, now: lateNight })

  assert.deepEqual(session.readAiChatSession({ now: nextDay }), [])
  assert.equal(storage.has('ai_chat_session'), false)
})

test('清除 AI 聊天缓存后不会再恢复历史消息', () => {
  const storage = installUniStub('token-a')
  session.writeAiChatSession({ messages, now: lateNight })

  session.clearAiChatSession()
  assert.equal(storage.has('ai_chat_session'), false)
  assert.deepEqual(session.readAiChatSession({ now: lateNight }), [])
})

test('登录切换或登录失效时会清除 AI 聊天缓存', async () => {
  const userStore = await readFile(new URL('../src/store/user.js', import.meta.url), 'utf8')
  const request = await readFile(new URL('../src/utils/request.js', import.meta.url), 'utf8')
  const login = await readFile(new URL('../src/pages/login/login.vue', import.meta.url), 'utf8')

  assert.match(userStore, /clearAiChatSession\(\)/)
  assert.match(request, /clearAiChatSession\(\)/)
  assert.match(login, /clearAiChatSession\(\)/)
})
