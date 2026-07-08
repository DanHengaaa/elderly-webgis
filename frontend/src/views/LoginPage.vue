<template>
  <div class="login-page">
    <section class="login-card">
      <div class="brand">
        <h1>智慧养老平台</h1>
        <p>多角色统一登录 · 客户 / 机构 / 管理员</p>
      </div>

      <div class="quick-row">
        <button @click="fillDemo('customer')">客户体验</button>
        <button @click="fillDemo('institution')">机构体验</button>
        <button @click="fillDemo('admin')">管理员体验</button>
      </div>

      <label>
        用户名
        <input v-model="form.username" placeholder="请输入用户名" />
      </label>

      <label>
        密码
        <input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          @keyup.enter="login"
        />
      </label>

      <button class="login-btn" @click="login">
        登录
      </button>

      <p v-if="errorMessage" class="error-text">
        {{ errorMessage }}
      </p>

      <div class="footer-row">
        <span>还没有账号？</span>
        <button @click="goRegister">去注册</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { saveAuth } from '../utils/auth'

const router = useRouter()
const route = useRoute()

const errorMessage = ref('')

const form = reactive({
  username: '',
  password: ''
})

function fillDemo(type) {
  form.username = type
  form.password = '123456'
}

async function login() {
  errorMessage.value = ''

  if (!form.username.trim() || !form.password.trim()) {
    errorMessage.value = '请输入用户名和密码。'
    return
  }

  try {
    const response = await axios.post('/api/auth/login', {
      username: form.username.trim(),
      password: form.password.trim()
    })

    saveAuth(response.data.token, response.data.user)

    const redirect = route.query.redirect

    if (redirect) {
      router.push(String(redirect))
      return
    }

    const roleCode = response.data.user.roleCode

    if (roleCode === 'ADMIN') {
      router.push('/admin')
    } else if (roleCode === 'INSTITUTION') {
      router.push('/institution-console')
    } else {
      router.push('/map')
    }
  } catch (error) {
    console.error('登录失败：', error)
    errorMessage.value = error.response?.data?.message || '登录失败，请检查用户名和密码。'
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(232, 132, 91, 0.18), transparent 32%),
    linear-gradient(135deg, #e3f1ed 0%, #f7f8f6 55%, #eef3f5 100%);
  color: #1f2933;
}

.login-card {
  width: 420px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d9e0e8;
  box-shadow: 0 24px 60px rgba(31, 41, 51, 0.14);
  padding: 32px;
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.brand h1 {
  margin: 0;
  color: #2e7d6b;
  font-size: 30px;
  font-weight: 900;
}

.brand p {
  margin: 9px 0 0;
  color: #52606d;
}

.quick-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 18px;
}

.quick-row button {
  height: 34px;
  border: none;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  font-weight: 900;
  cursor: pointer;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
  color: #52606d;
  font-weight: 900;
}

input {
  height: 44px;
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
  font-size: 15px;
}

input:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

.login-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 16px;
  background: #2e7d6b;
  color: #fff;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
}

.error-text {
  margin: 14px 0 0;
  color: #d9534f;
  font-weight: 800;
}

.footer-row {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 10px;
  color: #52606d;
}

.footer-row button {
  border: none;
  background: transparent;
  color: #2e7d6b;
  font-weight: 900;
  cursor: pointer;
}
</style>