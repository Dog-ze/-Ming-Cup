import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('问卷页面提供五道选择题并允许数字 0 作为答案', async () => {
  const page = await source('../src/pages/questionnaire/questionnaire.vue')

  assert.match(page, /key: 'purpose'/)
  assert.match(page, /key: 'strengthPreference'/)
  assert.match(page, /answers\[currentQuestion(?:\.value)?\.key\] === undefined/)
  assert.match(page, /submitQuestionnaire\(\{ \.\.\.answers \}\)/)
})

test('首页请求推荐接口并展示可解释推荐', async () => {
  const home = await source('../src/pages/home/home.vue')
  const api = await source('../src/api/recommend.js')

  assert.match(api, /url: '\/api\/recommend'/)
  assert.match(home, /import \{ getRecommend \} from '@\/api\/recommend\.js'/)
  assert.match(home, /async function loadRecommend\(\)/)
  assert.match(home, /const result = await getRecommend\(requestData\)/)
  assert.match(home, /recommendation\.reason/)
})

test('首页优先显示推荐接口返回的茶叶图片，并在没有图片时保留茶字图标', async () => {
  const home = await source('../src/pages/home/home.vue')

  assert.match(home, /v-if="recommendation\.imageUrl"/)
  // 后端图片地址必须先拼上当前后端地址（手机里 localhost 指向手机自己）。
  assert.match(home, /:src="resolveImageUrl\(recommendation\.imageUrl\)"/)
  assert.match(home, /v-else class="tea-icon tea-icon-fallback"/)
})

test('城市变化后，旧城市的推荐结果不会覆盖新城市', async () => {
  const home = await source('../src/pages/home/home.vue')

  assert.match(home, /const recommendRequestVersion = ref\(0\)/)
  assert.match(home, /const requestVersion = \+\+recommendRequestVersion\.value/)
  assert.match(home, /if \(requestVersion === recommendRequestVersion\.value\) \{\s*recommendData\.value = result/)
  assert.doesNotMatch(home, /if \(recommendLoading\.value\) return/)
})

test('城市变化后，旧城市的天气结果不会覆盖新城市', async () => {
  const home = await source('../src/pages/home/home.vue')

  assert.match(home, /const weatherRequestVersion = ref\(0\)/)
  assert.match(home, /const requestVersion = \+\+weatherRequestVersion\.value/)
  assert.match(home, /if \(requestVersion === weatherRequestVersion\.value\) \{\s*weather\.value = result/)
  assert.doesNotMatch(home, /if \(loading\.value\) return/)
})

test('个人页可以进入问卷页面', async () => {
  const profile = await source('../src/pages/profile/profile.vue')

  assert.match(profile, /@tap="goQuestionnaire"/)
  assert.match(profile, /function goQuestionnaire\(\)/)
  assert.match(profile, /pages\/questionnaire\/questionnaire/)
})

test('首页游客点击填写问卷时先进入登录页', async () => {
  const home = await source('../src/pages/home/home.vue')

  assert.match(home, /function goQuestionnaire\(\) \{[\s\S]*?if \(!userStore\.isLoggedIn\)/)
  assert.match(home, /url: '\/pages\/login\/login\?redirect=%2Fpages%2Fhome%2Fhome'/)
})

test('接口返回 401/40101/40103 时按登录失效处理，其它失败不清除登录态', async () => {
  const request = await source('../src/utils/request.js')
  const apiError = await source('../src/utils/api-error.js')

  // 登录失效码集中定义在 api-error.js，只有这些码才算「登录失效」。
  assert.match(apiError, /AUTH_ERROR_CODES\s*=\s*\[401,\s*40101,\s*40103\]/)
  assert.match(request, /isAuthExpiredError/)
  // 清登录态统一走 auth-session，保证响应式 token 与本地存储同步
  assert.match(request, /clearAuthToken\(\)/)
  assert.match(request, /import \{ authSessionVersion, authToken, clearAuthToken \} from '@\/utils\/auth-session\.js'/)
  // 网络失败必须带独立标记，不能被当成登录失效。
  assert.match(request, /code: 'NETWORK_ERROR'/)
})
