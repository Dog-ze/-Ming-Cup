import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('茶点页跳转附近茶食时对中文参数做 URL 编码', async () => {
  const source = await read('src/pages/pairing/pairing.vue')

  assert.match(source, /uni\.navigateTo\(\{/)
  assert.match(source, /teaCategory=\$\{encodeURIComponent\(teaCategory\.value\)\}/)
  assert.match(source, /foodName=\$\{encodeURIComponent\(food\)\}/)
})

test('附近茶食页必须再解码一次参数（App/小程序端不会自动解码）', async () => {
  const source = await read('src/pages/pairing/nearby.vue')

  // uni-app 官方说明：H5 会自动解码 onLoad 的 options，App/小程序端不会。
  // 不显式解码就会出现页面显示 %E4%BD%8E... 且后端报「请选择正确的茶类」。
  assert.match(source, /function decodeOption\(value\)/)
  assert.match(source, /decodeURIComponent\(text\)/)
  assert.match(source, /decodeOption\(options\.teaCategory\)/)
  assert.match(source, /decodeOption\(options\.foodName\)/)

  // 不能再直接把未解码的 options 赋给状态。
  assert.doesNotMatch(source, /teaCategory\.value = options\.teaCategory/)
  assert.doesNotMatch(source, /foodName\.value = options\.foodName/)
})

test('茶类参数缺失或非法时回退到默认茶类，而不是让整页不可用', async () => {
  const source = await read('src/pages/pairing/nearby.vue')

  assert.match(source, /function pickTeaCategory\(value\)/)
  assert.match(source, /DEFAULT_TEA_CATEGORY = '绿茶'/)
  assert.match(source, /teaCategories\.includes\(value\)/)
  assert.match(source, /pickTeaCategory\(decodeOption\(options\.teaCategory\)\)/)
})

test('缺少茶类或茶食时明确标记失败，并提示重试而不是「暂未找到」', async () => {
  const source = await read('src/pages/pairing/nearby.vue')

  assert.match(source, /if \(!teaCategory\.value \|\| !foodName\.value\) \{[\s\S]*?storeLoadFailed\.value = true/)
  // 刷新/重试路径仍要用到已解码后的参数。
  assert.match(source, /teaCategory: teaCategory\.value/)
  assert.match(source, /foodName: foodName\.value/)
})
