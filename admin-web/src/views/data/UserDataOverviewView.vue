<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getQuestionnaireStatistics,
  getUserDataPage,
  getUserQuestionnaire,
  getUserTeaRecords,
} from '@/api/admin-user-data'

const filters = reactive({ keyword: '', questionnaireCompleted: undefined })
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const errorMessage = ref('')

const currentUser = ref(null)
const recordDialogVisible = ref(false)
const recordRows = ref([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordPageSize = ref(10)
const recordLoading = ref(false)
const recordFilters = reactive({ keyword: '', dateRange: [] })

const questionnaireDialogVisible = ref(false)
const questionnaireLoading = ref(false)
const questionnaire = ref(null)

const statisticsDialogVisible = ref(false)
const statisticsLoading = ref(false)
const statistics = ref(null)

const statisticGroups = [
  { key: 'purposes', title: '饮茶目的' },
  { key: 'constitutionTypes', title: '体质感受' },
  { key: 'insomniaStatuses', title: '失眠情况' },
  { key: 'flavorPreferences', title: '口味偏好' },
  { key: 'strengthPreferences', title: '浓淡偏好' },
]

function mainParams() {
  return {
    page: page.value,
    pageSize: pageSize.value,
    keyword: filters.keyword.trim() || undefined,
    questionnaireCompleted: filters.questionnaireCompleted,
  }
}

function statisticParams() {
  return {
    keyword: filters.keyword.trim() || undefined,
    questionnaireCompleted: filters.questionnaireCompleted,
  }
}

function recordParams() {
  const [startDate, endDate] = recordFilters.dateRange || []
  return {
    page: recordPage.value,
    pageSize: recordPageSize.value,
    keyword: recordFilters.keyword.trim() || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
  }
}

async function loadUsers(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await getUserDataPage(mainParams())
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    errorMessage.value = error.message || '用户数据加载失败，请稍后重试'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

function resetMainFilters() {
  filters.keyword = ''
  filters.questionnaireCompleted = undefined
  loadUsers(true)
}

async function openTeaRecords(row) {
  currentUser.value = row
  recordFilters.keyword = ''
  recordFilters.dateRange = []
  recordPage.value = 1
  recordDialogVisible.value = true
  await loadTeaRecords()
}

async function loadTeaRecords(resetPage = false) {
  if (!currentUser.value) return
  if (resetPage) recordPage.value = 1
  recordLoading.value = true

  try {
    const data = await getUserTeaRecords(currentUser.value.userId, recordParams())
    recordRows.value = data.list
    recordTotal.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '品茶记录加载失败，请稍后重试')
  } finally {
    recordLoading.value = false
  }
}

function resetRecordFilters() {
  recordFilters.keyword = ''
  recordFilters.dateRange = []
  loadTeaRecords(true)
}

async function openQuestionnaire(row) {
  currentUser.value = row
  questionnaire.value = null
  questionnaireDialogVisible.value = true
  questionnaireLoading.value = true

  try {
    questionnaire.value = await getUserQuestionnaire(row.userId)
  } catch (error) {
    questionnaireDialogVisible.value = false
    ElMessage.error(error.message || '问卷加载失败，请稍后重试')
  } finally {
    questionnaireLoading.value = false
  }
}

async function openStatistics() {
  statisticsDialogVisible.value = true
  statisticsLoading.value = true

  try {
    statistics.value = await getQuestionnaireStatistics(statisticParams())
  } catch (error) {
    statisticsDialogVisible.value = false
    ElMessage.error(error.message || '问卷统计加载失败，请稍后重试')
  } finally {
    statisticsLoading.value = false
  }
}

function stars(rating) {
  const score = Number(rating) || 0
  return '★'.repeat(score) + '☆'.repeat(Math.max(0, 5 - score))
}

function statisticItems(groupKey) {
  return statistics.value?.[groupKey] || []
}

function statisticText(item) {
  const percent = Number(item.percent) || 0
  return `${item.count || 0} 人（${percent.toFixed(2)}%）`
}

onMounted(() => loadUsers())
</script>

<template>
  <section>
    <h1>用户数据</h1>
    <p class="sub-title">按用户汇总查看品茶记录和问卷内容；所有数据只读。</p>

    <el-alert
      v-if="errorMessage && rows.length"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      class="error-alert"
      @close="errorMessage = ''"
    />

    <el-card shadow="never" class="filter-card">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="用户名、手机号或邮箱"
        @keyup.enter="loadUsers(true)"
      />
      <el-select v-model="filters.questionnaireCompleted" clearable placeholder="全部问卷状态">
        <el-option label="已填写问卷" :value="1" />
        <el-option label="未填写问卷" :value="0" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadUsers(true)">查询</el-button>
      <el-button @click="resetMainFilters">重置</el-button>
      <el-button type="success" :loading="statisticsLoading" @click="openStatistics">查看问卷统计</el-button>
    </el-card>

    <el-table v-loading="loading" :data="rows" stripe empty-text="暂无符合条件的用户">
      <el-table-column label="用户名" min-width="180">
        <template #default="{ row }">{{ row.username }}（ID：{{ row.userId }}）</template>
      </el-table-column>
      <el-table-column prop="teaRecordCount" label="品茶记录数" width="140" align="center" />
      <el-table-column label="是否填写问卷" width="150" align="center">
        <template #default="{ row }">
          <el-tag :type="row.questionnaireCompleted ? 'success' : 'info'">
            {{ row.questionnaireCompleted ? '已填写' : '未填写' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openTeaRecords(row)">查看品茶记录</el-button>
          <el-button link type="primary" @click="openQuestionnaire(row)">查看问卷</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @size-change="() => loadUsers(true)"
      @current-change="() => loadUsers()"
    />

    <el-dialog v-model="recordDialogVisible" :title="`${currentUser?.username || ''}的品茶记录`" width="min(1000px, 94vw)">
      <el-card shadow="never" class="dialog-filter-card">
        <el-input
          v-model="recordFilters.keyword"
          clearable
          placeholder="茶名、茶类、品牌或评价"
          @keyup.enter="loadTeaRecords(true)"
        />
        <el-date-picker
          v-model="recordFilters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" :loading="recordLoading" @click="loadTeaRecords(true)">查询</el-button>
        <el-button @click="resetRecordFilters">重置</el-button>
      </el-card>

      <el-table v-loading="recordLoading" :data="recordRows" stripe empty-text="该用户暂无符合条件的品茶记录">
        <el-table-column prop="teaName" label="茶名" min-width="120" />
        <el-table-column prop="category" label="茶类" width="90" />
        <el-table-column label="评分" width="125">
          <template #default="{ row }"><span class="rating">{{ stars(row.rating) }}</span></template>
        </el-table-column>
        <el-table-column prop="comment" label="评价" min-width="180" show-overflow-tooltip />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.images?.[0]"
              :src="row.images[0]"
              :preview-src-list="row.images"
              fit="cover"
              class="record-cover"
            />
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="记录时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="recordPage"
        v-model:page-size="recordPageSize"
        :total="recordTotal"
        layout="total, prev, pager, next"
        class="pager"
        @current-change="() => loadTeaRecords()"
      />
    </el-dialog>

    <el-dialog v-model="questionnaireDialogVisible" :title="`${currentUser?.username || ''}的问卷`" width="560px">
      <el-skeleton v-if="questionnaireLoading" :rows="7" animated />
      <el-empty v-else-if="questionnaire && !questionnaire.completed" description="该用户暂未填写问卷" />
      <el-descriptions v-else-if="questionnaire" :column="1" border>
        <el-descriptions-item label="饮茶目的">{{ questionnaire.purpose }}</el-descriptions-item>
        <el-descriptions-item label="体质感受">{{ questionnaire.constitutionType }}</el-descriptions-item>
        <el-descriptions-item label="失眠情况">{{ questionnaire.insomnia }}</el-descriptions-item>
        <el-descriptions-item label="口味偏好">{{ questionnaire.flavorPreference }}</el-descriptions-item>
        <el-descriptions-item label="浓淡偏好">{{ questionnaire.strengthPreference }}</el-descriptions-item>
        <el-descriptions-item label="最后更新时间">{{ questionnaire.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-drawer v-model="statisticsDialogVisible" title="问卷总体统计" size="680px">
      <el-skeleton v-if="statisticsLoading" :rows="12" animated />
      <template v-else-if="statistics">
        <el-alert
          :title="`当前筛选条件下共有 ${statistics.sampleCount} 份问卷`"
          type="success"
          :closable="false"
          show-icon
        />
        <section v-for="group in statisticGroups" :key="group.key" class="statistics-group">
          <h3>{{ group.title }}</h3>
          <div v-for="item in statisticItems(group.key)" :key="item.code" class="statistic-row">
            <span>{{ item.label }}</span>
            <el-progress :percentage="Number(item.percent) || 0" :stroke-width="12" :format="() => statisticText(item)" />
          </div>
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
h1 { margin: 0; color: #213a31; font-size: 24px; }
.sub-title { margin: 10px 0 20px; color: #83968b; }
.error-alert { margin-bottom: 16px; }
.filter-card, .dialog-filter-card { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.filter-card .el-input, .dialog-filter-card .el-input { width: 240px; }
.filter-card .el-select { width: 160px; }
.pager { justify-content: flex-end; margin-top: 16px; }
.rating { color: #cf8b2e; letter-spacing: 1px; }
.record-cover { width: 42px; height: 42px; border-radius: 6px; }
.statistics-group { margin-top: 28px; }
.statistics-group h3 { margin: 0 0 12px; color: #213a31; }
.statistic-row { display: grid; grid-template-columns: 108px 1fr; gap: 12px; align-items: center; margin: 10px 0; color: #4b6659; }
@media (max-width: 900px) {
  .filter-card .el-input, .dialog-filter-card .el-input { width: 100%; }
  .statistic-row { grid-template-columns: 1fr; gap: 4px; }
}
</style>
