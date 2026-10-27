<template>
  <view class="edit-page">
    <view class="page-header">
      <text class="eyebrow">茶记</text>
      <text class="page-title">{{ isEdit ? '修改这一杯茶' : '记录这一杯茶' }}</text>
    </view>

    <view v-if="loadingDetail" class="loading-card"><text>正在读取茶记…</text></view>
    <view v-else>
      <view class="form-card">
        <text class="section-title">茶叶信息</text>
        <view class="form-item"><text class="form-label">茶名</text><input v-model="form.teaName" class="form-input" maxlength="64" placeholder="例如：西湖龙井" /></view>
        <view class="form-item"><text class="form-label">茶类</text><picker :range="categoryOptions" :value="categoryIndex" @change="changeCategory"><view class="picker-value">{{ form.category || '请选择茶类' }} <text>›</text></view></picker></view>
        <view class="form-item"><text class="form-label">品牌（可选）</text><input v-model="form.brand" class="form-input" maxlength="64" placeholder="例如：狮峰" /></view>
      </view>

      <view class="form-card">
        <text class="section-title">来源与价格</text>
        <view class="form-item"><text class="form-label">获取方式</text><picker :range="acquisitionOptions" :value="acquisitionIndex" @change="changeAcquisition"><view class="picker-value">{{ form.acquisitionMethod || '请选择获取方式' }} <text>›</text></view></picker></view>
        <view class="form-item"><text class="form-label">价格（元，可选）</text><input v-model="form.price" class="form-input" type="digit" placeholder="赠送或未知可不填" /></view>
      </view>

      <view class="form-card">
        <text class="section-title">冲泡与感受</text>
        <view class="form-item"><text class="form-label">水温（℃，可选）</text><input v-model="form.brewTemp" class="form-input" type="number" placeholder="例如：85" /></view>
        <view class="form-item"><text class="form-label">评分</text><view class="rating-row"><text v-for="star in 5" :key="star" class="star" :class="{ selected: star <= form.rating }" @tap="form.rating = star">★</text></view></view>
        <view class="form-item"><text class="form-label">品茶感受（可选）</text><textarea v-model="form.comment" class="form-textarea" maxlength="500" placeholder="写下这杯茶的香气、滋味或此刻心情" /><text class="count-tip">{{ form.comment.length }}/500</text></view>
        <view class="form-item"><text class="form-label">照片（可选，最多 9 张）</text><view class="image-grid"><view v-for="(url, index) in form.images" :key="`${url}-${index}`" class="image-item"><image :src="resolveImageUrl(url)" mode="aspectFill" @tap="previewImage(url)" /><text class="remove-image" @tap.stop="removeImage(index)">×</text></view><view v-if="form.images.length < 9" class="add-image" @tap="chooseImages"><text>{{ uploading ? '上传中…' : '＋' }}</text><text>添加照片</text></view></view></view>
      </view>
    </view>

    <view class="save-area"><button class="save-button" :loading="saving" :disabled="uploading" @tap="submit">{{ uploading ? '照片上传中…' : '保存记录' }}</button></view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createRecord, getRecord, updateRecord, uploadRecordImage } from '@/api/records.js'
import { useUserStore } from '@/store/user.js'
import { resolveImageUrl, toStoredImageUrl } from '@/utils/image-url.js'

const categoryOptions = ['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草']
const acquisitionOptions = ['线上购买', '线下购买', '朋友赠送', '其他']
const userStore = useUserStore()
const recordId = ref(null)
const loadingDetail = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({ teaName: '', category: '', brand: '', acquisitionMethod: '', price: '', brewTemp: '', rating: 5, comment: '', images: [] })
const isEdit = computed(() => recordId.value !== null)
const categoryIndex = computed(() => Math.max(0, categoryOptions.indexOf(form.category)))
const acquisitionIndex = computed(() => Math.max(0, acquisitionOptions.indexOf(form.acquisitionMethod)))

onLoad(async options => {
  if (!userStore.isLoggedIn) { goLogin(); return }
  if (!options.id) return
  const id = Number(options.id)
  if (!Number.isInteger(id) || id <= 0) { uni.showToast({ title: '记录编号不正确', icon: 'none' }); return }
  recordId.value = id
  await loadDetail()
})

async function loadDetail() {
  loadingDetail.value = true
  try {
    const record = await getRecord(recordId.value)
    Object.assign(form, { ...record, price: record.price ?? '', brewTemp: record.brewTemp ?? '', comment: record.comment || '', brand: record.brand || '', images: (record.images || []).map(toStoredImageUrl) })
  } catch {
    setTimeout(() => uni.navigateBack(), 400)
  } finally {
    loadingDetail.value = false
  }
}

function changeCategory(event) { form.category = categoryOptions[Number(event.detail.value)] }
function changeAcquisition(event) { form.acquisitionMethod = acquisitionOptions[Number(event.detail.value)] }

function optionalNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function buildPayload() {
  return { teaName: form.teaName.trim(), category: form.category, brand: form.brand.trim() || null, acquisitionMethod: form.acquisitionMethod, price: optionalNumber(form.price), brewTemp: optionalNumber(form.brewTemp), rating: Number(form.rating), comment: form.comment.trim() || null, images: form.images || [] }
}

async function chooseImages() {
  if (uploading.value || form.images.length >= 9) return
  try {
    const selected = await new Promise((resolve, reject) => {
      uni.chooseImage({
        count: 9 - form.images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: result => resolve(result.tempFilePaths || []),
        fail: reject
      })
    })
    if (!selected.length) return
    uploading.value = true
    for (const filePath of selected) {
      const url = await uploadRecordImage(filePath)
      form.images.push(url)
    }
  } catch {
    // 上传接口会提示失败原因；已上传成功的照片保留在表单中。
  } finally {
    uploading.value = false
  }
}

function previewImage(current) {
  uni.previewImage({ current: resolveImageUrl(current), urls: form.images.map(resolveImageUrl) })
}

function removeImage(index) {
  form.images.splice(index, 1)
}

function validate() {
  const payload = buildPayload()
  if (!payload.teaName || !payload.category || !payload.acquisitionMethod) { uni.showToast({ title: '请填写茶名并选择茶类、获取方式', icon: 'none' }); return false }
  if (!Number.isInteger(payload.rating) || payload.rating < 1 || payload.rating > 5) { uni.showToast({ title: '请选择 1 到 5 星评分', icon: 'none' }); return false }
  if (payload.price !== null && payload.price < 0) { uni.showToast({ title: '价格不能小于 0', icon: 'none' }); return false }
  if (payload.brewTemp !== null && (payload.brewTemp < 0 || payload.brewTemp > 100)) { uni.showToast({ title: '水温应在 0 到 100 之间', icon: 'none' }); return false }
  return true
}

async function submit() {
  if (saving.value || !validate()) return
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) await updateRecord(recordId.value, payload)
    else await createRecord(payload)
    uni.showToast({ title: '记录已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 450)
  } finally {
    saving.value = false
  }
}

function goLogin() { uni.navigateTo({ url: '/pages/login/login?redirect=%2Fpages%2Frecord%2Flist' }) }
</script>

<style scoped lang="scss">
.edit-page { min-height:100vh; box-sizing:border-box; padding:48rpx 32rpx calc(160rpx + env(safe-area-inset-bottom)); background:#faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat; color:#26342d; }.eyebrow,.page-title,.section-title,.form-label,.count-tip { display:block; }.eyebrow { color:#8daa98; font-size:24rpx; letter-spacing:2rpx; }.page-title { margin-top:12rpx; color:#175b43; font-size:42rpx; font-weight:600; }.form-card,.loading-card { box-sizing:border-box; margin-top:28rpx; padding:32rpx 30rpx; border-radius:24rpx; background:#fff; box-shadow:0 12rpx 32rpx rgba(38,52,45,.05); }.loading-card { color:#8daa98; text-align:center; }.section-title { color:#175b43; font-size:30rpx; font-weight:600; }.form-item { margin-top:28rpx; }.form-label { margin-bottom:12rpx; color:#52675a; font-size:25rpx; }.form-input,.picker-value,.form-textarea { box-sizing:border-box; width:100%; border-radius:14rpx; background:#f4f7f3; color:#26342d; font-size:28rpx; }.form-input,.picker-value { height:84rpx; padding:0 24rpx; line-height:84rpx; }.picker-value { display:flex; align-items:center; justify-content:space-between; }.picker-value text { color:#8daa98; font-size:40rpx; }.form-textarea { min-height:180rpx; padding:20rpx 24rpx; line-height:1.55; }.rating-row { display:flex; gap:16rpx; min-height:76rpx; align-items:center; }.star { color:#d8ddd7; font-size:52rpx; }.star.selected { color:#c18c43; }.count-tip { margin-top:10rpx; color:#8daa98; font-size:22rpx; text-align:right; }.image-grid { display:flex; flex-wrap:wrap; gap:16rpx; }.image-item,.add-image { position:relative; box-sizing:border-box; width:calc((100% - 32rpx) / 3); height:190rpx; overflow:hidden; border-radius:14rpx; }.image-item image { width:100%; height:100%; }.remove-image { position:absolute; top:8rpx; right:8rpx; display:flex; align-items:center; justify-content:center; width:40rpx; height:40rpx; border-radius:50%; color:#fff; background:rgba(38,52,45,.72); font-size:32rpx; line-height:1; }.add-image { display:flex; flex-direction:column; align-items:center; justify-content:center; color:#718578; background:#edf2ed; font-size:24rpx; }.add-image text:first-child { color:#175b43; font-size:48rpx; line-height:1; }.save-area { position:fixed; z-index:2; right:0; bottom:0; left:0; padding:20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); background:rgba(250,248,242,.96); }.save-button { border:0; border-radius:14rpx; color:#fff; background:#175b43; font-size:29rpx; }.save-button::after { border:0; }
</style>
