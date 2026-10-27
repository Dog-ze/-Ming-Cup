import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('概览请求传递用户趋势范围', async () => {
  const source = await read('src/api/dashboard.js')

  assert.match(source, /\/admin\/dashboard/)
  assert.match(source, /userTrendDays/)
})

test('概览页显示推广合作店铺并支持三种趋势范围', async () => {
  const source = await read('src/views/dashboard/DashboardView.vue')

  assert.match(source, /推广合作店铺/)
  assert.match(source, /promotionPartnerStoreCount/)
  assert.match(source, /trendOptions\s*=\s*\[7, 30, 90\]/)
  assert.match(source, /UserGrowthChart/)
})

test('用户增长图使用 ECharts，能够响应容器变化并在卸载时释放资源', async () => {
  const source = await read('src/components/UserGrowthChart.vue')

  assert.match(source, /import \* as echarts from 'echarts'/)
  assert.match(source, /ResizeObserver/)
  assert.match(source, /chart\?\.dispose\(\)/)
})
