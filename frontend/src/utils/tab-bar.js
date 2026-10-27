/**
 * 项目统一使用自定义底部导航（custom-tab-bar/index.vue）。
 *
 * 页面通过本模块控制系统 tabBar 的显隐：
 * - H5：隐藏系统文字栏，只显示自定义导航；
 * - App-Plus：系统原生 tabBar 无法被自定义组件替换，因此启动时整体隐藏，
 *   由自定义导航在自己的实例内控制显隐，页面切换不会把系统原生 tabBar 重新带回来。
 */
export function setTabBarVisibility(visible) {
  if (typeof uni.$emit === 'function') {
    uni.$emit('tea-tab-bar-visibility', visible)
  }

  // App 端不使用系统 tabBar，显隐由自定义导航组件自己负责。
  // 这里仍然发事件，保证自定义导航的显示状态与页面同步。
  if (isAppPlus()) return

  if (visible) {
    uni.showTabBar({ animation: false })
    return
  }

  uni.hideTabBar({ animation: false })
}

function isAppPlus() {
  let appPlus = false
  // #ifdef APP-PLUS
  appPlus = true
  // #endif
  return appPlus
}

/**
 * App 启动时调用：App-Plus 下系统原生 tabBar 不会被 pages.json 的 tabBar.custom 替换，
 * 必须在首屏页面创建之前整体隐藏，自定义导航才会是唯一可见的底部导航。
 */
export function initCustomTabBar() {
  if (!isAppPlus()) return

  if (typeof uni.hideTabBar === 'function') {
    uni.hideTabBar({ animation: false })
  }
}
