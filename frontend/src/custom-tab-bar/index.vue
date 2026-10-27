<template>
  <view v-if="visible" class="tea-tab-bar">
    <view class="tea-tab-shell">
      <view
        v-for="(item, index) in tabs"
        :key="item.path"
        class="tea-tab-item"
        :class="{ 'is-active': selectedIndex === index }"
        @tap="switchTab(item, index)"
      >
        <view class="tea-tab-icon-wrap">
          <view class="tea-tab-icon" :class="`tea-tab-icon--${item.icon}`" />
        </view>
        <text class="tea-tab-label">{{ item.text }}</text>
        <view class="tea-tab-dot" />
      </view>
    </view>
  </view>
</template>

<script>
import { getActiveTabPath, setActiveTabPath } from '@/utils/tab-bar-state.js'

export default {
  data() {
    return {
      selectedIndex: 0,
      visible: true,
      tabs: [
        { path: '/pages/home/home', text: '今日一品', icon: 'tea' },
        { path: '/pages/pairing/pairing', text: '茶点', icon: 'snack' },
        { path: '/pages/record/list', text: '茶记', icon: 'record' },
        { path: '/pages/profile/profile', text: '个人', icon: 'profile' }
      ]
    }
  },
  created() {
    this.syncSelectedTab()
    if (typeof uni.$on === 'function') {
      uni.$on('tea-tab-bar-visibility', this.setVisibility)
      uni.$on('tea-tab-bar-active-path', this.setActivePath)
    }
  },
  mounted() {
    this.hideNativeTabBarOnApp()
    this.syncSelectedTab()
  },
  onShow() {
    this.syncSelectedTab()
  },
  beforeUnmount() {
    if (typeof uni.$off === 'function') {
      uni.$off('tea-tab-bar-visibility', this.setVisibility)
      uni.$off('tea-tab-bar-active-path', this.setActivePath)
    }
  },
  methods: {
    setVisibility(visible) {
      this.visible = visible
    },
    setActivePath(path) {
      const activeIndex = this.tabs.findIndex((item) => item.path === path)

      if (activeIndex >= 0) {
        this.selectedIndex = activeIndex
      }
    },
  
    syncSelectedTab() {
      const activePath = getActiveTabPath()

      if (activePath) {
        this.setActivePath(activePath)
        return
      }

      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const currentPath = currentPage ? `/${currentPage.route}` : ''

      if (this.tabs.some((item) => item.path === currentPath)) {
        setActiveTabPath(currentPath)
      }
    },
    // App 端系统原生 tabBar 无法用组件替换，只能隐藏；这里做一次兜底，
    // 避免启动时机的差异导致原生 tabBar 与自定义导航同时出现。
    hideNativeTabBarOnApp() {
      // #ifdef APP-PLUS
      if (typeof uni.hideTabBar === 'function') {
        uni.hideTabBar({ animation: false })
      }
      // #endif
    },
    switchTab(item, index) {
      if (getActiveTabPath() === item.path) {
        return
      }

      setActiveTabPath(item.path)
      uni.switchTab({ url: item.path })
    }
  }
}
</script>

<style lang="scss">
.tea-tab-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 999;
  box-sizing: border-box;
  padding: 0 24rpx calc(16rpx + env(safe-area-inset-bottom));
  pointer-events: none;
}

.tea-tab-shell {
  display: flex;
  align-items: stretch;
  box-sizing: border-box;
  min-height: 132rpx;
  padding: 8rpx 12rpx 6rpx;
  border: 1rpx solid rgba(206, 218, 205, 0.72);
  border-radius: 32rpx;
  background: rgba(255, 254, 249, 0.94);
  box-shadow: 0 12rpx 32rpx rgba(42, 71, 54, 0.12);
  backdrop-filter: blur(18rpx);
  pointer-events: auto;
}

.tea-tab-item {
  display: flex;
  position: relative;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 4rpx 0;
  color: #829188;
}

