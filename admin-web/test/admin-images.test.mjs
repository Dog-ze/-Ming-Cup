import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('管理员图片 API 以 multipart 方式上传，并传入图片场景', async () => {
  const source = await read('src/api/admin-images.js')
  assert.match(source, /new FormData\(\)/)
  assert.match(source, /formData\.append\('file', file\)/)
  assert.match(source, /request\.post\('\/admin\/images', formData/)
  assert.match(source, /params: \{ scene \}/)
})

test('茶叶编辑器支持上传、预览和移除封面图', async () => {
  const source = await read('src/views/tea/TeaEditor.vue')
  assert.match(source, /uploadAdminImage\('tea', option\.file\)/)
  assert.match(source, /form\.imageUrl = data\.imageUrl/)
  assert.match(source, /function removeTeaImage\(\)/)
  assert.match(source, /<el-upload/)
  assert.match(source, /茶叶封面/)
})

test('茶食规则编辑器为每种食品保存名称和图片地址', async () => {
  const source = await read('src/views/pairing/PairingRuleView.vue')
  assert.match(source, /form\.foods\.push\(\{ name, imageUrl: '' \}\)/)
  assert.match(source, /uploadAdminImage\('pairing-food', option\.file\)/)
  assert.match(source, /food\.imageUrl = data\.imageUrl/)
  assert.match(source, /food\.name/)
})
