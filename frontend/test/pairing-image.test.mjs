import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('茶食页面优先显示后台返回的食品图片，缺图时使用默认图', async () => {
  const source = await read('src/pages/pairing/pairing.vue')
  assert.match(source, /v-for="food in pairing\.foods"/)
  assert.match(source, /food\.name/)
  // 后端图片地址必须先经过 resolveImageUrl 拼上当前后端地址，手机才能显示。
  assert.match(source, /resolveImageUrl\(food\.imageUrl\)/)
  assert.match(source, /foodImages\[food\.name\] \|\| lightPastryImage/)
})

test('茶食页面兼容后端尚未升级时返回的食品文字数组', async () => {
  const source = await read('src/pages/pairing/pairing.vue')
  assert.match(source, /function normalizeFoods\(foods\)/)
  assert.match(source, /typeof food === 'string'/)
  assert.match(source, /foods: normalizeFoods\(data\.foods\)/)
})
