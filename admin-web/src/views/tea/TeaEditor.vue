<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAdminImage } from '@/api/admin-images'
import { createTea, getTeaDetail, updateTea } from '@/api/admin-teas'
import { resolveAdminImageUrl } from '@/utils/image-url'

const props = defineProps({
  visible: Boolean,
  teaId: { type: Number, default: null },
})
const emit = defineEmits(['update:visible', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEditing = computed(() => props.teaId !== null)
const emptyForm = () => ({
  name: '', category: '', nature: '', caffeine: '', flavorTags: '[]',
  effectTags: '[]', suitableStrength: '', brewTemp: null, brewTime: null,
  source: 'local', imageUrl: '', enabled: 1,
})
const form = reactive(emptyForm())
const rules = {
  name: [{ required: true, message: '请输入茶叶名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入茶叶分类', trigger: 'blur' }],
  nature: [{ required: true, message: '请输入茶性', trigger: 'blur' }],
  caffeine: [{ required: true, message: '请输入咖啡因等级', trigger: 'blur' }],
  flavorTags: [{ required: true, message: '请输入口感标签', trigger: 'blur' }],
  effectTags: [{ required: true, message: '请输入饮用场景标签', trigger: 'blur' }],
  suitableStrength: [{ required: true, message: '请选择推荐浓度', trigger: 'change' }],
  source: [{ required: true, message: '请输入数据来源', trigger: 'blur' }],
  enabled: [{ required: true, message: '请选择启用状态', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function initialize() {
  resetForm()
  if (!isEditing.value) return
  loading.value = true
  try {
    Object.assign(form, await getTeaDetail(props.teaId))
  } catch (error) {
    ElMessage.error(error.message || '茶叶详情加载失败')
    emit('update:visible', false)
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (visible) => {
  if (visible) initialize()
  else resetForm()
})

function close() {
  emit('update:visible', false)
}

async function uploadTeaImage(option) {
  try {
    const data = await uploadAdminImage('tea', option.file)
    form.imageUrl = data.imageUrl
    option.onSuccess(data)
    ElMessage.success('茶叶图片上传成功')
  } catch (error) {
    option.onError(error)
    ElMessage.error(error.message || '茶叶图片上传失败')
  }
}

function removeTeaImage() {
  form.imageUrl = ''
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEditing.value) await updateTea(props.teaId, form)
    else await createTea(form)
    ElMessage.success(isEditing.value ? '茶叶已更新' : '茶叶已新增')
    emit('saved')
    close()
  } catch (error) {
    ElMessage.error(error.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="visible" :title="isEditing ? '编辑茶叶' : '新增茶叶'" width="680px" destroy-on-close @close="close">
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="92px">
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="名称" prop="name"><el-input v-model="form.name" maxlength="64" show-word-limit /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="分类" prop="category"><el-input v-model="form.category" placeholder="如：绿茶" maxlength="16" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="茶性" prop="nature"><el-select v-model="form.nature" placeholder="请选择"><el-option label="寒" value="寒" /><el-option label="凉" value="凉" /><el-option label="平" value="平" /><el-option label="温" value="温" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="咖啡因" prop="caffeine"><el-select v-model="form.caffeine" placeholder="请选择"><el-option label="低" value="低" /><el-option label="中" value="中" /><el-option label="高" value="高" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="推荐浓度" prop="suitableStrength"><el-select v-model="form.suitableStrength" placeholder="请选择"><el-option label="清淡" value="清淡" /><el-option label="适中" value="适中" /><el-option label="浓厚" value="浓厚" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="数据来源" prop="source"><el-input v-model="form.source" maxlength="16" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="水温(℃)"><el-input-number v-model="form.brewTemp" :min="1" :max="100" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="时间(秒)"><el-input-number v-model="form.brewTime" :min="1" :max="3600" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="茶叶封面">
        <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp" :http-request="uploadTeaImage">
          <img v-if="form.imageUrl" :src="resolveAdminImageUrl(form.imageUrl)" class="tea-cover-preview" alt="茶叶封面" />
          <el-button v-else type="primary" plain>选择并上传图片</el-button>
        </el-upload>
        <el-button v-if="form.imageUrl" link type="danger" @click="removeTeaImage">移除图片</el-button>
        <div class="upload-tip">支持 JPG、PNG、WebP，单张不超过 5MB。</div>
      </el-form-item>
      <el-form-item label="口感标签" prop="flavorTags"><el-input v-model="form.flavorTags" type="textarea" :rows="2" placeholder='JSON数组，例如：["清淡鲜爽","栗香"]' /></el-form-item>
      <el-form-item label="饮用场景" prop="effectTags"><el-input v-model="form.effectTags" type="textarea" :rows="2" placeholder='JSON数组，例如：["解腻","提神"]' /></el-form-item>
      <el-form-item label="启用状态" prop="enabled"><el-radio-group v-model="form.enabled"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
    </el-form>
    <template #footer><el-button @click="close">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.tea-cover-preview { display: block; width: 160px; height: 100px; border-radius: 6px; object-fit: cover; }
.upload-tip { margin-top: 6px; color: #909399; font-size: 12px; }
</style>
