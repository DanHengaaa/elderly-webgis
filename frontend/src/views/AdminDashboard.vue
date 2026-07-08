<template>
  <div class="simple-page">
    <section class="card">
      <h1>管理员后台</h1>
      <p>这里后续用于机构入驻审批、社区内容审核、数据治理与运营看板。</p>

      <div class="actions">
        <button @click="goCommunityReview">社区内容审核</button>
        <button @click="goInstitutionReview">机构入驻审核</button>
        <button @click="logout">退出登录</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearAuth } from '../utils/auth'

const router = useRouter()

function goCommunityReview() {
  router.push('/admin/community-review')
}

function goInstitutionReview() {
  router.push('/admin/institution-review')
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
  flex-wrap: wrap;
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