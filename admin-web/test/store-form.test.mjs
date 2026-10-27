import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function getPayloadNormalizer(storeForm) {
  const source = await readFile(
    new URL('../src/views/stores/StoreManagementView.vue', import.meta.url),
    'utf8',
  )
  const start = source.indexOf('function normalizeStorePayload() {')
  const end = source.indexOf('\n}\n\nasync function saveStore', start) + 2

  assert.notEqual(start, -1, '未找到店铺保存数据处理函数')
  assert.notEqual(end, 1, '无法确定店铺保存数据处理函数的结束位置')

  return new Function('storeForm', `${source.slice(start, end)}\nreturn normalizeStorePayload`)(storeForm)
}

test('联系电话为空时仍可生成店铺保存数据', async () => {
  const normalizeStorePayload = await getPayloadNormalizer({
    storeName: '鲜果时光(新华北路店)',
    coverImage: '',
    address: '丰泽区西湖街与新华北路交叉口西南80米',
    longitude: null,
    latitude: null,
    phone: null,
    teaCategories: ['绿茶'],
    foodTags: ['水果'],
    description: '水果店铺',
  })

  const payload = normalizeStorePayload()

  assert.equal(payload.phone, null)
})
