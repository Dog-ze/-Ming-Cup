<template>
  <view v-if="visible" class="picker-mask" @tap.self="close">
    <view class="picker-panel">
      <view class="picker-header">
        <text class="picker-title">选择城市</text>
        <text class="picker-close" @tap="close">×</text>
      </view>

      <view class="picker-content">
        <scroll-view class="province-list" scroll-y>
          <view
            v-for="(region, index) in chinaRegions"
            :key="region.code"
            class="province-item"
            :class="{ active: index === provinceIndex }"
            @tap="selectProvince(index)"
          >
            {{ region.province }}
          </view>
        </scroll-view>

        <scroll-view class="city-list" scroll-y>
          <view
            v-for="city in currentCities"
            :key="city.code"
            class="city-item"
            :class="{ active: city.code === selectedCity?.code }"
            @tap="selectedCity = city"
          >
            {{ city.name }}
          </view>
        </scroll-view>
      </view>

      <view class="picker-actions">
        <button class="cancel-button" @tap="close">取消</button>
        <button class="confirm-button" @tap="confirm">确定</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { chinaRegions } from '@/data/chinaRegions.js'
import { setTabBarVisibility } from '@/utils/tab-bar.js'
import { useLocationStore } from '@/store/location.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  currentCity: {
    type: String,
    default: ''
  },
  // 个人资料页传入正在编辑的表单；确认城市时直接修改这份表单。
  targetForm: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible'])
const locationStore = useLocationStore()
const provinceIndex = ref(0)
const selectedCity = ref(null)

const currentCities = computed(() => chinaRegions[provinceIndex.value]?.cities || [])

function normalizeCityName(name) {
  return String(name || '').replace(/市$/, '')
}

function resetSelection() {
  const cityName = normalizeCityName(props.currentCity)
  const matchedProvinceIndex = chinaRegions.findIndex(region =>
    region.cities.some(city => normalizeCityName(city.name) === cityName)
  )

  provinceIndex.value = matchedProvinceIndex >= 0 ? matchedProvinceIndex : 0
  selectedCity.value = currentCities.value.find(
    city => normalizeCityName(city.name) === cityName
  ) || currentCities.value[0] || null
}

function selectProvince(index) {
  provinceIndex.value = index
  selectedCity.value = currentCities.value[0] || null
}

function close() {
  emit('update:visible', false)
}

function confirm() {
  if (!selectedCity.value) {
    uni.showToast({ title: '请先选择城市', icon: 'none' })
    return
  }

  locationStore.setLocation({
    city: selectedCity.value.name,
    longitude: selectedCity.value.longitude,
    latitude: selectedCity.value.latitude
  })

  if (props.targetForm) {
    props.targetForm.city = selectedCity.value.name
    props.targetForm.longitude = selectedCity.value.longitude
    props.targetForm.latitude = selectedCity.value.latitude
  }

  close()
}

watch(() => props.visible, visible => {
  // 原生底部导航会盖住弹窗底部的“确定”按钮，因此打开时暂时隐藏。
  setTabBarVisibility(!visible)
  if (visible) resetSelection()
}, { immediate: true })

// 页面离开时保证恢复导航栏，避免其他页面没有底部导航。
onBeforeUnmount(() => {
  setTabBarVisibility(true)
})
</script>

<style scoped lang="scss">
.picker-mask {
  position: fixed;
  z-index: 999;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(30, 42, 29, 0.45);
}

.picker-panel {
  box-sizing: border-box;
  width: 100%;
  height: 720rpx;
  padding: 32rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #fffdf7;
}

.picker-header,
.picker-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-title {
  color: #175b43;
  font-size: 34rpx;
  font-weight: 600;
}

.picker-close {
  padding: 0 12rpx;
  color: #718578;
  font-size: 46rpx;
  line-height: 1;
}

.picker-content {
  display: flex;
  height: 510rpx;
  margin-top: 28rpx;
  overflow: hidden;
  border-radius: 16rpx;
  background: #f5f4ed;
}

.province-list {
  width: 42%;
  background: #edf2ed;
}

.city-list {
  flex: 1;
}

.province-item,
.city-item {
  padding: 26rpx 24rpx;
  color: #52675a;
  font-size: 27rpx;
}

.province-item.active,
.city-item.active {
  color: #175b43;
  font-weight: 600;
  background: #fffdf7;
}

.picker-actions {
  gap: 20rpx;
  margin-top: 24rpx;
}

.cancel-button,
.confirm-button {
  flex: 1;
  border: 0;
  border-radius: 14rpx;
  font-size: 27rpx;
}

.cancel-button {
  color: #52675a;
  background: #edf2ed;
}

.confirm-button {
  color: #ffffff;
  background: #175b43;
}

.cancel-button::after,
.confirm-button::after {
  border: 0;
}
</style>
