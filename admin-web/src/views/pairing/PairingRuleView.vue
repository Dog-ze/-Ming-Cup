<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadAdminImage } from '@/api/admin-images'
import { createPairingRule, deletePairingRule, getPairingRule, getPairingRulePage, updatePairingRule } from '@/api/admin-pairing-rules'
import { resolveAdminImageUrl } from '@/utils/image-url'

const categories = ['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草']
const filters = reactive({ teaCategory: '' })
const table = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const foodInput = ref('')
const formRef = ref()
const form = reactive({ teaCategory: '', foods: [], advice: '' })
const rules = {
  teaCategory: [{ required: true, message: '请选择茶类', trigger: 'change' }],
  foods: [{ type: 'array', required: true, min: 1, message: '请至少添加一种食品', trigger: 'change' }],
  advice: [{ required: true, message: '请输入搭配建议', trigger: 'blur' }],
}

async function loadRules(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const data = await getPairingRulePage({
      page: page.value,
      pageSize: pageSize.value,
      teaCategory: filters.teaCategory || undefined,
    })
    table.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '规则列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { teaCategory: '', foods: [], advice: '' })
  foodInput.value = ''
  formRef.value?.clearValidate()
}

function normalizeFoods(foods) {
  if (!Array.isArray(foods)) return []
  return foods.map((food) => {
    if (typeof food === 'string') return { name: food, imageUrl: '' }
    return { name: food.name || '', imageUrl: food.imageUrl || '' }
  }).filter((food) => food.name)
}

async function openEditor(row) {
  resetForm()
  editingId.value = row?.id ?? null
  if (row) {
    try {
      const detail = await getPairingRule(row.id)
      Object.assign(form, { ...detail, foods: normalizeFoods(detail.foods) })
    } catch (error) {
      ElMessage.error(error.message || '规则详情加载失败')
      return
    }
  }
  dialogVisible.value = true
}

function addFood() {
  const name = foodInput.value.trim()
  if (!name) return
  if (form.foods.some((food) => food.name === name)) return ElMessage.warning('该食品已添加')
  if (form.foods.length >= 10) return ElMessage.warning('最多添加10种食品')
  form.foods.push({ name, imageUrl: '' })
  foodInput.value = ''
  formRef.value?.validateField('foods')
}

function removeFood(name) {
  form.foods = form.foods.filter((food) => food.name !== name)
  formRef.value?.validateField('foods')
}

async function uploadFoodImage(option, food) {
  try {
    const data = await uploadAdminImage('pairing-food', option.file)
    food.imageUrl = data.imageUrl
    option.onSuccess(data)
    ElMessage.success(`${food.name}图片上传成功`)
  } catch (error) {
    option.onError(error)
    ElMessage.error(error.message || '食品图片上传失败')
  }
}

function removeFoodImage(food) {
  food.imageUrl = ''
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value === null) await createPairingRule(form)
    else await updatePairingRule(editingId.value, form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadRules()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeRule(row) {
  try {
    await ElMessageBox.confirm(
      `删除“${row.teaCategory}”的搭配规则后，用户端会显示通用建议。确认删除吗？`,
      '删除规则',
      { type: 'warning' },
    )
    await deletePairingRule(row.id)
    ElMessage.success('删除成功')
    await loadRules()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败')
  }
}

function resetFilters() {
  filters.teaCategory = ''
  loadRules(true)
}

onMounted(loadRules)
</script>

<template>
  <section>
    <div class="heading">
      <div><h1>茶食搭配规则</h1><p>维护用户端按茶类展示的日常饮食建议。</p></div>
      <el-button type="success" @click="openEditor()">新增规则</el-button>
    </div>

    <el-card shadow="never" class="filters">
      <el-select v-model="filters.teaCategory" clearable placeholder="全部茶类">
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="loadRules(true)">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </el-card>

    <el-table v-loading="loading" :data="table" stripe>
      <el-table-column prop="teaCategory" label="茶类" width="100" />
      <el-table-column label="推荐食品" min-width="220">
        <template #default="{ row }"><el-tag v-for="food in row.foods" :key="food.name || food" class="food-tag">{{ food.name || food }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="advice" label="搭配建议" min-width="300" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="openEditor(row)">编辑</el-button><el-button link type="danger" @click="removeRule(row)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pager" @current-change="() => loadRules()" @size-change="() => loadRules(true)" />

    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新增规则' : '编辑规则'" width="620px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="茶类" prop="teaCategory"><el-select v-model="form.teaCategory" :disabled="editingId !== null" placeholder="请选择茶类"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="推荐食品" prop="foods">
          <el-input v-model="foodInput" placeholder="输入食品名称后按回车添加">
            <template #append><el-button @click="addFood">添加</el-button></template>
          </el-input>
          <div class="food-editor-list">
            <div v-for="food in form.foods" :key="food.name" class="food-editor-item">
              <div class="food-editor-name">{{ food.name }}</div>
              <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp" :http-request="(option) => uploadFoodImage(option, food)">
                <img v-if="food.imageUrl" :src="resolveAdminImageUrl(food.imageUrl)" class="food-image-preview" :alt="food.name" />
                <el-button v-else size="small">上传图片</el-button>
              </el-upload>
              <el-button v-if="food.imageUrl" link type="warning" @click="removeFoodImage(food)">移除图片</el-button>
              <el-button link type="danger" @click="removeFood(food.name)">删除食品</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="搭配建议" prop="advice"><el-input v-model="form.advice" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
h1 { margin: 0; color: #213a31; font-size: 24px; }
.heading { display: flex; justify-content: space-between; margin-bottom: 20px; }
.heading p { color: #83968b; margin: 8px 0 0; }
.filters { display: flex; gap: 12px; margin-bottom: 16px; }
.filters .el-select { width: 160px; }
.food-tag { margin: 0 6px 6px 0; }
.food-editor-list { width: 100%; margin-top: 10px; }
.food-editor-item { display: flex; align-items: center; gap: 12px; min-height: 48px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.food-editor-name { flex: 1; color: #303133; }
.food-image-preview { display: block; width: 72px; height: 48px; border-radius: 4px; object-fit: cover; }
.pager { justify-content: flex-end; margin-top: 16px; }
</style>
