<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserDetail, getUserPointConsumptions, getUsers, grantUserPoints, updateUserStatus } from '@/api/admin-users'

const filters = reactive({ keyword: '', status: undefined })
const table = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const consumptionVisible = ref(false)
const consumptionUser = ref(null)
const consumptionRecords = ref([])
const consumptionTotal = ref(0)
const consumptionPage = ref(1)
const consumptionPageSize = ref(10)
const consumptionLoading = ref(false)

async function loadUsers(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const data = await getUsers({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
    })
    table.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  try {
    detail.value = await getUserDetail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '用户详情加载失败')
  }
}

async function changeStatus(row) {
  const targetStatus = row.status === 1 ? 0 : 1
  const action = targetStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${action}用户“${row.username}”吗？`, `${action}用户`, { type: 'warning' })
    const updated = await updateUserStatus(row.id, targetStatus)
    Object.assign(row, updated)
    ElMessage.success(`${action}成功`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

async function grantPoints(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入 1 到 10000 的积分。', `给“${row.username}”发放积分`, { inputPattern: /^[1-9]\d{0,3}$/, inputErrorMessage: '请输入 1 到 10000 的整数' })
    const { value: reason } = await ElMessageBox.prompt('请填写本次发放原因。', '发放原因', { inputPattern: /\S+/, inputErrorMessage: '发放原因不能为空' })
    row.pointsBalance = await grantUserPoints(row.id, { amount: Number(value), reason })
    ElMessage.success('积分已发放')
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '积分发放失败') }
}

async function loadConsumptions() {
  if (!consumptionUser.value) return
  consumptionLoading.value = true
  try {
    const data = await getUserPointConsumptions(consumptionUser.value.id, {
      page: consumptionPage.value,
      pageSize: consumptionPageSize.value,
    })
    consumptionRecords.value = data.records
    consumptionTotal.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '积分消费明细加载失败')
  } finally {
    consumptionLoading.value = false
  }
}

function showConsumptions(row) {
  consumptionUser.value = row
  consumptionPage.value = 1
  consumptionVisible.value = true
  loadConsumptions()
}

onMounted(loadUsers)
</script>

<template>
  <section>
    <h1>用户管理</h1>

    <el-card shadow="never" class="filters">
      <el-input v-model="filters.keyword" clearable placeholder="关键词、手机号或邮箱" @keyup.enter="loadUsers(true)" />
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadUsers(true)">查询</el-button>
    </el-card>

    <el-table v-loading="loading" :data="table" stripe>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="190" />
      <el-table-column prop="city" label="城市" min-width="100" />
      <el-table-column prop="pointsBalance" label="积分" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="170" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          <el-button link type="primary" @click="showConsumptions(row)">消费明细</el-button>
          <el-button link type="warning" @click="grantPoints(row)">发放积分</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="changeStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @current-change="() => loadUsers()"
      @size-change="() => loadUsers(true)"
    />

    <el-drawer v-model="detailVisible" title="用户详情" size="520px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
          <el-descriptions-item label="城市">{{ detail.city }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">{{ detail.status === 1 ? '启用' : '停用' }}</el-descriptions-item>
          <el-descriptions-item label="问卷状态">{{ detail.questionnaireCompleted ? '已完成' : '未完成' }}</el-descriptions-item>
          <el-descriptions-item label="当前积分">{{ detail.pointsBalance }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detail.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <h3>最近品茶记录</h3>
        <el-empty v-if="!detail.recentTeaRecords.length" description="暂无记录" />
        <el-table v-else :data="detail.recentTeaRecords">
          <el-table-column prop="teaName" label="茶名" />
          <el-table-column prop="category" label="茶类" />
          <el-table-column prop="rating" label="评分" />
          <el-table-column prop="createdAt" label="时间" min-width="160" />
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="consumptionVisible" :title="`${consumptionUser?.username || ''}的积分消费明细`" width="760px">
      <el-table v-loading="consumptionLoading" :data="consumptionRecords" empty-text="暂无积分消费记录">
        <el-table-column prop="description" label="消费项目" min-width="180" />
        <el-table-column label="扣除积分" width="110">
          <template #default="{ row }">{{ row.changeAmount }} 积分</template>
        </el-table-column>
        <el-table-column label="消费后余额" width="120">
          <template #default="{ row }">{{ row.balanceAfter }} 积分</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="消费时间" min-width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="consumptionPage"
        v-model:page-size="consumptionPageSize"
        :total="consumptionTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="loadConsumptions"
        @size-change="() => { consumptionPage = 1; loadConsumptions() }"
      />
    </el-dialog>
  </section>
</template>

<style scoped>
h1 { margin: 0 0 20px; color: #213a31; font-size: 24px; }
.filters { display: flex; gap: 12px; margin-bottom: 16px; }
.filters .el-input { width: 280px; }
.filters .el-select { width: 140px; }
.pager { justify-content: flex-end; margin-top: 16px; }
h3 { margin-top: 24px; color: #213a31; }
@media (max-width: 760px) { .filters { align-items: stretch; flex-direction: column; }.filters .el-input, .filters .el-select { width: 100%; } }
</style>
