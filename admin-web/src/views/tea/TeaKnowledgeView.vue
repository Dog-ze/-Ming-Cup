<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteTea, getTeaCategories, getTeaDetail, getTeaPage, updateTeaStatus } from '@/api/admin-teas'
import TeaEditor from './TeaEditor.vue'

const filters = reactive({ keyword: '', category: '', status: undefined })
const table = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const editorVisible = ref(false)
const editingId = ref(null)

async function loadTeas(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const data = await getTeaPage({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      category: filters.category || undefined,
      status: filters.status,
    })
    table.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '茶叶列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getTeaCategories()
  } catch (error) {
    ElMessage.error(error.message || '茶叶分类加载失败')
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', category: '', status: undefined })
  loadTeas(true)
}

async function showDetail(id) {
  try {
    detail.value = await getTeaDetail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '茶叶详情加载失败')
  }
}

function createTea() {
  editingId.value = null
  editorVisible.value = true
}

function editTea(row) {
  editingId.value = row.id
  editorVisible.value = true
}

async function changeStatus(row) {
  const nextStatus = row.enabled === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${action}茶叶“${row.name}”吗？`, `${action}茶叶`, { type: 'warning' })
    await updateTeaStatus(row.id, nextStatus)
    row.enabled = nextStatus
    ElMessage.success(`${action}成功`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || `${action}失败`)
  }
}

async function removeTea(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除茶叶“${row.name}”吗？删除后无法恢复。`,
      '删除茶叶',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteTea(row.id)
    if (table.value.length === 1 && page.value > 1) page.value -= 1
    await Promise.all([loadTeas(), loadCategories()])
    ElMessage.success('茶叶已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除茶叶失败')
  }
}

async function afterSaved() {
  await Promise.all([loadTeas(), loadCategories()])
}

onMounted(() => Promise.all([loadTeas(), loadCategories()]))
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>茶叶知识库</h1><p>维护 APP 中展示的茶叶知识内容。</p></div><el-button type="success" @click="createTea">新增茶叶</el-button></div>

    <el-card shadow="never" class="filters">
      <el-input v-model="filters.keyword" clearable placeholder="名称、分类或产地" @keyup.enter="loadTeas(true)" />
      <el-select v-model="filters.category" clearable placeholder="全部分类">
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="已上架" :value="1" /><el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadTeas(true)">查询</el-button><el-button @click="resetFilters">重置</el-button>
    </el-card>

    <el-table v-loading="loading" :data="table" stripe>
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="category" label="分类" min-width="100" />
      <el-table-column prop="nature" label="茶性" min-width="80" />
      <el-table-column prop="caffeine" label="咖啡因" min-width="90" />
      <el-table-column prop="source" label="来源" min-width="90" />
      <el-table-column label="状态" width="92"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '已启用' : '已停用' }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="240" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row.id)">详情</el-button><el-button link type="primary" @click="editTea(row)">编辑</el-button><el-button link :type="row.enabled === 1 ? 'danger' : 'success'" @click="changeStatus(row)">{{ row.enabled === 1 ? '停用' : '启用' }}</el-button><el-button link type="danger" @click="removeTea(row)">删除</el-button></template></el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pager" @current-change="() => loadTeas()" @size-change="() => loadTeas(true)" />

    <el-drawer v-model="detailVisible" title="茶叶详情" size="560px"><template v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item><el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item><el-descriptions-item label="茶性">{{ detail.nature }}</el-descriptions-item><el-descriptions-item label="咖啡因">{{ detail.caffeine }}</el-descriptions-item><el-descriptions-item label="口感标签">{{ detail.flavorTags }}</el-descriptions-item><el-descriptions-item label="饮用场景">{{ detail.effectTags }}</el-descriptions-item><el-descriptions-item label="推荐浓度">{{ detail.suitableStrength }}</el-descriptions-item><el-descriptions-item label="冲泡水温">{{ detail.brewTemp ? `${detail.brewTemp}℃` : '—' }}</el-descriptions-item><el-descriptions-item label="冲泡时间">{{ detail.brewTime ? `${detail.brewTime}秒` : '—' }}</el-descriptions-item><el-descriptions-item label="来源">{{ detail.source }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.enabled === 1 ? '已启用' : '已停用' }}</el-descriptions-item><el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item></el-descriptions></template></el-drawer>
    <TeaEditor v-model:visible="editorVisible" :tea-id="editingId" @saved="afterSaved" />
  </section>
</template>

<style scoped>
h1 { margin: 0; color: #213a31; font-size: 24px; }.page-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }.page-heading p { margin: 8px 0 0; color: #83968b; }.filters { display: flex; gap: 12px; margin-bottom: 16px; }.filters .el-input { width: 280px; }.filters .el-select { width: 140px; }.tea-image { width: 48px; height: 48px; border-radius: 4px; }.no-image { color: #9aa89f; font-size: 12px; }.pager { justify-content: flex-end; margin-top: 16px; }@media (max-width: 760px) { .page-heading, .filters { align-items: stretch; flex-direction: column; }.filters .el-input, .filters .el-select { width: 100%; } }
</style>
