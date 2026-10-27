import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { computed } from 'vue'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

function installUniStub(initial = {}) {
  const storage = new Map(Object.entries(initial))
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key)
  }
  return storage
}

// 先装桩再静态导入：auth-session 的初始值会读一次本地存储。
// 这里用静态 import 而不是 data: URL，因为源码 import 了裸包名 'vue'。
installUniStub()
const { authToken, clearAuthToken, hasAuthToken, setAuthToken } = await import(
  '../src/utils/auth-session.js'
)

test('复现原始缺陷：getter 直接读 uni.getStorageSync 会被缓存成「首读锁存」', () => {
  const store = new Map()
  globalThis.uni = {
    getStorageSync: (key) => store.get(key),
    setStorageSync: (key, value) => store.set(key, value)
  }

  // 旧写法：Pinia 会把 getter 包成 computed，而 getStorageSync 不是响应式的。
  const legacyIsLoggedIn = computed(() => Boolean(uni.getStorageSync('token')))

  assert.equal(legacyIsLoggedIn.value, false, '冷启动时还没有 token')

  // 登录：直接写 storage（旧 login.vue 就是这么做的）
  uni.setStorageSync('token', 'jwt-from-login')

  // 这就是故障现场：storage 里有 token，UI 却仍判定未登录。
  assert.equal(Boolean(uni.getStorageSync('token')), true, 'storage 里确实有 token')
  assert.equal(
    legacyIsLoggedIn.value,
    false,
    '这正是缺陷：写 storage 不会让缓存的 computed 失效'
  )
})

test('修复后：登录/退出会立即反映到 isLoggedIn，不会锁存', () => {
  installUniStub()
  clearAuthToken()

  const isLoggedIn = computed(() => hasAuthToken())
  assert.equal(isLoggedIn.value, false)

  setAuthToken('jwt-account-b')
  assert.equal(isLoggedIn.value, true, '登录后必须立即变为已登录')
  assert.equal(uni.getStorageSync('token'), 'jwt-account-b', '同时要落本地存储')

  clearAuthToken()
  assert.equal(isLoggedIn.value, false, '退出登录后必须立即变为未登录')
  assert.equal(uni.getStorageSync('token'), undefined, '本地存储也要清掉')

  // 换账号登录：同样要立即生效
  setAuthToken('jwt-account-c')
  assert.equal(isLoggedIn.value, true)
})

test('authToken 初始值来自本地存储，保证冷启动就是已登录', async () => {
  const session = await read('src/utils/auth-session.js')

  // 冷启动恢复：模块初始化时就用本地存储里的 token 作为 ref 初值，
  // 这样「已登录状态下重启 App」不会先渲染成未登录。
  assert.match(session, /export const authToken = ref\(readStoredToken\(\)\)/)
  assert.match(session, /function readStoredToken\(\) \{[\s\S]*?uni\.getStorageSync\('token'\)/)
  // 写入/清除必须同时更新响应式 ref 与本地存储
  assert.match(session, /export function setAuthToken\(token\) \{[\s\S]*?authToken\.value = String\(token \|\| ''\)[\s\S]*?uni\.setStorageSync\('token'/)
  assert.match(session, /export function clearAuthToken\(\) \{[\s\S]*?authToken\.value = ''[\s\S]*?uni\.removeStorageSync\('token'\)/)
})

test('user store 的 isLoggedIn 不再直接读 storage，而是走响应式 auth-session', async () => {
  const store = await read('src/store/user.js')

  assert.match(store, /import \{ clearAuthToken, hasAuthToken \} from '@\/utils\/auth-session\.js'/)
  assert.match(store, /isLoggedIn: \(\) => hasAuthToken\(\)/)
  // 绝不能再出现直接读 storage 当登录判断的写法
  assert.doesNotMatch(store, /isLoggedIn:\s*\(\)\s*=>\s*Boolean\(uni\.getStorageSync/)
  // 退出登录必须走统一入口，保证响应式 token 与存储同步
  assert.match(store, /clearLoginState\(\) \{[\s\S]*?clearAuthToken\(\)/)
})

test('登录页通过 setAuthToken 写入登录态，而不是直接写 storage', async () => {
  const login = await read('src/pages/login/login.vue')

  assert.match(login, /import \{ hasAuthToken, setAuthToken \} from '@\/utils\/auth-session\.js'/)
  assert.match(login, /setAuthToken\(auth\?\.token\)/)
  assert.doesNotMatch(login, /uni\.setStorageSync\('token'/)
  // 写入失败时不能谎报登录成功
  assert.match(login, /if \(!hasAuthToken\(\)\) \{[\s\S]*?保存失败/)
})

test('request 层的 token 取用与 401 清理都走 auth-session', async () => {
  const request = await read('src/utils/request.js')

  assert.match(request, /const token = authToken\.value/)
  assert.match(request, /clearAuthToken\(\)/)
  assert.doesNotMatch(request, /uni\.removeStorageSync\('token'\)/)
})

test('五个主页面都通过 userStore.isLoggedIn 判断，修复一处即全部生效', async () => {
  const pages = [
    'src/pages/profile/profile.vue',
    'src/pages/home/home.vue',
    'src/pages/pairing/pairing.vue',
    'src/pages/record/list.vue',
    'src/pages/record/edit.vue'
  ]

  for (const page of pages) {
    const source = await read(page)
    assert.doesNotMatch(
      source,
      /Boolean\(uni\.getStorageSync\('token'\)\)|uni\.getStorageSync\('token'\)/,
      `${page} 不应自己读 storage 判断登录态`
    )
  }
})
