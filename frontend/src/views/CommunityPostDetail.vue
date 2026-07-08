<template>
  <div class="detail-page">
    <header class="top-bar">
      <div class="brand" @click="goCommunity">
        <span class="brand-icon">养</span>
        <div>
          <strong>养老社区</strong>
          <p>探院笔记与养老经验分享</p>
        </div>
      </div>

      <div class="top-actions">
        <button class="ghost-btn" @click="goCommunity">返回社区</button>
        <button class="primary-btn" @click="goPublish">发布笔记</button>
      </div>
    </header>

    <main v-if="loading" class="state-card">
      正在加载笔记详情...
    </main>

    <main v-else-if="errorMessage" class="state-card error">
      {{ errorMessage }}
    </main>

    <main v-else-if="post" class="detail-layout">
      <!-- 左侧：帖子正文 + 评论区 -->
      <section class="content-card">
        <div class="post-meta-row">
          <span class="type-pill">{{ post.postTypeText || '社区笔记' }}</span>
          <span v-if="post.district" class="district-pill">{{ post.district }}</span>
          <span v-if="post.careTypeText" class="care-pill">{{ post.careTypeText }}</span>
        </div>

        <h1>{{ post.title }}</h1>

        <div class="author-row">
          <div class="avatar">
            {{ getAvatarText(post.authorName) }}
          </div>
          <div>
            <strong>{{ post.authorName || '匿名用户' }}</strong>
            <p>{{ formatTime(post.createdAt) }} · 浏览 {{ post.viewCount || 0 }}</p>
          </div>
        </div>

        <div v-if="post.imageUrls && post.imageUrls.length" class="image-grid">
          <img
            v-for="url in post.imageUrls"
            :key="url"
            :src="normalizeFileUrl(url)"
            alt="社区笔记图片"
          />
        </div>

        <article class="post-content">
          {{ post.content }}
        </article>

        <div v-if="post.tags && post.tags.length" class="tag-row">
          <span v-for="tag in post.tags" :key="tag">#{{ tag }}</span>
        </div>

        <div class="info-strip">
          <div>
            <span>预算区间</span>
            <strong>{{ formatBudget(post.budgetMin, post.budgetMax) }}</strong>
          </div>
          <div>
            <span>探访日期</span>
            <strong>{{ post.visitDate || '未填写' }}</strong>
          </div>
          <div>
            <span>实地探访</span>
            <strong>{{ post.isFieldVisit ? '是' : '否' }}</strong>
          </div>
        </div>

        <div class="action-row">
          <button
            class="action-btn"
            :class="{ active: post.likedByCurrentUser }"
            @click="likePost"
          >
            {{ post.likedByCurrentUser ? '♥ 已点赞' : '♡ 点赞' }}
            {{ post.likeCount || 0 }}
          </button>

          <button
            class="action-btn"
            :class="{ active: post.collectedByCurrentUser }"
            @click="collectPost"
          >
            {{ post.collectedByCurrentUser ? '★ 已收藏' : '☆ 收藏' }}
            {{ post.collectCount || 0 }}
          </button>

          <button
            v-if="isPostOwner"
            class="danger-btn"
            @click="deletePost"
          >
            删除帖子
          </button>
        </div>

        <!-- 评论区：放在左侧正文下方 -->
        <section class="comment-card">
          <div class="comment-title-row">
            <h2>评论 {{ comments.length }}</h2>
            <span>欢迎补充真实体验与建议</span>
          </div>

          <div class="comment-input">
            <textarea
              v-model="commentText"
              placeholder="写下你的看法或补充信息..."
            ></textarea>

            <div class="comment-input-actions">
              <p v-if="!loggedIn">登录后可发表评论、点赞和收藏</p>
              <button @click="submitComment">发表评论</button>
            </div>
          </div>

          <div v-if="commentLoading" class="comment-empty">
            正在加载评论...
          </div>

          <div v-else-if="comments.length === 0" class="comment-empty">
            暂无评论，来写下第一条看法吧。
          </div>

          <div v-else class="comment-list">
            <article v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-head">
                <div class="comment-user">
                  <div class="comment-avatar">
                    {{ getAvatarText(comment.authorName) }}
                  </div>
                  <div>
                    <strong>{{ comment.authorName || '匿名用户' }}</strong>
                    <span>{{ formatTime(comment.createdAt) }}</span>
                  </div>
                </div>

                <button
                  v-if="canDeleteComment(comment)"
                  class="comment-delete-btn"
                  @click="deleteComment(comment)"
                >
                  删除
                </button>
              </div>

              <p>{{ comment.content }}</p>
            </article>
          </div>
        </section>
      </section>

      <!-- 右侧：只保留机构信息和评分信息，不放评论 -->
      <aside class="side-card">
        <section v-if="post.institutionId" class="institution-card">
          <h2>关联机构</h2>

          <div class="institution-box" @click="goInstitution(post.institutionId)">
            <strong>{{ post.institutionName || '查看养老机构' }}</strong>
            <p>{{ post.institutionAddress || '暂无详细地址' }}</p>
            <button>查看机构详情</button>
          </div>
        </section>

        <section class="rating-card">
          <h2>笔记评分</h2>

          <div class="score-main">
            <strong>{{ formatRating(post.overallRating) }}</strong>
            <span>综合评分</span>
          </div>

          <div class="rating-list">
            <div>
              <span>环境</span>
              <b>{{ formatRating(post.environmentRating) }}</b>
            </div>
            <div>
              <span>护理</span>
              <b>{{ formatRating(post.careRating) }}</b>
            </div>
            <div>
              <span>医疗</span>
              <b>{{ formatRating(post.medicalRating) }}</b>
            </div>
            <div>
              <span>生活</span>
              <b>{{ formatRating(post.lifeRating) }}</b>
            </div>
            <div>
              <span>探视</span>
              <b>{{ formatRating(post.visitRating) }}</b>
            </div>
            <div>
              <span>价格透明</span>
              <b>{{ formatRating(post.priceTransparencyRating) }}</b>
            </div>
          </div>
        </section>

        <section class="side-tip">
          <h2>温馨提示</h2>
          <p>
            社区笔记属于用户经验分享，建议结合机构详情、空间分析结果、电话咨询和实地探访综合判断。
          </p>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { getCurrentUser, isLoggedIn } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const post = ref(null)
