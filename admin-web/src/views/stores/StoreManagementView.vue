<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createStore,
  deleteStore,
  getStoreDetail,
  getStorePage,
  updateStore,
  updateStorePromotion,
  updateStoreStatus,
  uploadStoreImage,
} from '@/api/admin-stores'

const teaCategories = ['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草']
const filters = reactive({ keyword: '', status: undefined, promotionEnabled: undefined })
const table = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const saving = ref(false)
const storeDialogVisible = ref(false)
const promotionDialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const editingId = ref(null)
const promotionStore = ref(null)
const formRef = ref()
const foodInput = ref('')

const storeForm = reactive(createEmptyStore())
const promotionForm = reactive({ promotionEnabled: 0, promotionScore: 0, promotionPeriod: [] })

const storeRules = {
  storeName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  teaCategories: [{ type: 'array', required: true, min: 1, message: '请至少选择一种茶类', trigger: 'change' }],
  foodTags: [{ type: 'array', required: true, min: 1, message: '请至少添加一个食品标签', trigger: 'change' }],
  description: [{ required: true, message: '请输入店铺说明', trigger: 'blur' }],
}

function createEmptyStore() {
  return {
    storeName: '', coverImage: '', address: '', longitude: null, latitude: null,
    phone: '', teaCategories: [], foodTags: [], description: '',
  }
}

async function loadStores(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const data = await getStorePage({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
      promotionEnabled: filters.promotionEnabled,
    })
    table.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '店铺列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: undefined, promotionEnabled: undefined })
  loadStores(true)
}

function resetStoreForm() {
  Object.assign(storeForm, createEmptyStore())
  foodInput.value = ''
  formRef.value?.clearValidate()
}

async function openStoreEditor(row) {
  resetStoreForm()
  editingId.value = row?.id ?? null
  if (row) {
    try {
      const store = await getStoreDetail(row.id)
      Object.assign(storeForm, { ...store, phone: store.phone ?? '' })
    } catch (error) {
      ElMessage.error(error.message || '店铺详情加载失败')
      return
    }
  }
  storeDialogVisible.value = true
}

function addFoodTag() {
  const tag = foodInput.value.trim()
  if (!tag) return
  if (storeForm.foodTags.includes(tag)) return ElMessage.warning('该食品标签已添加')
  if (storeForm.foodTags.length >= 10) return ElMessage.warning('食品标签最多10个')
  storeForm.foodTags.push(tag)
  foodInput.value = ''
  formRef.value?.validateField('foodTags')
}

function removeFoodTag(tag) {
  storeForm.foodTags = storeForm.foodTags.filter((item) => item !== tag)
  formRef.value?.validateField('foodTags')
}

function beforeCoverUpload(file) {
  const supported = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!supported) {
    ElMessage.error('只能上传 JPG、PNG 或 WebP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('封面图片不能超过5MB')
    return false
  }
  return true
}

async function uploadCover(option) {
  try {
    storeForm.coverImage = await uploadStoreImage(option.file)
    option.onSuccess(storeForm.coverImage)
    ElMessage.success('封面上传成功')
  } catch (error) {
    option.onError(error)
    ElMessage.error(error.message || '封面上传失败')
  }
}

function removeCover() {
  storeForm.coverImage = ''
}

function normalizeStorePayload() {
  const longitude = storeForm.longitude === '' || storeForm.longitude === null ? null : Number(storeForm.longitude)
  const latitude = storeForm.latitude === '' || storeForm.latitude === null ? null : Number(storeForm.latitude)
  return {
    ...storeForm,
    storeName: storeForm.storeName.trim(),
    coverImage: storeForm.coverImage || null,
    address: storeForm.address.trim(),
    phone: String(storeForm.phone ?? '').trim() || null,
    longitude,
    latitude,
    teaCategories: [...storeForm.teaCategories],
    foodTags: storeForm.foodTags.map((tag) => tag.trim()),
    description: storeForm.description.trim(),
  }
}

async function saveStore() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if ((storeForm.longitude === null) !== (storeForm.latitude === null)) {
    ElMessage.warning('经度和纬度必须同时填写')
    return
  }
  saving.value = true
  try {
    const payload = normalizeStorePayload()
    if (editingId.value === null) await createStore(payload)
    else await updateStore(editingId.value, payload)
    ElMessage.success('店铺资料已保存')
    storeDialogVisible.value = false
    await loadStores()
  } catch (error) {
    ElMessage.error(error.message || '店铺保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(row) {
  const status = row.status === 1 ? 0 : 1
  const action = status === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认${action}店铺“${row.storeName}”吗？`, `${action}店铺`, { type: 'warning' })
    await updateStoreStatus(row.id, status)
    row.status = status
    ElMessage.success(`${action}成功`)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || `${action}失败`)
  }
}

async function removeStore(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除店铺“${row.storeName}”吗？删除后无法恢复。`,
      '删除店铺',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteStore(row.id)
    if (table.value.length === 1 && page.value > 1) page.value -= 1
    await loadStores()
    ElMessage.success('店铺已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除店铺失败')
  }
}

