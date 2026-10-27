import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('茶点接口只按茶类请求搭配数据', async () => {
  const api = await source('../src/api/pairing.js')

  assert.match(api, /url: '\/api\/pairing'/)
  assert.match(api, /method: 'GET'/)
})

test('茶点页面包含游客引导、茶类选择和免责声明，但不展示数据来源提示', async () => {
  const page = await source('../src/pages/pairing/pairing.vue')

  assert.match(page, /v-if="!isLoggedIn"/)
  assert.match(page, /登录后查看茶食搭配/)
  assert.match(page, /const teaCategories = \['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草'\]/)
  assert.match(page, /getPairing\(\{ teaCategory: teaCategory\.value \}\)/)
  assert.doesNotMatch(page, /const sourceText = computed/)
  assert.doesNotMatch(page, /当前使用本地搭配建议。/)
  assert.doesNotMatch(page, /当前使用通用搭配建议。/)
  assert.doesNotMatch(page, /class="source-tip"/)
  assert.match(page, /本内容仅供日常饮食参考，不构成医疗建议；如有不适请咨询专业人士。/)
  assert.doesNotMatch(page, /useLocationStore/)
  assert.doesNotMatch(page, /weather\?\./)
})

test('登录页的游客入口跳转到真实的首页', async () => {
  const login = await source('../src/pages/login/login.vue')

  assert.match(login, /uni\.switchTab\(\{ url: '\/pages\/home\/home' \}\)/)
  assert.doesNotMatch(login, /\/pages\/index\/index/)
})

test('登录页支持按手机号或邮箱登录，用户名只在注册时填写', async () => {
  const login = await source('../src/pages/login/login.vue')

  assert.match(login, /v-if="!isRegister" class="login-type-switch"/)
  assert.match(login, /手机号登录/)
  assert.match(login, /邮箱登录/)
  assert.match(login, new RegExp('v-if="isRegister" class="form-item">\\s*<text class="form-label">用户名</text>'))
  assert.match(login, /await login\(\{ loginType: loginType\.value, account: form\.account\.trim\(\), password: form\.password \}\)/)
})

test('切换手机号和邮箱登录时清空账号和密码输入', async () => {
  const login = await source('../src/pages/login/login.vue')

  assert.match(login, /@tap="changeLoginType\('phone'\)"/)
  assert.match(login, /@tap="changeLoginType\('email'\)"/)
  assert.match(
    login,
    /function changeLoginType\(type\) \{[\s\S]*?loginType\.value = type[\s\S]*?form\.account = ''[\s\S]*?form\.password = ''[\s\S]*?\}/,
  )
})

test('注册用户名允许中文，长度最多十个字符', async () => {
  const login = await source('../src/pages/login/login.vue')

  assert.match(login, /\^\[\\u4E00-\\u9FFFA-Za-z0-9_\]\{1,10\}\$/)
  assert.match(login, /1～10 个字符，可用中文、字母、数字或下划线/)
})

test('茶点搭配以带图片的双列食物卡片展示', async () => {
  const page = await source('../src/pages/pairing/pairing.vue')

  assert.match(page, /class="food-grid"/)
  assert.match(page, /class="food-card"/)
  assert.match(page, /:src="foodImage\(food\)"/)
  assert.match(page, /function foodImage\(food\)/)
  assert.doesNotMatch(page, /class="food-tags"/)
})

test('点击推荐茶食会跳转并传递茶类和食物名称', async () => {
  const page = await source('../src/pages/pairing/pairing.vue')

  assert.match(page, /@tap="goNearbyStore\(food\.name\)"/)
  assert.match(page, /function goNearbyStore\(food\)/)
  assert.match(page, /pages\/pairing\/nearby\?teaCategory=/)
})

test('附近茶食页可切换本地茶点和网上茶点占位内容', async () => {
  const nearby = await source('../src/pages/pairing/nearby.vue').catch(() => '')
  const pages = await source('../src/pages.json')

  assert.match(nearby, /本地茶点/)
  assert.match(nearby, /网上茶点/)
  assert.match(nearby, /广告位招租/)
  assert.match(nearby, /function changeTab\(tab\)/)
  assert.match(pages, /"path": "pages\/pairing\/nearby"/)
})

test('附近茶食页使用统一店铺列表，并只为推广店铺显示推广标签', async () => {
  const nearby = await source('../src/pages/pairing/nearby.vue')

  assert.match(nearby, /v-for="store in stores"/)
  assert.match(nearby, /v-if="store\.promotion"/)
  assert.match(nearby, />推广</)
  assert.doesNotMatch(nearby, /后台精选店铺/)
})

test('附近茶食页从后端统一店铺接口加载列表，并在定位更新后重新加载', async () => {
  const nearby = await source('../src/pages/pairing/nearby.vue')
  const api = await source('../src/api/stores.js').catch(() => '')

  assert.match(api, /url: '\/api\/pairing\/stores\/nearby'/)
  assert.match(nearby, /import \{ getNearbyStores \} from '@\/api\/stores\.js'/)
  assert.match(nearby, /await getNearbyStores\(/)
  assert.match(nearby, /stores\.value = result\.stores \?\? \[\]/)
  assert.match(nearby, /await loadStores\(!silent\)/)
})

test('附近茶食页在店铺服务不可用时给出明确提示，并保留无店铺提示', async () => {
  const nearby = await source('../src/pages/pairing/nearby.vue')

  assert.match(nearby, /附近店铺暂不可用，请稍后重试/)
  assert.match(nearby, /暂未找到附近店铺/)
})

test('手动更新位置时，店铺加载失败不会再显示更新成功提示', async () => {
  const nearby = await source('../src/pages/pairing/nearby.vue')

  assert.match(nearby, /const storesLoaded = await loadStores\(!silent\)/)
  assert.match(nearby, /if \(!silent && storesLoaded\)/)
})

test('茶点页面开启下拉刷新', async () => {
  const pages = await source('../src/pages.json')

  assert.match(pages, /"path": "pages\/pairing\/pairing"[\s\S]*?"enablePullDownRefresh": true/)
})
