<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin-auth'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { max: 32, message: '管理员账号不能超过 32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度应为 6 至 72 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await login({ username: form.username.trim(), password: form.password })
    adminStore.setSession(result.token, result)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (error) {
    form.password = ''
    ElMessage.error(error.message || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="identity"><span>茗</span><p>一盏茗 · 管理后台</p></div>
      <h1>管理员登录</h1>
      <p class="hint">用于管理用户、茶叶、茶食规则、店铺与推广数据。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="管理员账号" prop="username"><el-input v-model="form.username" autocomplete="username" placeholder="请输入管理员账号" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" @keyup.enter="submit" /></el-form-item>
        <el-button type="primary" native-type="submit" class="login-button" :loading="loading">{{ loading ? '登录中…' : '登录' }}</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: radial-gradient(circle at top right, #dceee2, transparent 42%), #f5f7f4; }
.login-card { width: 430px; padding: 42px; border-radius: 18px; background: #fff; box-shadow: 0 18px 50px rgb(23 63 49 / 12%); }
.identity { display: flex; align-items: center; gap: 10px; color: #567063; }.identity span { display: grid; width: 34px; height: 34px; place-items: center; color: #fff; border-radius: 50%; background: #176244; font-weight: 700; }.identity p { margin: 0; font-size: 14px; }
h1 { margin: 28px 0 8px; color: #173f31; font-size: 28px; }.hint { margin: 0 0 28px; color: #7a9184; font-size: 14px; line-height: 1.7; }.login-button { width: 100%; margin-top: 8px; background: #176244; border-color: #176244; }.notice { margin: 22px 0 0; color: #99aa9f; text-align: center; font-size: 12px; line-height: 1.6; }
</style>
