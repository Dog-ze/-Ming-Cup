export function getCurrentLocation() {
  // #ifdef H5
  // H5 中直接使用浏览器定位。Chrome 已能取得坐标，避免 uni.getLocation 的 H5 适配失败。
  return getBrowserLocation()
  // #endif

  // App 和小程序继续使用 uni-app 原有定位能力。
  return getUniLocation()
}

/**
 * H5 浏览器定位。浏览器只能返回坐标，不能稳定返回城市名称，
 * 因此不把“当前位置”写进城市字段；它只是首页展示文字，不能作为后台资料中的城市。
 */
function getBrowserLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject({ errMsg: 'getLocation:fail browser geolocation is unavailable' })
      return
    }

    navigator.geolocation.getCurrentPosition(
      result => {
        resolve({
          city: null,
          displayLabel: '当前位置',
          longitude: result.coords.longitude,
          latitude: result.coords.latitude,
          coordinateSystem: 'wgs84'
        })
      },
      error => {
        reject({ errMsg: `getLocation:fail ${error.message || error.code || 'unknown error'}` })
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

/**
 * App、小程序定位。
 */
function getUniLocation() {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      // 请求地址信息；部分平台可能不返回城市，所以城市允许为 null。
      geocode: true,
      // 优先使用较高精度；最多等待 5 秒，避免用户长时间无反馈。
      isHighAccuracy: true,
      highAccuracyExpireTime: 5000,
      success(result) {
        resolve({
          // 后台保存行政区城市，必须保留“市”，例如“莆田市”。
          city: normalizeCityName(result.address?.city),
          // 首页只展示“当前位置”，不把它当成真实城市保存。
          displayLabel: '当前位置',
          longitude: result.longitude,
          latitude: result.latitude,
          coordinateSystem: 'gcj02'
        })
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function normalizeCityName(city) {
  const name = String(city || '').trim()
  if (!name) return null
  return name.endsWith('市') ? name : `${name}市`
}

/**
 * 将系统或浏览器返回的定位错误，转成用户能看懂的提示。
 */
export function getLocationErrorMessage(error) {
  const message = String(error && (error.errMsg || error.message) || '').toLowerCase()

  if (message.includes('auth') || message.includes('permission') || message.includes('deny')) {
    return '定位权限未开启，请允许定位后重试'
  }

  if (message.includes('https') || message.includes('secure origin')) {
    return '浏览器定位需要使用 localhost 或 HTTPS 地址'
  }

  return '暂时无法获取当前位置，请手动选择城市'
}