async function showDetail(id) {
  try {
    detail.value = await getStoreDetail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '店铺详情加载失败')
  }
}

async function openPromotion(row) {
  try {
    const store = await getStoreDetail(row.id)
    promotionStore.value = store
    Object.assign(promotionForm, {
      promotionEnabled: store.promotionEnabled || 0,
      promotionScore: store.promotionScore || 0,
      promotionPeriod: store.promotionStartAt && store.promotionEndAt
        ? [store.promotionStartAt, store.promotionEndAt]
        : [],
    })
    promotionDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '推广设置加载失败')
  }
}

async function savePromotion() {
  const hasPromotionPeriod = Array.isArray(promotionForm.promotionPeriod)
    && promotionForm.promotionPeriod.length === 2
  if (promotionForm.promotionEnabled === 1 && !hasPromotionPeriod) {
    ElMessage.warning('开启推广时请选择推广开始和结束时间')
    return
  }
  saving.value = true
  try {
    await updateStorePromotion(promotionStore.value.id, {
      promotionEnabled: promotionForm.promotionEnabled,
      promotionScore: promotionForm.promotionEnabled === 1 ? Number(promotionForm.promotionScore) : 0,
      promotionStartAt: promotionForm.promotionEnabled === 1 ? promotionForm.promotionPeriod[0] : null,
      promotionEndAt: promotionForm.promotionEnabled === 1 ? promotionForm.promotionPeriod[1] : null,
    })
    ElMessage.success('推广设置已保存')
    promotionDialogVisible.value = false
    await loadStores()
  } catch (error) {
    ElMessage.error(error.message || '推广设置保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadStores)
</script>

<template>
  <section>
    <div class="page-heading">
      <div><h1>店铺与推广</h1><p>维护店铺资料、封面图片、上架状态及推广有效期。</p></div>
      <el-button type="success" @click="openStoreEditor()">新增店铺</el-button>
    </div>

    <el-card shadow="never" class="filters">
      <el-input v-model="filters.keyword" clearable placeholder="店铺名称或地址" @keyup.enter="loadStores(true)" />
      <el-select v-model="filters.status" clearable placeholder="全部状态"><el-option label="已上架" :value="1" /><el-option label="已下架" :value="0" /></el-select>
      <el-select v-model="filters.promotionEnabled" clearable placeholder="全部推广状态"><el-option label="已开启推广" :value="1" /><el-option label="未开启推广" :value="0" /></el-select>
      <el-button type="primary" @click="loadStores(true)">查询</el-button><el-button @click="resetFilters">重置</el-button>
    </el-card>

    <el-table v-loading="loading" :data="table" stripe>
      <el-table-column label="封面" width="90"><template #default="{ row }"><el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="cover" /><span v-else class="no-cover">未设置</span></template></el-table-column>
      <el-table-column prop="storeName" label="店铺名称" min-width="140" />
      <el-table-column prop="address" label="详细地址" min-width="220" show-overflow-tooltip />
      <el-table-column label="适配茶类" min-width="160"><template #default="{ row }"><el-tag v-for="item in row.teaCategories" :key="item" class="category-tag">{{ item }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag></template></el-table-column>
      <el-table-column label="推广" width="100"><template #default="{ row }"><el-tag :type="row.promotionEnabled === 1 ? 'warning' : 'info'">{{ row.promotionEnabled === 1 ? '已开启' : '未开启' }}</el-tag></template></el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="300" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row.id)">详情</el-button><el-button link type="primary" @click="openStoreEditor(row)">编辑</el-button><el-button link type="warning" @click="openPromotion(row)">推广</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="changeStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button><el-button link type="danger" @click="removeStore(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pager" @current-change="() => loadStores()" @size-change="() => loadStores(true)" />

    <el-dialog v-model="storeDialogVisible" :title="editingId === null ? '新增店铺' : '编辑店铺'" width="720px" destroy-on-close @closed="resetStoreForm">
      <el-form ref="formRef" :model="storeForm" :rules="storeRules" label-width="92px">
        <el-form-item label="店铺名称" prop="storeName"><el-input v-model="storeForm.storeName" maxlength="100" /></el-form-item>
        <el-form-item label="封面图片"><el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp" :before-upload="beforeCoverUpload" :http-request="uploadCover"><el-image v-if="storeForm.coverImage" :src="storeForm.coverImage" fit="cover" class="cover-preview" /><el-button v-else>上传封面</el-button></el-upload><el-button v-if="storeForm.coverImage" link type="warning" @click="removeCover">移除封面</el-button></el-form-item>
        <el-form-item label="详细地址" prop="address"><el-input v-model="storeForm.address" maxlength="300" /></el-form-item>
        <el-form-item label="经纬度"><div class="coordinates"><el-input-number v-model="storeForm.longitude" :precision="6" :min="-180" :max="180" placeholder="经度" /><el-input-number v-model="storeForm.latitude" :precision="6" :min="-90" :max="90" placeholder="纬度" /></div><span class="form-tip">可同时留空；填写时经纬度必须同时填写。</span></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="storeForm.phone" maxlength="20" /></el-form-item>
        <el-form-item label="适配茶类" prop="teaCategories"><el-select v-model="storeForm.teaCategories" multiple placeholder="至少选择一种茶类"><el-option v-for="item in teaCategories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="食品标签" prop="foodTags"><el-input v-model="foodInput" placeholder="输入食品标签后按回车添加" @keyup.enter="addFoodTag"><template #append><el-button @click="addFoodTag">添加</el-button></template></el-input><div class="tag-editor"><el-tag v-for="tag in storeForm.foodTags" :key="tag" closable @close="removeFoodTag(tag)">{{ tag }}</el-tag></div></el-form-item>
        <el-form-item label="店铺说明" prop="description"><el-input v-model="storeForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="storeDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveStore">保存店铺</el-button></template>
    </el-dialog>

    <el-dialog v-model="promotionDialogVisible" title="推广设置" width="520px">
      <el-form :model="promotionForm" label-width="110px"><el-form-item label="启用推广"><el-switch v-model="promotionForm.promotionEnabled" :active-value="1" :inactive-value="0" /></el-form-item><template v-if="promotionForm.promotionEnabled === 1"><el-form-item label="推广分数"><el-input-number v-model="promotionForm.promotionScore" :min="0" :max="30" /></el-form-item><el-form-item label="有效期"><el-date-picker v-model="promotionForm.promotionPeriod" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" /></el-form-item></template></el-form>
      <template #footer><el-button @click="promotionDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="savePromotion">保存推广设置</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="店铺详情" size="560px"><el-descriptions v-if="detail" :column="1" border><el-descriptions-item label="店铺名称">{{ detail.storeName }}</el-descriptions-item><el-descriptions-item label="封面"><el-image v-if="detail.coverImage" :src="detail.coverImage" fit="cover" class="detail-cover" /><span v-else>未设置</span></el-descriptions-item><el-descriptions-item label="详细地址">{{ detail.address }}</el-descriptions-item><el-descriptions-item label="经纬度">{{ detail.longitude ?? '—' }}，{{ detail.latitude ?? '—' }}</el-descriptions-item><el-descriptions-item label="联系电话">{{ detail.phone || '—' }}</el-descriptions-item><el-descriptions-item label="适配茶类">{{ detail.teaCategories.join('、') }}</el-descriptions-item><el-descriptions-item label="食品标签">{{ detail.foodTags.join('、') }}</el-descriptions-item><el-descriptions-item label="店铺说明">{{ detail.description }}</el-descriptions-item><el-descriptions-item label="推广">{{ detail.promotionEnabled === 1 ? `已开启，分数 ${detail.promotionScore}` : '未开启' }}</el-descriptions-item><el-descriptions-item label="推广有效期">{{ detail.promotionStartAt && detail.promotionEndAt ? `${detail.promotionStartAt} 至 ${detail.promotionEndAt}` : '—' }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.status === 1 ? '已上架' : '已下架' }}</el-descriptions-item></el-descriptions></el-drawer>
  </section>
</template>

<style scoped>
h1 { margin: 0; color: #213a31; font-size: 24px; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
.page-heading p { margin: 8px 0 0; color: #83968b; }
.filters { display: flex; gap: 12px; margin-bottom: 16px; }
.filters .el-input { width: 280px; }.filters .el-select { width: 150px; }
.cover { width: 56px; height: 42px; border-radius: 4px; }.no-cover, .form-tip { color: #9aa89f; font-size: 12px; }
.category-tag, .tag-editor .el-tag { margin: 0 6px 6px 0; }.pager { justify-content: flex-end; margin-top: 16px; }
.cover-preview { display: block; width: 132px; height: 88px; border-radius: 6px; }.detail-cover { width: 220px; height: 146px; border-radius: 6px; }
.coordinates { display: flex; gap: 12px; }.tag-editor { margin-top: 10px; }.form-tip { display: block; width: 100%; margin-top: 8px; }
@media (max-width: 760px) { .page-heading, .filters { align-items: stretch; flex-direction: column; }.filters .el-input, .filters .el-select { width: 100%; }.coordinates { flex-direction: column; } }
</style>
