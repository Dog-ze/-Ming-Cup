import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const sourceRoot = new URL('../src/', import.meta.url)

async function readSource(relativePath) {
  return readFile(new URL(relativePath, sourceRoot), 'utf8')
}

function toDataModule(source) {
  return 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
}

/*
 * request.js 使用 @ 别名引用 config/env.js（内含 Vite 专属的 import.meta.env），
 * node 无法直接解析；因此把依赖替换成 data: 模块后再加载，
 * 思路与 tab-bar.test.mjs 一致。
 */
async function loadRequestModule() {
  const envModule = toDataModule("export const BASE_URL = 'http://127.0.0.1:8080'")
  const apiErrorModule = toDataModule(await readSource('utils/api-error.js'))
  const recordSessionModule = toDataModule(await readSource('utils/record-session.js'))
  const aiChatSessionModule = toDataModule(await readSource('utils/ai-chat-session.js'))
  // request.js 现在从 auth-session 取响应式 token；
  // 真实模块 import 了裸包 'vue'，data: URL 里无法解析，因此替换成等价的最小实现。
  const authSessionModule = toDataModule(`
    export const authToken = { value: 'test-token' }
    export const authSessionVersion = { value: 0 }
    export function setAuthToken(token) {
      authToken.value = String(token || '')
      authSessionVersion.value += 1
      uni.setStorageSync('token', authToken.value)
    }
    export function clearAuthToken() {
      authToken.value = ''
      authSessionVersion.value += 1
      uni.removeStorageSync('token')
    }
  `)

  const source = (await readSource('utils/request.js'))
    .replace("'@/config/env.js'", `'${envModule}'`)
    .replace("'@/utils/api-error.js'", `'${apiErrorModule}'`)
    .replace("'@/utils/record-session.js'", `'${recordSessionModule}'`)
    .replace("'@/utils/ai-chat-session.js'", `'${aiChatSessionModule}'`)
    .replace("'@/utils/auth-session.js'", `'${authSessionModule}'`)

  return {
    requestModule: await import(toDataModule(source)),
    authSession: await import(authSessionModule),
  }
}

const { requestModule: { request }, authSession } = await loadRequestModule()
const { isAuthExpiredError, AUTH_ERROR_CODES } = await import(
  toDataModule(await readSource('utils/api-error.js'))
)

/** 构造 uni 全局假对象，记录存储与提示调用。 */
function createUniStub({ result, fail = false } = {}) {
  const storage = new Map([
    ['token', 'test-token'],
    ['user', { username: '茶友' }]
  ])
  const calls = { toasts: [], removed: [], stored: [] }

  globalThis.uni = {
    getStorageSync(key) {
      return storage.get(key)
    },
    setStorageSync(key, value) {
      storage.set(key, value)
      calls.stored.push([key, value])
    },
    removeStorageSync(key) {
      storage.delete(key)
      calls.removed.push(key)
    },
    showToast(options) {
      calls.toasts.push(options.title)
    },
    request(options) {
      // 贴近真实 uni.request：异步回调，不在调用栈内同步触发。
      queueMicrotask(() => {
        if (fail) options.fail({ errMsg: 'request:fail' })
        else options.success({ data: result })
      })
    }
  }

  return { calls, storage }
}

test('登录失效码包含未授权、旧版失效码与账号停用码', () => {
  assert.deepEqual(AUTH_ERROR_CODES, [401, 40101, 40103])
})

test('isAuthExpiredError 只认可后端登录失效业务码', () => {
  assert.equal(isAuthExpiredError({ code: 401 }), true)
  assert.equal(isAuthExpiredError({ code: 40101 }), true)
  assert.equal(isAuthExpiredError({ code: 40103 }), true)

  assert.equal(isAuthExpiredError({ code: 400 }), false)
  assert.equal(isAuthExpiredError({ code: 'NETWORK_ERROR' }), false)
  assert.equal(isAuthExpiredError(new Error('x')), false)
  assert.equal(isAuthExpiredError(undefined), false)
  assert.equal(isAuthExpiredError(null), false)
  assert.equal(isAuthExpiredError('40101'), false)
})

