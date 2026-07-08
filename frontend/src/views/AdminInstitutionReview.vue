<template>
  <div class="admin-page">
    <header class="admin-topbar">
      <button @click="goAdmin">← 返回后台</button>
      <h1>机构入驻审核</h1>
      <select v-model="status" @change="loadApplications">
        <option value="pending">待审核</option>
        <option value="approved">已通过</option>
        <option value="rejected">已驳回</option>
      </select>
    </header>

    <main class="admin-main">
      <section v-if="applications.length === 0" class="empty-card">
        当前暂无对应状态的入驻申请。
      </section>

      <article v-for="item in applications" :key="item.id" class="review-card">
        <div>
          <span class="status-tag">{{ item.statusText }}</span>
        </div>

        <h2>{{ item.institutionName }}</h2>
        <p>{{ item.district }} · {{ item.address }}</p>
        <p>联系人：{{ item.contactPerson }} ｜ 电话：{{ item.contactPhone }}</p>
        <p>{{ item.description }}</p>

        <div class="material-list">
          <span v-if="item.licenseUrl">营业执照：{{ item.licenseUrl }}</span>
          <span v-if="item.recordCertificateUrl">备案证明：{{ item.recordCertificateUrl }}</span>
          <span v-if="item.otherMaterialUrl">其他材料：{{ item.otherMaterialUrl }}</span>
        </div>

        <div v-if="status === 'pending'" class="actions">
          <button @click="approve(item.id)">通过</button>
          <button class="danger" @click="reject(item.id)">驳回</button>
        </div>
      </article>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const status = ref('pending')
const applications = ref([])

onMounted(() => {
  loadApplications()
})

async function loadApplications() {
  const response = await axios.get('/api/admin/institution-applications', {
    params: {
      status: status.value
    }
  })

  applications.value = response.data || []
}

async function approve(id) {
  await axios.post(`/api/admin/institution-applications/${id}/approve`)
  await loadApplications()
}

async function reject(id) {
  const reason = window.prompt('请输入驳回原因：', '资质材料不完整或信息不符合要求。')

  if (reason === null) {
    return
  }

  await axios.post(`/api/admin/institution-applications/${id}/reject`, {
    reason
  })

  await loadApplications()
}

function goAdmin() {
  router.push('/admin')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #eef3f5;
}

.admin-topbar {
  height: 82px;
  padding: 0 32px;
  background: linear-gradient(135deg, #2e7d6b, #e8845b);
  color: #fff;
  display: grid;
  grid-template-columns: 150px 1fr 160px;
  align-items: center;
}

.admin-topbar h1 {
  text-align: center;
}

.admin-topbar button,
.admin-topbar select {
  height: 40px;
  border: none;
  border-radius: 999px;
  padding: 0 14px;
  font-weight: 900;
}

.admin-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px;
}

.empty-card,
.review-card {
  background: #fff;
  border-radius: 24px;
  border: 1px solid #d9e0e8;
  padding: 24px;
  margin-bottom: 18px;
  box-shadow: 0 12px 28px rgba(31, 41, 51, 0.08);
}

.status-tag {
  display: inline-block;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 6px 10px;
  font-weight: 900;
}

.review-card h2 {
  margin: 14px 0 8px;
}

.review-card p {
  color: #52606d;
  line-height: 1.7;
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #7b8794;
  font-size: 13px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}

.actions button {
  height: 38px;
  border: none;
  border-radius: 999px;
  padding: 0 18px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.actions button.danger {
  background: #d9534f;
}
</style>