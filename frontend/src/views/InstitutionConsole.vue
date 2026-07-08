<template>
  <div class="simple-page">
    <section class="card">
      <h1>机构工作台</h1>
      <p>这里后续用于机构入驻申请、床位价格维护、资质上传、实景图库和评价回复。</p>

      <div class="actions">
        <button @click="goApplication">提交入驻申请</button>
        <button @click="logout">退出登录</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearAuth } from '../utils/auth'

const router = useRouter()

function goApplication() {
  router.push('/institution-application')
}

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.simple-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef3f5;
}

.card {
  width: 620px;
  border-radius: 26px;
  background: #fff;
  padding: 32px;
  box-shadow: 0 18px 44px rgba(31, 41, 51, 0.12);
}

h1 {
  margin: 0;
  color: #2e7d6b;
}

p {
  color: #52606d;
  line-height: 1.8;
}

.actions {
  display: flex;
  gap: 12px;
}

button {
  height: 40px;
  border: none;
  border-radius: 999px;
  padding: 0 18px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}
</style>