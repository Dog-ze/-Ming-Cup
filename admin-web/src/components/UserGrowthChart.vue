<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  points: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const chartElement = ref(null)
let chart = null
let resizeObserver = null

const hasNoNewUsers = computed(() => props.points.length > 0 && props.points.every((point) => point.newUserCount === 0))

function shortDate(date) {
  return typeof date === 'string' ? date.slice(5) : ''
}

function renderChart() {
  if (!chartElement.value) return
  chart ||= echarts.init(chartElement.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (items) => {
        const point = props.points[items[0].dataIndex]
        return `${point.date}<br/>新增用户：${point.newUserCount} 人`
      },
    },
    grid: { top: 28, right: 28, bottom: 28, left: 50 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.points.map((point) => shortDate(point.date)),
      axisLabel: { interval: props.points.length > 30 ? Math.ceil(props.points.length / 8) - 1 : 'auto' },
    },
    yAxis: { type: 'value', min: 0, minInterval: 1, name: '新增用户' },
    series: [{
      name: '新增用户',
      type: 'line',
      smooth: true,
      data: props.points.map((point) => point.newUserCount),
      symbol: props.points.length <= 30 ? 'circle' : 'none',
      lineStyle: { color: '#176244', width: 3 },
      itemStyle: { color: '#176244' },
      areaStyle: { color: 'rgba(23, 98, 68, 0.16)' },
    }],
  }, { notMerge: true })
  chart.resize()
}

watch(() => props.points, async () => {
  await nextTick()
  renderChart()
}, { deep: true })

onMounted(() => {
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div v-loading="loading" class="chart-wrap">
    <div ref="chartElement" class="chart" aria-label="用户增加趋势图" />
    <p v-if="hasNoNewUsers" class="empty-tip">当前范围内暂无新增用户</p>
  </div>
</template>

<style scoped>
.chart-wrap { position: relative; min-height: 320px; }
.chart { width: 100%; height: 320px; }
.empty-tip { position: absolute; top: 50%; left: 50%; margin: 0; color: #83968b; transform: translate(-50%, -50%); }
</style>
