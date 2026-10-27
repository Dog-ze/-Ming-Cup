<script>
import { useUserStore } from '@/store/user.js'
import { initCustomTabBar } from '@/utils/tab-bar.js'

export default {
  onLaunch() {
    // App-Plus 不会用 pages.json 的 tabBar.custom 替换系统原生 tabBar，
    // 必须在首屏页面创建前整体隐藏，自定义茶席式导航才会成为唯一可见的底部导航。
    initCustomTabBar()

    // 游客可直接使用“今日一品”，因此此处不跳转登录页。
    // 若本地已有资料，则恢复到 Pinia，供已登录页面使用。
    useUserStore().restoreProfile()
  },
}
</script>

<style lang="scss">
@import 'uview-plus/index.scss';

page {
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #2d3a31;
  font-size: 28rpx;
}

/* #ifdef H5 */
.uni-tabbar {
  display: none !important;
}

.uni-app--showtabbar uni-page-wrapper,
.uni-app--showtabbar uni-page-wrapper::after {
  height: 100% !important;
}

.uni-app--showtabbar uni-page-wrapper::after {
  display: none !important;
}
/* #endif */
</style>
