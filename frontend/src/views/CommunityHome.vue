<template>
  <div class="community-page">
    <header class="community-topbar">
      <button class="back-btn" @click="goMap">← 返回地图</button>

      <div class="top-title">
        <h1>银龄社区</h1>
        <p>真实探院笔记 · 家属照护经验 · 养老机构问答</p>
      </div>

      <button class="publish-btn" @click="goPublish">发布笔记</button>
    </header>

    <main class="community-main">
      <section class="hero-card">
        <div>
          <h2>像小红书一样记录养老选择过程</h2>
          <p>分享探院经历、照护经验、机构评价和避坑指南，让养老决策更透明。</p>
        </div>

        <div class="hero-stats">
          <div>
            <strong>{{ total }}</strong>
            <span>篇社区笔记</span>
          </div>
          <div>
            <strong>{{ posts.length }}</strong>
            <span>当前显示</span>
          </div>
        </div>
      </section>

      <section class="filter-card">
        <div class="type-tabs">
          <button
            v-for="item in typeOptions"
            :key="item.value"
            :class="{ active: filters.type === item.value }"
            @click="changeType(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="filter-row">
          <input
            v-model="filters.keyword"
            placeholder="搜索探院笔记、机构名称、照护经验"
            @keyup.enter="fetchPosts"
          />

          <select v-model="filters.district" @change="fetchPosts">
            <option value="">全部区县</option>
            <option v-for="district in districts" :key="district" :value="district">
              {{ district }}
            </option>
          </select>

          <select v-model="filters.sort" @change="fetchPosts">
            <option value="latest">最新发布</option>
            <option value="hot">最热内容</option>
            <option value="collect">收藏最多</option>
            <option value="rating">评分最高</option>
          </select>

          <button @click="fetchPosts">搜索</button>
        </div>
      </section>

      <section v-if="loading" class="loading-box">
        正在加载社区内容...
      </section>

      <section v-else-if="posts.length === 0" class="empty-box">
        暂无社区笔记，快来发布第一篇探院笔记吧。
      </section>

      <section v-else class="waterfall">
        <article
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="goDetail(post.id)"
        >
          <div class="cover">
            <img v-if="post.coverImageUrl" :src="normalizeFileUrl(post.coverImageUrl)" alt="" />
            <div v-else class="cover-placeholder">
              {{ post.postTypeText }}
            </div>

            <span class="type-tag">{{ post.postTypeText }}</span>
          </div>

          <div class="post-body">
            <h3>{{ post.title }}</h3>
            <p>{{ post.contentSummary }}</p>

            <div class="tag-row">
              <span v-for="tag in post.tags" :key="tag">
                #{{ tag }}
              </span>
            </div>

            <div v-if="post.institutionName" class="institution-line">
              关联机构：{{ post.institutionName }}
            </div>

            <div class="post-footer">
              <span>{{ post.authorName }}</span>
              <div>
                <span>♡ {{ post.likeCount }}</span>
                <span>💬 {{ post.commentCount }}</span>
                <span>☆ {{ post.collectCount }}</span>
              </div>
            </div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const loading = ref(false)
const posts = ref([])
const total = ref(0)

const filters = reactive({
  type: 'all',
  keyword: '',
  district: '',
  sort: 'latest',
  page: 1,
  size: 16
})

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '探院笔记', value: 'visit' },
  { label: '照护经验', value: 'care' },
  { label: '机构评价', value: 'review' },
  { label: '养老避坑', value: 'guide' },
  { label: '探视路线', value: 'route' },
  { label: '问题求助', value: 'question' }
]

const districts = [
  '玄武区',
  '秦淮区',
  '建邺区',
  '鼓楼区',
  '浦口区',
  '栖霞区',
  '雨花台区',
  '江宁区',
  '六合区',
  '溧水区',
  '高淳区'
]

onMounted(() => {
  fetchPosts()
})

async function fetchPosts() {
  loading.value = true

  try {
    const response = await axios.get('/api/community/posts', {
      params: {
        type: filters.type,
        keyword: filters.keyword || undefined,
        district: filters.district || undefined,
        sort: filters.sort,
        page: filters.page,
        size: filters.size
      }
    })

    posts.value = response.data.items || []
    total.value = response.data.total || 0
  } catch (error) {
    console.error('加载社区帖子失败：', error)
  } finally {
    loading.value = false
  }
}

function changeType(type) {
  filters.type = type
  filters.page = 1
  fetchPosts()
}

function goDetail(id) {
  router.push(`/community/posts/${id}`)
}

function goPublish() {
  router.push('/community/publish')
}

