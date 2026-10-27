<script setup>
import { Document, Food, House, Location, Notebook, Tickets, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { logout as logoutRequest } from '@/api/admin-auth'

const router = useRouter()
const adminStore = useAdminStore()

async function logout() {
  try {
    await logoutRequest()
  } catch {
    // Token 可能已失效；无论如何都要清除当前浏览器会话。
  } finally {
    adminStore.clearSession()
    router.replace('/login')
  }
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="224px" class="sidebar">
      <div class="brand"><span class="brand-mark">茗</span><div><strong>一盏茗</strong><small>管理后台</small></div></div>
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/dashboard"><el-icon><House /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/tea-knowledge"><el-icon><Notebook /></el-icon><span>茶叶知识库</span></el-menu-item>
        <el-menu-item index="/pairing-rules"><el-icon><Food /></el-icon><span>茶食规则</span></el-menu-item>
        <el-menu-item index="/stores"><el-icon><Location /></el-icon><span>店铺与推广</span></el-menu-item>
        <el-menu-item index="/user-data"><el-icon><Document /></el-icon><span>用户数据</span></el-menu-item>
        <el-menu-item index="/operation-logs"><el-icon><Tickets /></el-icon><span>操作日志</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar"><span>{{ $route.meta.title || '一盏茗管理后台' }}</span><el-dropdown><span class="admin-name">{{ adminStore.profile?.username || '管理员' }}</span><template #dropdown><el-dropdown-menu><el-dropdown-item @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></el-header>
      <el-main class="main-content"><RouterView /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell { min-height: 100vh; }
.sidebar { background: #173f31; color: #fff; }
.brand { height: 76px; display: flex; align-items: center; gap: 10px; padding: 0 24px; border-bottom: 1px solid rgb(255 255 255 / 12%); }
.brand-mark { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; background: #e8b969; color: #173f31; font-weight: 700; }
.brand strong, .brand small { display: block; } .brand small { margin-top: 3px; color: #aac8b9; font-size: 12px; }
.side-menu { border-right: 0; background: transparent; } .side-menu :deep(.el-menu-item) { color: #c7dbd1; } .side-menu :deep(.el-menu-item:hover), .side-menu :deep(.el-menu-item.is-active) { color: #fff; background: #246047; }
.topbar { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e8eee9; font-weight: 600; }.admin-name { cursor: pointer; color: #4b6659; font-weight: 400; }.main-content { background: #f5f7f4; padding: 24px; }
</style>
