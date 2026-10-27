import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('品茶记录接口覆盖新增、分页、详情、修改和删除', async () => {
  const api = await source('../src/api/records.js')

  assert.match(api, /url: '\/api\/records'/)
  assert.match(api, /url: `\/api\/records\/\$\{id\}`/)
  assert.match(api, /method: 'POST'/)
  assert.match(api, /method: 'PUT'/)
  assert.match(api, /method: 'DELETE'/)
})

test('茶记列表为游客提供登录引导，并支持刷新、分页和删除确认', async () => {
  const page = await source('../src/pages/record/list.vue')

  assert.match(page, /v-if="!isLoggedIn"/)
  assert.match(page, /登录后，记录每一盏茶的味道与心情/)
  assert.match(page, /onPullDownRefresh/)
  assert.match(page, /onReachBottom/)
  assert.match(page, /listRecords\(1, PAGE_SIZE\)/)
  assert.match(page, /uni\.showModal/)
  assert.match(page, /deleteRecord\(id\)/)
})

test('品茶记录编辑页包含固定选项、数字转换和新增修改提交', async () => {
  const page = await source('../src/pages/record/edit.vue')

  assert.match(page, /const categoryOptions = \['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草'\]/)
  assert.match(page, /const acquisitionOptions = \['线上购买', '线下购买', '朋友赠送', '其他'\]/)
  assert.match(page, /function optionalNumber\(value\)/)
  assert.match(page, /createRecord\(payload\)/)
  assert.match(page, /updateRecord\(recordId\.value, payload\)/)
  assert.doesNotMatch(page, /brewTime/)
})

test('品茶记录编辑页支持选择、上传、预览和删除照片', async () => {
  const page = await source('../src/pages/record/edit.vue')
  const api = await source('../src/api/records.js')

  assert.match(api, /export function uploadRecordImage/)
  assert.match(page, /uni\.chooseImage/)
  assert.match(page, /uploadRecordImage\(filePath\)/)
  assert.match(page, /uni\.previewImage/)
  assert.match(page, /removeImage\(index\)/)
  assert.match(page, /最多 9 张/)
  assert.match(page, /:disabled="uploading"/)
})
