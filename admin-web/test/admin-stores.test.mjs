import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('店铺 API 覆盖分页、详情、保存、状态、推广、删除和封面上传', async () => {
  const source = await read('src/api/admin-stores.js')

  assert.match(source, /request\.get\('\/admin\/pairing-stores', \{ params \}\)/)
  assert.match(source, /request\.get\(`\/admin\/pairing-stores\/\$\{id\}`\)/)
  assert.match(source, /request\.post\('\/admin\/pairing-stores', data\)/)
  assert.match(source, /request\.put\(`\/admin\/pairing-stores\/\$\{id\}`, data\)/)
  assert.match(source, /request\.put\(`\/admin\/pairing-stores\/\$\{id\}\/status`, \{ status \}\)/)
  assert.match(source, /request\.put\(`\/admin\/pairing-stores\/\$\{id\}\/promotion`, data\)/)
  assert.match(source, /request\.delete\(`\/admin\/pairing-stores\/\$\{id\}`\)/)
  assert.match(source, /request\.post\('\/admin\/pairing-stores\/images', formData\)/)
})

test('店铺页面支持筛选、封面上传、上下架、删除和独立推广设置', async () => {
  const source = await read('src/views/stores/StoreManagementView.vue')

  assert.match(source, /getStorePage/)
  assert.match(source, /uploadStoreImage/)
  assert.match(source, /updateStoreStatus/)
  assert.match(source, /deleteStore/)
  assert.match(source, /确认删除店铺/)
  assert.match(source, /updateStorePromotion/)
  assert.match(source, /el-date-picker/)
  assert.match(source, /ElMessageBox\.confirm/)
  assert.match(source, /el-pagination/)
})

test('店铺路由使用真实页面而不是占位页', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path: '\/stores'/)
  assert.match(source, /StoreManagementView\.vue/)
  assert.doesNotMatch(source, /placeholder\('\/stores', '店铺与推广'\)/)
})