const comments = ref([])
const commentText = ref('')
const loading = ref(false)
const commentLoading = ref(false)
const errorMessage = ref('')

const currentUser = computed(() => getCurrentUser())
const loggedIn = computed(() => isLoggedIn())

const isPostOwner = computed(() => {
  if (!post.value || !currentUser.value) {
    return false
  }

  if (!post.value.authorId) {
    return false
  }

  return Number(post.value.authorId) === Number(currentUser.value.id)
})

onMounted(() => {
  loadPage()
})

watch(
  () => route.params.id,
  () => {
    loadPage()
  }
)

async function loadPage() {
  await fetchPost()
  await fetchComments()
}

async function fetchPost() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await axios.get(`/api/community/posts/${route.params.id}`)
    post.value = response.data
  } catch (error) {
    console.error('加载帖子详情失败：', error)
    errorMessage.value = '帖子详情加载失败，可能帖子不存在或尚未通过审核。'
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  commentLoading.value = true

  try {
    const response = await axios.get(`/api/community/posts/${route.params.id}/comments`)
    comments.value = response.data || []
  } catch (error) {
    console.error('加载评论失败：', error)
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

async function likePost() {
  if (!requireLogin()) {
    return
  }

  try {
    const response = await axios.post(`/api/community/posts/${route.params.id}/like`)

    if (response.data && post.value) {
      post.value.likedByCurrentUser = response.data.liked
      post.value.likeCount = response.data.likeCount
    }
  } catch (error) {
    console.error('点赞失败：', error)
    alert(getErrorMessage(error, '点赞失败，请稍后再试。'))
  }
}

async function collectPost() {
  if (!requireLogin()) {
    return
  }

  try {
    const response = await axios.post(`/api/community/posts/${route.params.id}/collect`)

    if (response.data && post.value) {
      post.value.collectedByCurrentUser = response.data.collected
      post.value.collectCount = response.data.collectCount
    }
  } catch (error) {
    console.error('收藏失败：', error)
    alert(getErrorMessage(error, '收藏失败，请稍后再试。'))
  }
}

async function submitComment() {
  if (!requireLogin()) {
    return
  }

  const content = commentText.value.trim()

  if (!content) {
    alert('请输入评论内容。')
    return
  }

  try {
    await axios.post(`/api/community/posts/${route.params.id}/comments`, {
      parentId: null,
      content
    })

    commentText.value = ''
    await fetchComments()
    await fetchPost()
  } catch (error) {
    console.error('发表评论失败：', error)
    alert(getErrorMessage(error, '评论发布失败，请稍后再试。'))
  }
}

async function deleteComment(comment) {
  if (!requireLogin()) {
    return
  }

  if (!confirm('确定要删除这条评论吗？')) {
    return
  }

  try {
    await axios.delete(`/api/community/posts/${route.params.id}/comments/${comment.id}`)
    await fetchComments()
    await fetchPost()
  } catch (error) {
    console.error('删除评论失败：', error)
    alert(getErrorMessage(error, '删除评论失败，只能删除自己的评论。'))
  }
}

async function deletePost() {
  if (!requireLogin()) {
    return
  }

  if (!confirm('确定要删除这篇帖子吗？删除后普通用户将无法再看到。')) {
    return
  }

  try {
    await axios.delete(`/api/community/posts/${route.params.id}`)
    alert('帖子已删除')
    router.push('/community')
  } catch (error) {
    console.error('删除帖子失败：', error)
    alert(getErrorMessage(error, '删除帖子失败，只能删除自己发布的帖子。'))
  }
}

function canDeleteComment(comment) {
  if (!comment || !currentUser.value) {
    return false
  }

  if (!comment.authorId) {
    return false
  }

  return Number(comment.authorId) === Number(currentUser.value.id)
}

function requireLogin() {
  if (loggedIn.value) {
    return true
  }

  router.push({
    path: '/login',
    query: {
      redirect: route.fullPath
    }
  })

  return false
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

function getAvatarText(name) {
  if (!name) {
    return '养'
  }

  return String(name).slice(0, 1)
}

function formatTime(value) {
  if (!value) {
    return ''
  }

  return String(value).replace('T', ' ').slice(0, 16)
}

function formatRating(value) {
  if (value === null || value === undefined || value === '') {
    return '暂无'
  }

  const number = Number(value)

  if (Number.isNaN(number)) {
    return '暂无'
  }

  return number.toFixed(1)
}

function formatBudget(min, max) {
  if (min && max) {
    return `${min} - ${max} 元/月`
  }

  if (min) {
    return `${min} 元/月以上`
  }

  if (max) {
    return `${max} 元/月以内`
  }

  return '未填写'
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || fallback
}

function goCommunity() {
  router.push('/community')
}

function goPublish() {
  router.push('/community/publish')
}

function goInstitution(id) {
  router.push(`/institutions/${id}`)
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  height: auto;
  overflow-y: auto;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top left, rgba(62, 184, 151, 0.16), transparent 32%),
    linear-gradient(180deg, #f5faf8 0%, #eef4f2 100%);
  padding: 22px 28px 80px;
  color: #20312d;
  box-sizing: border-box;
}

.top-bar {
  max-width: 1220px;
  margin: 0 auto 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2e7d6b, #68c7a5);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 900;
  box-shadow: 0 12px 28px rgba(46, 125, 107, 0.22);
}

.brand strong {
  font-size: 18px;
  color: #1f5f52;
}

.brand p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #6f827d;
}

.top-actions {
  display: flex;
  gap: 10px;
}

.ghost-btn,
.primary-btn {
  border: none;
  border-radius: 999px;
  height: 38px;
  padding: 0 18px;
  font-weight: 800;
  cursor: pointer;
}

.ghost-btn {
  background: #fff;
  color: #2e7d6b;
  border: 1px solid rgba(46, 125, 107, 0.18);
}

.primary-btn {
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
  box-shadow: 0 12px 26px rgba(46, 125, 107, 0.18);
}

.state-card {
  max-width: 1220px;
  margin: 0 auto;
  background: #fff;
  border-radius: 26px;
  padding: 32px;
  box-shadow: 0 18px 45px rgba(29, 70, 61, 0.08);
  color: #5d716c;
}

.state-card.error {
  color: #c94b4b;
}

.detail-layout {
  max-width: 1220px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 22px;
  align-items: start;
}

.content-card,
.side-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28px;
  box-shadow: 0 18px 45px rgba(29, 70, 61, 0.08);
  border: 1px solid rgba(46, 125, 107, 0.08);
}

.content-card {
  padding: 32px;
}

.side-card {
  padding: 22px;
  position: sticky;
  top: 20px;
}

.post-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.type-pill,
.district-pill,
.care-pill {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 900;
}

.type-pill {
  background: rgba(46, 125, 107, 0.12);
  color: #2e7d6b;
}

.district-pill {
  background: rgba(104, 199, 165, 0.14);
  color: #367b6d;
}

.care-pill {
  background: rgba(255, 183, 77, 0.18);
  color: #a46411;
}

.content-card h1 {
  margin: 0;
  font-size: 30px;
  color: #163c35;
  line-height: 1.35;
}

.author-row {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar,
.comment-avatar {
  border-radius: 50%;
  background: linear-gradient(135deg, #2e7d6b, #75c8a9);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 900;
  flex-shrink: 0;
}

.avatar {
  width: 44px;
  height: 44px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  font-size: 14px;
}

.author-row strong {
  color: #244c44;
}

.author-row p {
  margin: 4px 0 0;
  color: #7a8b86;
  font-size: 13px;
}

.image-grid {
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}

.image-grid img {
  width: 100%;
  height: 210px;
  object-fit: cover;
  border-radius: 18px;
  background: #edf4f1;
}

.post-content {
  margin-top: 26px;
  white-space: pre-wrap;
  line-height: 2;
  color: #2f4641;
  font-size: 16px;
}

.tag-row {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  border-radius: 999px;
  background: #f1f7f5;
  color: #2e7d6b;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 900;
}

.info-strip {
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.info-strip div {
  background: #f7fbfa;
  border-radius: 18px;
  padding: 14px;
  border: 1px solid #e6f0ed;
}

.info-strip span {
  display: block;
  font-size: 12px;
  color: #788b85;
  margin-bottom: 6px;
}

.info-strip strong {
  color: #244c44;
}

.action-row {
  margin-top: 26px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-btn,
.danger-btn {
  border: none;
  border-radius: 999px;
  height: 42px;
  padding: 0 18px;
  font-weight: 900;
  cursor: pointer;
}

.action-btn {
  background: #eef7f4;
  color: #2e7d6b;
}

.action-btn.active {
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
}

.danger-btn {
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.comment-card {
  margin-top: 30px;
  padding-top: 26px;
  border-top: 1px solid #eef2f5;
}

.comment-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.comment-title-row h2 {
  margin: 0;
  color: #2e7d6b;
  font-size: 21px;
}

.comment-title-row span {
  color: #7d8f89;
  font-size: 13px;
}

.comment-input {
  background: #f7fbfa;
  border-radius: 20px;
  padding: 14px;
  border: 1px solid #e6f0ed;
}

.comment-input textarea {
  width: 100%;
  min-height: 96px;
  resize: vertical;
  border: 1px solid #d9e5e1;
  border-radius: 16px;
  padding: 12px;
  outline: none;
  box-sizing: border-box;
  line-height: 1.7;
  color: #2f4641;
  background: #fff;
}

.comment-input textarea:focus {
  border-color: #67c6a3;
  box-shadow: 0 0 0 3px rgba(103, 198, 163, 0.14);
}

.comment-input-actions {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.comment-input-actions p {
  margin: 0;
  font-size: 12px;
  color: #8a9a95;
}

.comment-input-actions button {
  border: none;
  border-radius: 999px;
  height: 40px;
  padding: 0 20px;
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.comment-empty {
  margin-top: 18px;
  padding: 22px;
  border-radius: 18px;
  background: #f8fbfa;
  color: #7a8b86;
  text-align: center;
}

.comment-list {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.comment-item {
  border-radius: 20px;
  background: #fff;
  border: 1px solid #edf2f0;
  padding: 16px;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.comment-user {
  display: flex;
  gap: 10px;
  align-items: center;
}

.comment-user strong {
  display: block;
  color: #244c44;
}

.comment-user span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: #8a9a95;
}

.comment-item p {
  margin: 12px 0 0;
  color: #344d47;
  line-height: 1.75;
  white-space: pre-wrap;
}

.comment-delete-btn {
  border: none;
  border-radius: 999px;
  padding: 5px 11px;
  background: rgba(217, 83, 79, 0.1);
  color: #d9534f;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.side-card h2 {
  margin: 0 0 14px;
  color: #2e7d6b;
  font-size: 18px;
}

.institution-card,
.rating-card,
.side-tip {
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #edf2f0;
}

.side-tip {
  border-bottom: none;
  padding-bottom: 0;
  margin-bottom: 0;
}

.institution-box {
  border-radius: 20px;
  background: #f7fbfa;
  padding: 16px;
  cursor: pointer;
  border: 1px solid #e6f0ed;
}

.institution-box strong {
  color: #244c44;
}

.institution-box p {
  color: #7a8b86;
  font-size: 13px;
  line-height: 1.6;
}

.institution-box button {
  border: none;
  border-radius: 999px;
  height: 34px;
  padding: 0 14px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.score-main {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.score-main strong {
  font-size: 34px;
  color: #2e7d6b;
}

.score-main span {
  color: #7a8b86;
  margin-bottom: 6px;
}

.rating-list {
  display: grid;
  gap: 10px;
}

.rating-list div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f7fbfa;
  border-radius: 14px;
  padding: 10px 12px;
}

.rating-list span {
  color: #6f827d;
}

.rating-list b {
  color: #2e7d6b;
}

.side-tip p {
  margin: 0;
  line-height: 1.75;
  color: #6f827d;
  font-size: 13px;
}

@media (max-width: 980px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .side-card {
    position: static;
  }

  .info-strip {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-page {
    padding: 16px;
  }

  .top-bar {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .content-card {
    padding: 22px;
  }

  .content-card h1 {
    font-size: 24px;
  }

  .image-grid {
    grid-template-columns: 1fr;
  }

  .comment-title-row,
  .comment-input-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>