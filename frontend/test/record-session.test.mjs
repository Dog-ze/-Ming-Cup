import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')
const toDataUrl = (source) => 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')

const {
  clearRecordSession,
  readRecordSession,
  writeRecordSession
} = await import(toDataUrl(await read('src/utils/record-session.js')))

/** 构造与 uni.setStorageSync / getStorageSync / removeStorageSync 等价的存储桩。 */
function installUniStub(initialToken = 'token-a') {
  const storage = new Map()
  if (initialToken) storage.set('token', initialToken)

  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key)
  }

  return storage
}

test('同一账号下茶记缓存可以正常读写', () => {
  installUniStub('token-a')

  writeRecordSession({ records: [{ id: 1 }], page: 1, total: 1 })

  const session = readRecordSession()
  assert.deepEqual(session.records, [{ id: 1 }])
  assert.equal(session.total, 1)
})

test('退出登录后茶记缓存必须清空，不能再读出来', () => {
  const storage = installUniStub('token-a')
  writeRecordSession({ records: [{ id: 1 }, { id: 2 }], page: 1, total: 2 })

  clearRecordSession()
  assert.equal(storage.has('record_list_cache'), false)

  // 退出登录 = token 被移除，此时即使缓存还在也必须返回空会话。
  storage.delete('token')
  assert.deepEqual(readRecordSession().records, [])
})

test('换账号登录时，上一个账号的茶记缓存不会被读出来', () => {
  const storage = installUniStub('token-a')
  writeRecordSession({ records: [{ id: 1 }], page: 1, total: 1 })

  // 另一个账号登录：令牌变了，缓存必须失效。
  storage.set('token', 'token-b')
  assert.deepEqual(readRecordSession().records, [], 'B 账号不能看到 A 账号的茶记')

  // 旧缓存被识别为过期后会被顺手删除。
  assert.equal(storage.has('record_list_cache'), false)
})

test('旧账号请求不能使用新账号 token 写入茶记缓存', () => {
  const storage = installUniStub('token-b')

  writeRecordSession({
    records: [{ id: 'record-of-a' }],
    page: 1,
    total: 1,
    expectedToken: 'token-a',
  })

  assert.equal(storage.has('record_list_cache'), false)
})

test('退出登录（clearLoginState）会同时清掉茶记缓存', async () => {
  const source = await read('src/store/user.js')
  assert.match(source, /import \{ clearRecordSession \} from '@\/utils\/record-session\.js'/)
  assert.match(source, /clearLoginState\(\) \{[\s\S]*?clearRecordSession\(\)/)
})

test('登录失效（401）时也会清掉茶记缓存', async () => {
  const source = await read('src/utils/request.js')
  assert.match(source, /clearRecordSession\(\)/)
})

test('茶记页在未登录时清空列表，在已登录时按账号恢复缓存', async () => {
  const source = await read('src/pages/record/list.vue')

  assert.match(source, /import \{ clearRecordSession, readRecordSession, writeRecordSession \}/)
  // 未登录分支必须先清空，否则常驻的 tab 页会继续显示上一个账号的数据。
  assert.match(source, /if \(!isLoggedIn\.value\) \{[\s\S]*?resetLocalState\(\)[\s\S]*?return/)
  assert.match(source, /function resetLocalState\(\) \{[\s\S]*?records\.value = \[\]/)
  assert.match(source, /restoreLocalState\(\)/)
  assert.match(source, /function persistCache\(expectedToken = authToken\.value\)/)
  assert.match(source, /import \{ authSessionVersion, authToken \} from '@\/utils\/auth-session\.js'/)
  assert.match(source, /function isCurrentRecordSession\(token, sessionVersion\)/)
  assert.match(source, /expectedToken/)
})

test('登录成功前清掉上一个账号的茶记缓存（避免短暂显示别人的数据）', async () => {
  const source = await read('src/pages/login/login.vue')
  assert.match(source, /import \{ clearRecordSession \} from '@\/utils\/record-session\.js'/)

  // 登录态写入之前必须先清缓存，顺序不能反。
  // 写入登录态统一走 setAuthToken（响应式 token + 本地存储），不再直接 setStorageSync。
  const order = source.indexOf('clearRecordSession()')
  const setToken = source.indexOf('setAuthToken(')
  assert.ok(order > -1 && setToken > -1, '登录页缺少清缓存或写入登录态的语句')
  assert.ok(order < setToken, '清缓存必须发生在写入新登录态之前')
})
