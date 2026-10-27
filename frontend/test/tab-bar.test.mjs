import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

/**
 * uni-app 的条件编译注释只在构建时生效，node 直接 import 源码时会保留两组分支。
 * 这里按 H5 预处理一次：丢掉 APP-PLUS 分支、保留 H5 分支，运行时行为才与网页端一致。
 */
function preprocessAsH5(source) {
  return source
    .replace(/[ \t]*\/\/\s*#ifdef\s+APP-PLUS[\s\S]*?\/\/\s*#endif[^\n]*\n?/g, '')
    .replace(/[ \t]*\/\/\s*#ifndef\s+APP-PLUS[^\n]*\n?/g, '')
    .replace(/[ \t]*\/\/\s*#endif[^\n]*\n?/g, '')
}

const tabBarUrl = async () => {
  const source = preprocessAsH5(
    await readFile(new URL('../src/utils/tab-bar.js', import.meta.url), 'utf8')
  )
  return 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
}

const { setTabBarVisibility } = await import(await tabBarUrl())

const pagesConfig = await readFile(
  new URL('../src/pages.json', import.meta.url),
  'utf8'
)

test('底部导航使用项目内的茶席式自定义导航', async () => {
  assert.equal(JSON.parse(pagesConfig).tabBar.custom, true)

  const customTabBar = await readFile(
    new URL('../src/custom-tab-bar/index.vue', import.meta.url),
    'utf8'
  )

  assert.match(customTabBar, /tea-tab-shell/)
  assert.match(customTabBar, /今日一品/)
  assert.match(customTabBar, /茶点/)
  assert.match(customTabBar, /茶记/)
  assert.match(customTabBar, /个人/)
  assert.match(customTabBar, /uni\.switchTab/)
})

test('网页端的四个主页面均挂载茶席式导航，并隐藏系统文字栏', async () => {
  const tabPages = [
    '../src/pages/home/home.vue',
    '../src/pages/pairing/pairing.vue',
    '../src/pages/record/list.vue',
    '../src/pages/profile/profile.vue'
  ]

  for (const pagePath of tabPages) {
    const pageSource = await readFile(new URL(pagePath, import.meta.url), 'utf8')
    assert.match(pageSource, /<TeaTabBar\s*\/>/)
    assert.match(pageSource, /import TeaTabBar from '@\/custom-tab-bar\/index\.vue'/)
  }

  const appSource = await readFile(new URL('../src/App.vue', import.meta.url), 'utf8')
  assert.match(appSource, /\.uni-tabbar\s*\{\s*display:\s*none\s*!important;/)
})

test('App-Plus 也会编译并渲染自定义导航（不再被 #ifdef H5 排除）', async () => {
  const tabPages = [
    '../src/pages/home/home.vue',
    '../src/pages/pairing/pairing.vue',
    '../src/pages/record/list.vue',
    '../src/pages/profile/profile.vue'
  ]

  for (const pagePath of tabPages) {
    const pageSource = await readFile(new URL(pagePath, import.meta.url), 'utf8')

    // 旧的写法把组件和 import 都包在 // #ifdef H5 里，导致 App-Plus 包中根本没有这个组件。
    assert.doesNotMatch(
      pageSource,
      /#ifdef H5[\s\S]{0,200}TeaTabBar/,
      `${pagePath} 不应再用 #ifdef H5 排除自定义导航`
    )
    assert.match(pageSource, /import TeaTabBar from '@\/custom-tab-bar\/index\.vue'/)
    assert.match(pageSource, /<TeaTabBar\s*\/>/)
  }
})

test('App 启动时隐藏系统原生 tabBar，避免与自定义导航同时出现', async () => {
  const appSource = await readFile(new URL('../src/App.vue', import.meta.url), 'utf8')
  assert.match(appSource, /import \{ initCustomTabBar \} from '@\/utils\/tab-bar\.js'/)
  assert.match(appSource, /initCustomTabBar\(\)/)

  const tabBarSource = await readFile(new URL('../src/utils/tab-bar.js', import.meta.url), 'utf8')
  assert.match(tabBarSource, /export function initCustomTabBar\(\)/)
  assert.match(tabBarSource, /#ifdef APP-PLUS/)
  assert.match(tabBarSource, /uni\.hideTabBar/)

  const customTabBar = await readFile(
    new URL('../src/custom-tab-bar/index.vue', import.meta.url),
    'utf8'
  )
  assert.match(customTabBar, /#ifdef APP-PLUS/)
  assert.match(customTabBar, /hideNativeTabBarOnApp/)
})

test('打开弹窗时隐藏底部导航', () => {
  let hideCalled = false
  globalThis.uni = {
    hideTabBar() {
      hideCalled = true
    },
    showTabBar() {}
  }

  setTabBarVisibility(false)

  assert.equal(hideCalled, true)
})

test('关闭弹窗时恢复底部导航', () => {
  let showCalled = false
  globalThis.uni = {
    hideTabBar() {},
    showTabBar() {
      showCalled = true
    }
  }

  setTabBarVisibility(true)

  assert.equal(showCalled, true)
})

test('弹窗开关会同步通知自定义导航', () => {
  const visibilityEvents = []
  globalThis.uni = {
    hideTabBar() {},
    showTabBar() {},
    $emit(eventName, visible) {
      visibilityEvents.push({ eventName, visible })
    }
  }

  setTabBarVisibility(false)
  setTabBarVisibility(true)

  assert.deepEqual(visibilityEvents, [
    { eventName: 'tea-tab-bar-visibility', visible: false },
    { eventName: 'tea-tab-bar-visibility', visible: true }
  ])
})

test('连续切换页面时，选中路径由共享状态保存', async () => {
  const stateSource = await readFile(
    new URL('../src/utils/tab-bar-state.js', import.meta.url),
    'utf8'
  ).catch(() => '')

  assert.notEqual(stateSource, '', '需要一个跨页面保存选中路径的状态模块')

  const stateModuleUrl = 'data:text/javascript;base64,' + Buffer.from(stateSource).toString('base64')
  const { getActiveTabPath, setActiveTabPath } = await import(stateModuleUrl)

  setActiveTabPath('/pages/record/list')
  setActiveTabPath('/pages/home/home')
  setActiveTabPath('/pages/pairing/pairing')

  assert.equal(getActiveTabPath(), '/pages/pairing/pairing')

  const customTabBar = await readFile(
    new URL('../src/custom-tab-bar/index.vue', import.meta.url),
    'utf8'
  )
  assert.match(customTabBar, /setActiveTabPath\(item\.path\)/)

  const pagePaths = [
    ['../src/pages/home/home.vue', '/pages/home/home'],
    ['../src/pages/pairing/pairing.vue', '/pages/pairing/pairing'],
    ['../src/pages/record/list.vue', '/pages/record/list'],
    ['../src/pages/profile/profile.vue', '/pages/profile/profile']
  ]

  for (const [pageFile, route] of pagePaths) {
    const pageSource = await readFile(new URL(pageFile, import.meta.url), 'utf8')
    assert.match(pageSource, new RegExp(`setActiveTabPath\\('${route}'\\)`))
  }
})
