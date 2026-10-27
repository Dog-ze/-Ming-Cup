import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(
  new URL('../src/utils/location.js', import.meta.url),
  'utf8'
)
const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
const { getCurrentLocation, getLocationErrorMessage } = await import(moduleUrl)

test('H5 浏览器可定位时，优先使用浏览器原生定位结果', async () => {
  const originalWindow = Object.getOwnPropertyDescriptor(globalThis, 'window')
  const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, 'navigator')
  const originalUni = globalThis.uni

  Object.defineProperty(globalThis, 'window', { value: {}, configurable: true })
  Object.defineProperty(globalThis, 'navigator', {
    value: {
      geolocation: {
        getCurrentPosition(success) {
          success({ coords: { longitude: 118.9996, latitude: 25.4094 } })
        }
      }
    },
    configurable: true
  })
  globalThis.uni = {
    getLocation() {
      throw new Error('H5 不应调用 uni.getLocation')
    }
  }

  try {
    const location = await getCurrentLocation()
    assert.deepEqual(location, {
      city: null,
      displayLabel: '当前位置',
      longitude: 118.9996,
      latitude: 25.4094,
      coordinateSystem: 'wgs84'
    })
  } finally {
    if (originalWindow) Object.defineProperty(globalThis, 'window', originalWindow)
    else delete globalThis.window
    if (originalNavigator) Object.defineProperty(globalThis, 'navigator', originalNavigator)
    else delete globalThis.navigator
    globalThis.uni = originalUni
  }
})

test('App 定位保留带“市”的真实城市，同时标记首页显示当前位置', async () => {
  const appSource = source.replace(
    /\/\/ #ifdef H5[\s\S]*?\/\/ #endif\s*/,
    ''
  )
  const appModuleUrl = 'data:text/javascript;base64,' + Buffer.from(appSource).toString('base64')
  const { getCurrentLocation: getAppCurrentLocation } = await import(appModuleUrl)
  const originalUni = globalThis.uni

  globalThis.uni = {
    getLocation({ success }) {
      success({
        address: { city: '莆田' },
        longitude: 119.0077,
        latitude: 25.4541
      })
    }
  }

  try {
    const location = await getAppCurrentLocation()
    assert.deepEqual(location, {
      city: '莆田市',
      displayLabel: '当前位置',
      longitude: 119.0077,
      latitude: 25.4541,
      coordinateSystem: 'gcj02'
    })
  } finally {
    globalThis.uni = originalUni
  }
})

test('定位权限被拒绝时，返回开启权限提示', () => {
  const message = getLocationErrorMessage({
    errMsg: 'getLocation:fail auth deny'
  })

  assert.equal(message, '定位权限未开启，请允许定位后重试')
})

test('H5 地址不安全时，返回 localhost 或 HTTPS 提示', () => {
  const message = getLocationErrorMessage({
    errMsg: 'getLocation:fail https is required'
  })

  assert.equal(message, '浏览器定位需要使用 localhost 或 HTTPS 地址')
})

test('其他定位失败时，返回手动选择城市提示', () => {
  const message = getLocationErrorMessage({
    errMsg: 'getLocation:fail timeout'
  })

  assert.equal(message, '暂时无法获取当前位置，请手动选择城市')
})

test('位置仓库存储并恢复坐标系，附近茶食页自动定位后将坐标系传给后端', async () => {
  const store = await readFile(new URL('../src/store/location.js', import.meta.url), 'utf8')
  const nearby = await readFile(new URL('../src/pages/pairing/nearby.vue', import.meta.url), 'utf8')

  assert.match(store, /coordinateSystem: 'gcj02'/)
  assert.match(store, /this\.coordinateSystem = location\.coordinateSystem \|\| 'gcj02'/)
  assert.match(store, /coordinateSystem: this\.coordinateSystem/)
  assert.match(nearby, /onLoad\(async \(options\) =>/)
  assert.match(nearby, /await refreshCurrentLocation\(true\)/)
  assert.match(nearby, /coordinateSystem: locationStore\.coordinateSystem \|\| 'gcj02'/)
  assert.match(nearby, /正在查找附近茶食/)
})
