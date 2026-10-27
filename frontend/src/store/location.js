import { defineStore } from 'pinia'

const STORAGE_KEY = 'guest_location'

export const useLocationStore = defineStore('location', {
  state: () => ({
    city: '北京市',
    displayLabel: '北京市',
    longitude: null,
    latitude: null,
    coordinateSystem: 'gcj02'
  }),

  actions: {
    restore() {
      const saved = uni.getStorageSync(STORAGE_KEY)
      if (saved?.city) {
        Object.assign(this, saved)
        // 兼容旧缓存：旧版本没有展示名称时，直接显示真实城市。
        this.displayLabel = saved.displayLabel || saved.city
        // 旧缓存没有坐标类型时，按 App/小程序常用的 GCJ-02 处理。
        this.coordinateSystem = saved.coordinateSystem || 'gcj02'
      }
    },

    setLocation(location) {
      this.city = location.city
      this.displayLabel = location.displayLabel || location.city
      this.longitude = location.longitude ?? null
      this.latitude = location.latitude ?? null
      this.coordinateSystem = location.coordinateSystem || 'gcj02'
      uni.setStorageSync(STORAGE_KEY, {
        city: this.city,
        displayLabel: this.displayLabel,
        longitude: this.longitude,
        latitude: this.latitude,
        coordinateSystem: this.coordinateSystem
      })
    }
  }
})
