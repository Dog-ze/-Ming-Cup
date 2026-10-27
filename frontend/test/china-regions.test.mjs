import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(
  new URL('../src/data/chinaRegions.js', import.meta.url),
  'utf8'
)
const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
const { chinaRegions } = await import(moduleUrl)

test('全国省市数据包含 34 个省级地区', () => {
  assert.equal(chinaRegions.length, 34)
})

test('北京市作为直辖市，只有北京市这个可选城市', () => {
  const beijing = chinaRegions.find(region => region.province === '北京市')

  assert.deepEqual(beijing.cities.map(city => city.name), ['北京市'])
})

test('浙江省包含杭州市', () => {
  const zhejiang = chinaRegions.find(region => region.province === '浙江省')

  assert.ok(zhejiang.cities.some(city => city.name === '杭州市'))
})