function goMap() {
  router.push('/map')
}

function normalizeFileUrl(url) {
  if (!url) {
    return ''
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  const base = import.meta.env.VITE_API_BASE_URL || ''

  if (!base) {
    return url
  }

  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}




</script>

<style scoped>
.community-page {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top left, rgba(232, 132, 91, 0.12), transparent 32%),
    linear-gradient(180deg, #f7f8f6 0%, #eef3f5 100%);
  color: #1f2933;
}

.community-topbar {
  min-height: 96px;
  padding: 0 36px;
  display: grid;
  grid-template-columns: 150px 1fr 150px;
  align-items: center;
  background: linear-gradient(135deg, #e8845b 0%, #2e7d6b 100%);
  color: #fff;
  box-shadow: 0 10px 28px rgba(46, 125, 107, 0.24);
}

.top-title {
  text-align: center;
}

.top-title h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
}

.top-title p {
  margin: 8px 0 0;
  opacity: 0.9;
}

.back-btn,
.publish-btn {
  height: 42px;
  border: 1px solid rgba(255,255,255,0.55);
  border-radius: 999px;
  background: rgba(255,255,255,0.14);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.publish-btn {
  background: #fff;
  color: #2e7d6b;
  border: none;
}

.community-main {
  max-width: 1580px;
  margin: 0 auto;
  padding: 26px;
}

.hero-card,
.filter-card {
  border-radius: 26px;
  background: rgba(255,255,255,0.96);
  border: 1px solid #d9e0e8;
  box-shadow: 0 14px 34px rgba(31, 41, 51, 0.08);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.hero-card h2 {
  margin: 0;
  color: #2e7d6b;
  font-size: 26px;
}

.hero-card p {
  margin: 10px 0 0;
  color: #52606d;
}

.hero-stats {
  display: flex;
  gap: 14px;
}

.hero-stats div {
  min-width: 110px;
  padding: 16px;
  border-radius: 20px;
  background: #e3f1ed;
  text-align: center;
}

.hero-stats strong {
  display: block;
  color: #2e7d6b;
  font-size: 28px;
}

.hero-stats span {
  font-size: 13px;
  color: #52606d;
  font-weight: 800;
}

.filter-card {
  margin-top: 18px;
  padding: 18px;
}

.type-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.type-tabs button {
  border: none;
  border-radius: 999px;
  padding: 9px 15px;
  background: #f5f7f9;
  color: #52606d;
  font-weight: 900;
  cursor: pointer;
}

.type-tabs button.active {
  background: #2e7d6b;
  color: #fff;
}

.filter-row {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 160px 160px 100px;
  gap: 12px;
}

.filter-row input,
.filter-row select {
  height: 42px;
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
}

.filter-row button {
  border: none;
  border-radius: 14px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.loading-box,
.empty-box {
  margin-top: 22px;
  padding: 40px;
  border-radius: 24px;
  background: #fff;
  text-align: center;
  color: #52606d;
}

.waterfall {
  margin-top: 24px;
  columns: 4 260px;
  column-gap: 20px;
}

.post-card {
  break-inside: avoid;
  margin-bottom: 20px;
  border-radius: 24px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d9e0e8;
  box-shadow: 0 10px 24px rgba(31, 41, 51, 0.08);
  cursor: pointer;
  transition: all 0.18s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 36px rgba(31, 41, 51, 0.12);
}

.cover {
  position: relative;
  height: 190px;
  background: #e3f1ed;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(232,132,91,0.35), transparent 36%),
    linear-gradient(135deg, #e3f1ed, #f5e8dc);
  color: #2e7d6b;
  font-size: 24px;
  font-weight: 900;
}

.type-tag {
  position: absolute;
  left: 12px;
  top: 12px;
  border-radius: 999px;
  padding: 6px 11px;
  background: rgba(255,255,255,0.9);
  color: #2e7d6b;
  font-size: 12px;
  font-weight: 900;
}

.post-body {
  padding: 16px;
}

.post-body h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.post-body p {
  margin: 9px 0 0;
  color: #52606d;
  line-height: 1.7;
  font-size: 14px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.tag-row span {
  color: #2e7d6b;
  font-size: 13px;
  font-weight: 800;
}

.institution-line {
  margin-top: 10px;
  color: #e8845b;
  font-size: 13px;
  font-weight: 900;
}

.post-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eef2f5;
  display: flex;
  justify-content: space-between;
  color: #7b8794;
  font-size: 13px;
}

.post-footer div {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .community-topbar {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 18px;
  }

  .top-title {
    text-align: left;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-stats {
    flex-wrap: wrap;
  }
}
</style>