test('旧会话请求晚到的 401 不会清除新登录账号', async () => {
  const storage = new Map([
    ['token', 'token-a'],
    ['user', { username: '用户 A' }],
  ])
  const calls = { removed: [], toasts: [] }
  let pendingRequest

  globalThis.uni = {
    getStorageSync(key) {
      return storage.get(key)
    },
    setStorageSync(key, value) {
      storage.set(key, value)
    },
    removeStorageSync(key) {
      storage.delete(key)
      calls.removed.push(key)
    },
    showToast(options) {
      calls.toasts.push(options.title)
    },
    request(options) {
      pendingRequest = options
    },
  }

  authSession.setAuthToken('token-a')
  const oldRequest = request({ url: '/api/user/profile' })
  assert.equal(pendingRequest.header.Authorization, 'Bearer token-a')

  authSession.setAuthToken('token-b')
  storage.set('user', { username: '用户 B' })
  pendingRequest.success({ data: { code: 401, msg: '登录已失效' } })

  await assert.rejects(oldRequest, (error) => {
    assert.equal(error.isStaleSessionError, true)
    assert.equal(isAuthExpiredError(error), false)
    return true
  })

  assert.equal(authSession.authToken.value, 'token-b')
  assert.equal(storage.get('token'), 'token-b')
  assert.deepEqual(storage.get('user'), { username: '用户 B' })
  assert.deepEqual(calls.removed, [])
  assert.deepEqual(calls.toasts, [])
})

test('code 0 时正常返回 data，且不触碰登录态', async () => {
  const { calls, storage } = createUniStub({ result: { code: 0, data: { username: '茶友' } } })

  const data = await request({ url: '/api/user/profile' })

  assert.deepEqual(data, { username: '茶友' })
  assert.deepEqual(calls.removed, [])
  assert.deepEqual(calls.toasts, [])
  assert.equal(storage.get('token'), 'test-token')
})

test('code 40101 时清除 token/user、茶记与 AI 聊天缓存，并提示重新登录', async () => {
  const { calls, storage } = createUniStub({ result: { code: 40101, msg: '登录已失效' } })

  await assert.rejects(request({ url: '/api/user/profile' }), error => {
    assert.equal(error.code, 40101)
    assert.equal(error.isApiError, true)
    assert.equal(error.msg, '登录已失效')
    assert.equal(isAuthExpiredError(error), true)
    return true
  })

  // 登录失效等同退出登录，按账号缓存的茶记与 AI 聊天也要一起清掉。
  assert.deepEqual(calls.removed, ['token', 'user', 'record_list_cache', 'ai_chat_session'])
  assert.equal(storage.has('token'), false)
  assert.deepEqual(calls.toasts, ['登录已失效，请重新登录'])
})

test('其它业务错误只提示 msg，不清除登录态', async () => {
  const { calls, storage } = createUniStub({ result: { code: 400, msg: '参数不合法' } })

  await assert.rejects(request({ url: '/api/user/profile' }), error => {
    assert.equal(error.code, 400)
    assert.equal(error.isApiError, true)
    assert.equal(isAuthExpiredError(error), false)
    return true
  })

  assert.deepEqual(calls.removed, [])
  assert.equal(storage.get('token'), 'test-token')
  assert.deepEqual(calls.toasts, ['参数不合法'])
})

test('网络失败保留登录态，且绝不被判定为登录失效', async () => {
  const { calls, storage } = createUniStub({ fail: true })

  await assert.rejects(request({ url: '/api/user/profile' }), error => {
    assert.equal(error.code, 'NETWORK_ERROR')
    assert.equal(error.isNetworkError, true)
    assert.equal(isAuthExpiredError(error), false)
    return true
  })

  assert.deepEqual(calls.removed, [])
  assert.equal(storage.get('token'), 'test-token')
  assert.deepEqual(calls.toasts, ['网络连接失败，请检查网络或后端服务'])
})

test('用户仓库刷新资料失败时直接抛出，不清空已缓存资料', async () => {
  const source = await readSource('store/user.js')
  const refreshProfile = source.match(/async refreshProfile\(\) \{([\s\S]*?)\n {4}\},/)

  assert.notEqual(refreshProfile, null, '需要能定位到 refreshProfile 实现')
  assert.match(refreshProfile[1], /await getProfile\(\)/)
  assert.doesNotMatch(refreshProfile[1], /clearLoginState/)
  assert.doesNotMatch(refreshProfile[1], /\bcatch\b/)
  assert.doesNotMatch(refreshProfile[1], /this\.profile\s*=\s*null/)
})

test('个人页仅在登录失效时退出登录，网络失败保留登录态', async () => {
  const source = await readSource('pages/profile/profile.vue')

  assert.match(source, /import \{ isAuthExpiredError \} from '@\/utils\/api-error\.js'/)
  assert.match(source, /isAuthExpiredError\(error\)/)
  assert.match(source, /资料刷新失败，请稍后重试/)

  // onShow 的 catch 必须是「先判断再退出」，而不是无条件 clearLoginState()。
  assert.match(
    source,
    /catch \(error\) \{[\s\S]*?if \(isAuthExpiredError\(error\)\) \{[\s\S]*?userStore\.clearLoginState\(\)[\s\S]*?\}[\s\S]*?else \{[\s\S]*?资料刷新失败，请稍后重试[\s\S]*?\}/
  )
  assert.doesNotMatch(source, /catch\s*\{\s*userStore\.clearLoginState\(\)\s*\}/)
})