.tea-tab-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.tea-tab-icon {
  position: relative;
  box-sizing: border-box;
  width: 38rpx;
  height: 38rpx;
  color: currentColor;
}

.tea-tab-label {
  margin-top: 3rpx;
  font-size: 21rpx;
  letter-spacing: 1rpx;
  line-height: 1.25;
}

.tea-tab-dot {
  width: 6rpx;
  height: 6rpx;
  margin-top: 4rpx;
  border-radius: 50%;
  background: transparent;
}

.tea-tab-item.is-active {
  color: #1a6248;
  font-weight: 600;
}

.tea-tab-item.is-active .tea-tab-icon-wrap {
  background: #dcefe3;
  transform: translateY(-2rpx);
}

.tea-tab-item.is-active .tea-tab-dot {
  background: #2f8c68;
}

.tea-tab-icon--tea {
  height: 30rpx;
  margin-top: 6rpx;
  border: 3rpx solid currentColor;
  border-top: 0;
  border-radius: 0 0 18rpx 18rpx;
}

.tea-tab-icon--tea::before {
  position: absolute;
  top: -4rpx;
  left: -4rpx;
  width: 40rpx;
  height: 9rpx;
  border-top: 3rpx solid currentColor;
  border-radius: 50%;
  content: '';
}

.tea-tab-icon--tea::after {
  position: absolute;
  top: -14rpx;
  left: 17rpx;
  width: 8rpx;
  height: 12rpx;
  border-left: 3rpx solid currentColor;
  border-radius: 50%;
  content: '';
  transform: rotate(16deg);
}

.tea-tab-icon--snack {
  width: 34rpx;
  height: 34rpx;
  margin-top: 2rpx;
  border: 3rpx solid currentColor;
  border-radius: 50%;
}

.tea-tab-icon--snack::before,
.tea-tab-icon--snack::after {
  position: absolute;
  border-radius: 50%;
  background: currentColor;
  content: '';
}

.tea-tab-icon--snack::before {
  top: 8rpx;
  left: 8rpx;
  width: 5rpx;
  height: 5rpx;
  box-shadow: 10rpx 2rpx 0 currentColor, 4rpx 11rpx 0 currentColor;
}

.tea-tab-icon--snack::after {
  right: -4rpx;
  bottom: -4rpx;
  width: 11rpx;
  height: 11rpx;
  border: 3rpx solid #fffef9;
}

.tea-tab-icon--record {
  width: 31rpx;
  height: 38rpx;
  margin-top: 1rpx;
  border: 3rpx solid currentColor;
  border-radius: 5rpx;
}

.tea-tab-icon--record::before {
  position: absolute;
  top: 9rpx;
  left: 6rpx;
  width: 15rpx;
  height: 3rpx;
  border-radius: 3rpx;
  background: currentColor;
  box-shadow: 0 8rpx 0 currentColor;
  content: '';
}

.tea-tab-icon--record::after {
  position: absolute;
  top: -3rpx;
  right: -3rpx;
  width: 11rpx;
  height: 11rpx;
  border-top: 3rpx solid currentColor;
  border-right: 3rpx solid currentColor;
  border-radius: 0 5rpx 0 5rpx;
  background: #fffef9;
  content: '';
}

.tea-tab-icon--profile::before {
  position: absolute;
  top: 0;
  left: 11rpx;
  box-sizing: border-box;
  width: 16rpx;
  height: 16rpx;
  border: 3rpx solid currentColor;
  border-radius: 50%;
  content: '';
}

.tea-tab-icon--profile::after {
  position: absolute;
  right: 3rpx;
  bottom: 1rpx;
  left: 3rpx;
  box-sizing: border-box;
  height: 16rpx;
  border: 3rpx solid currentColor;
  border-bottom: 0;
  border-radius: 18rpx 18rpx 0 0;
  content: '';
}
</style>
