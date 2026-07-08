<template>
  <div class="admin-page">
    <header class="admin-topbar">
      <button @click="goAdmin">← 返回后台</button>
      <h1>社区内容审核</h1>
      <select v-model="status" @change="loadPosts">
        <option value="0">待审核</option>
        <option value="1">已通过</option>
        <option value="2">已驳回</option>
      </select>
    </header>

    <main class="admin-main">
      <section v-if="posts.length === 0" class="empty-card">
        当前暂无对应状态的社区帖子。
      </section>

      <article v-for="post in posts" :key="post.id" class="review-card">
        <div>
          <span class="type-tag">{{ post.postTypeText }}</span>
          <span class="status-tag">{{ post.statusText }}</span>
        </div>

        <h2>{{ post.title }}</h2>
        <p>{{ post.contentSummary }}</p>

        <div class="meta">
          <span>作者：{{ post.authorName }}</span>
          <span>发布时间：{{ formatTime(post.createdAt) }}</span>
        </div>

        <div v-if="Number(status) === 0" class="actions">
          <button @click="approvePost(post.id)">通过</button>
          <button class="danger" @click="rejectPost(post.id)">驳回</button>
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

const posts = ref([])
const status = ref('0')

onMounted(() => {
  loadPosts()
})

async function loadPosts() {
  const response = await axios.get('/api/admin/community/posts', {
    params: {
      status: status.value
    }
  })

  posts.value = response.data || []
}

async function approvePost(id) {
  await axios.post(`/api/admin/community/posts/${id}/approve`)
  await loadPosts()
}

async function rejectPost(id) {
  const reason = window.prompt('请输入驳回原因：', '内容不符合社区发布规范。')

  if (reason === null) {
    return
  }

  await axios.post(`/api/admin/community/posts/${id}/reject`, {
    reason
  })

  await loadPosts()
}

function formatTime(value) {
  if (!value) {
    return ''
  }

  return String(value).replace('T', ' ').slice(0, 16)
}

function goAdmin() {
  router.push('/admin')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #eef3f5;
  color: #1f2933;
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
  margin: 0;
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
  border-radius: 22px;
  background: #fff;
  border: 1px solid #d9e0e8;
  box-shadow: 0 12px 28px rgba(31, 41, 51, 0.08);
  padding: 22px;
  margin-bottom: 16px;
}

.type-tag,
.status-tag {
  display: inline-block;
  border-radius: 999px;
  padding: 6px 10px;
  margin-right: 8px;
  font-size: 13px;
  font-weight: 900;
}

.type-tag {
  background: #e3f1ed;
  color: #2e7d6b;
}

.status-tag {
  background: #f5e8dc;
  color: #e8845b;
}

.review-card h2 {
  margin: 14px 0 8px;
}

.review-card p {
  color: #52606d;
  line-height: 1.8;
}

.meta {
  display: flex;
  gap: 18px;
  color: #7b8794;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
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