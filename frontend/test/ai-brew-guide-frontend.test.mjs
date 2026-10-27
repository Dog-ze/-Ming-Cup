import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('首页推荐茶可请求 AI 泡法并一次性展示完整结果', async () => {
  const home = await source('../src/pages/home/home.vue')
  const api = await source('../src/api/ai.js')

  assert.match(api, /url: '\/api\/ai\/brew-guides'/)
  assert.match(api, /method: 'POST'/)
  assert.match(home, /import \{ getBrewGuide \} from '@\/api\/ai\.js'/)
  assert.match(home, /@tap="generateBrewGuide\(recommendation\)"/)
  assert.match(home, /async function generateBrewGuide\(recommendation\)/)
  assert.match(home, /const result = await getBrewGuide\(requestData\)/)
  assert.match(home, /v-if="brewGuideVisible"/)
  assert.match(home, /formatBrewGuideText\(brewGuide\.content\)/)
})

test('首页生成 AI 泡法时使用当前位置，并阻止同一茶叶重复提交', async () => {
  const home = await source('../src/pages/home/home.vue')

  assert.match(home, /const brewGuideLoadingTeaId = ref\(null\)/)
  assert.match(home, /:loading="brewGuideLoadingTeaId === recommendation\.teaId"/)
  assert.match(home, /:disabled="brewGuideLoadingTeaId !== null"/)
  assert.match(home, /longitude: locationStore\.longitude/)
  assert.match(home, /latitude: locationStore\.latitude/)
})
