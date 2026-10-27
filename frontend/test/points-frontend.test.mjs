import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = path => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('积分中心路由和接口已提供', async () => {
  const [pages, api, page] = await Promise.all([
    read('src/pages.json'), read('src/api/points.js'), read('src/pages/points/points.vue')
  ])
  assert.match(pages, /pages\/points\/points/)
  assert.match(api, /\/api\/points\/account/)
  assert.match(api, /\/api\/points\/daily-claim/)
  assert.match(page, /每日领取 20 积分/)
  assert.match(page, /dailyClaimed/)
  assert.match(page, /transactionPage\.records/)
  assert.match(page, /已领取/)
})

test('个人页和 AI 页面展示积分入口与扣费规则', async () => {
  const [profile, home, chat] = await Promise.all([
    read('src/pages/profile/profile.vue'), read('src/pages/home/home.vue'), read('src/pages/ai-chat/chat.vue')
  ])
  assert.match(profile, /积分中心/)
  assert.match(home, /今日首次免费|5 积分/)
  assert.match(chat, /每次问茶消耗 10 积分/)
})

test('首页根据后端免费资格切换 AI 泡法价格文字', async () => {
  const source = await read('src/pages/home/home.vue')
  assert.match(source, /brewGuideFreeAvailable/)
  assert.match(source, /今日首次免费.*5 积分/)
  assert.match(source, /getPointAccount/)
})
