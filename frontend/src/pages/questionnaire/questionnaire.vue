<template>
  <view class="page">
    <text class="eyebrow">饮茶偏好</text>
    <text class="title">用 5 个选择，找到更适合你的茶</text>
    <text class="progress">第 {{ questionIndex + 1 }} / {{ questions.length }} 题</text>
    <view class="card">
      <text class="question">{{ currentQuestion.title }}</text>
      <view v-for="option in currentQuestion.options" :key="option.value" class="option"
        :class="{ selected: answers[currentQuestion.key] === option.value }"
        @tap="answers[currentQuestion.key] = option.value">
        <text>{{ option.label }}</text><text v-if="answers[currentQuestion.key] === option.value">✓</text>
      </view>
    </view>
    <view class="actions">
      <button class="secondary" :disabled="questionIndex === 0" @tap="previousQuestion">上一题</button>
      <button class="primary" :loading="submitting" @tap="nextQuestion">{{ isLastQuestion ? '完成' : '下一题' }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getQuestionnaire, submitQuestionnaire } from '@/api/questionnaire.js'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()
const questionIndex = ref(0)
const submitting = ref(false)
const questions = [
  { key: 'purpose', title: '你喝茶主要是为了什么？', options: [{ value: 0, label: '解腻' }, { value: 1, label: '提神' }, { value: 2, label: '暖胃' }, { value: 3, label: '降火' }, { value: 4, label: '放松' }, { value: 5, label: '随便' }] },
  { key: 'constitutionType', title: '你平时更容易出现哪种感受？', options: [{ value: 0, label: '怕冷' }, { value: 1, label: '怕热' }, { value: 2, label: '都还好' }] },
  { key: 'insomnia', title: '你是否容易因饮茶影响睡眠？', options: [{ value: 0, label: '容易' }, { value: 1, label: '不容易' }, { value: 2, label: '不确定' }] },
  { key: 'flavorPreference', title: '你喜欢哪种口感？', options: [{ value: 0, label: '清淡鲜爽' }, { value: 1, label: '花香果香' }, { value: 2, label: '醇厚回甘' }, { value: 3, label: '陈香厚重' }, { value: 4, label: '甜润温和' }] },
  { key: 'strengthPreference', title: '你偏好茶汤浓度？', options: [{ value: 0, label: '清淡' }, { value: 1, label: '适中' }, { value: 2, label: '浓厚' }] }
]
const answers = reactive({ purpose: undefined, constitutionType: undefined, insomnia: undefined, flavorPreference: undefined, strengthPreference: undefined })
const currentQuestion = computed(() => questions[questionIndex.value])
const isLastQuestion = computed(() => questionIndex.value === questions.length - 1)

onMounted(async () => {
  if (!userStore.isLoggedIn) { uni.showToast({ title: '请先登录后填写问卷', icon: 'none' }); setTimeout(() => uni.navigateBack(), 400); return }
  try { const saved = await getQuestionnaire(); if (saved) Object.assign(answers, saved) } catch { /* 保留空问卷让用户填写 */ }
})
function previousQuestion() { if (questionIndex.value > 0) questionIndex.value -= 1 }
async function nextQuestion() {
  if (answers[currentQuestion.value.key] === undefined) { uni.showToast({ title: '请选择一个答案', icon: 'none' }); return }
  if (!isLastQuestion.value) { questionIndex.value += 1; return }
  submitting.value = true
  try { await submitQuestionnaire({ ...answers }); await userStore.refreshProfile(); uni.showToast({ title: '偏好已保存', icon: 'success' }); setTimeout(() => uni.navigateBack(), 500) } finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.page { min-height: 100vh; box-sizing: border-box; padding: 48rpx 32rpx; background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat; color: #26342d; }
.eyebrow,.title,.progress,.question { display:block; }.eyebrow,.progress { color:#8daa98; font-size:25rpx; }.title { margin-top:14rpx; color:#175b43; font-size:42rpx; font-weight:600; }.progress { margin-top:20rpx; }
.card { margin-top:42rpx; padding:34rpx 30rpx; border-radius:24rpx; background:#fff; box-shadow:0 12rpx 32rpx rgba(38,52,45,.05); }.question { font-size:34rpx; font-weight:600; }
.option { display:flex; justify-content:space-between; align-items:center; min-height:90rpx; padding:0 24rpx; margin-top:18rpx; border:2rpx solid #edf0eb; border-radius:16rpx; color:#52675a; font-size:28rpx; }.option.selected { border-color:#175b43; color:#175b43; background:#e6f1e8; font-weight:600; }
.actions { display:flex; gap:20rpx; margin-top:36rpx; }.secondary,.primary { flex:1; border:0; border-radius:14rpx; font-size:28rpx; }.secondary { color:#52675a; background:#edf2ed; }.primary { color:#fff; background:#175b43; }.secondary::after,.primary::after { border:0; }
</style>
