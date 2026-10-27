<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDashboard } from '@/api/dashboard'
import UserGrowthChart from '@/components/UserGrowthChart.vue'

const router = useRouter()
const trendDays = ref(30)
const dashboard = ref(null)
const loading = ref(true)
const changingTrend = ref(false)
const errorMessage = ref('')
const trendOptions = [7, 30, 90]

const stats = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '用户总数', value: dashboard.value.totalUserCount, path: '/users' },
    { label: '启用用户', value: dashboard.value.enabledUserCount, path: '/users' },
    { label: '茶叶条目', value: dashboard.value.teaCount, path: '/tea-knowledge' },
    { label: '推广合作店铺', value: dashboard.value.promotionPartnerStoreCount, path: '/stores' },
    { label: '今日品茶记录', value: dashboard.value.todayTeaRecordCount, path: '/tea-records' },
    { label: '已填问卷', value: dashboard.value.questionnaireCompletedCount, path: '/questionnaires' },
  ]
})

async function loadDashboard({ keepContent = false } = {}) {
  if (!keepContent) loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await getDashboard(trendDays.value)
  } catch (error) {
    errorMessage.value = error.message || '数据暂时不可用，请稍后重试'
    if (keepContent) ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
    changingTrend.value = false
  }
}

async function changeTrendDays(days) {
  if (days === trendDays.value || changingTrend.value) return
  trendDays.value = days
  changingTrend.value = true
  await loadDashboard({ keepContent: true })
}

onMounted(() => loadDashboard())
</script>

<template>
  <section>
    <h1>工作台</h1>
    <p class="sub-title">集中查看后台运营数据。</p>

    <el-alert v-if="errorMessage && dashboard" :title="errorMessage" type="error" show-icon closable class="error-alert" @close="errorMessage = ''" />

    <template v-if="dashboard">
      <div class="stat-grid">
        <el-card v-for="item in stats" :key="item.label" shadow="never" class="stat-card" @click="router.push(item.path)">
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
        </el-card>
      </div>

      <el-card shadow="never" class="growth-card">
        <template #header>
          <div class="growth-header">
            <div>
              <h2>用户增加趋势</h2>
              <p>近 {{ dashboard.userGrowth.days }} 天新增 {{ dashboard.userGrowth.totalNewUsers }} 人</p>
            </div>
            <el-button-group>
              <el-button v-for="days in trendOptions" :key="days" :type="trendDays === days ? 'primary' : 'default'" :loading="changingTrend && trendDays === days" @click="changeTrendDays(days)">近 {{ days }} 天</el-button>
            </el-button-group>
          </div>
        </template>
        <UserGrowthChart :points="dashboard.userGrowth.points" :loading="changingTrend" />
      </el-card>
    </template>

    <el-card v-else-if="loading" shadow="never" class="loading-card"><el-skeleton :rows="8" animated /></el-card>
    <el-result v-else icon="error" title="数据暂时不可用" :sub-title="errorMessage || '请稍后重试'">
      <template #extra><el-button type="primary" @click="loadDashboard">重新加载</el-button></template>
    </el-result>
  </section>
</template>

<style scoped>
h1 { margin: 0; font-size: 24px; color: #213a31; }
.sub-title { margin: 10px 0 24px; color: #83968b; }
.error-alert { margin-bottom: 16px; }
.stat-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.stat-card { cursor: pointer; transition: transform .2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card p { margin: 0; color: #778d80; }
.stat-card strong { display: block; margin-top: 16px; color: #176244; font-size: 30px; }
.growth-card, .loading-card { margin-top: 24px; }
.growth-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.growth-header h2 { margin: 0; font-size: 18px; color: #213a31; }
.growth-header p { margin: 6px 0 0; color: #83968b; font-size: 13px; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.growth-header { align-items: flex-start; flex-direction: column; } }
</style>
