<template>
  <div class="publish-page">
    <header class="publish-topbar">
      <button @click="goCommunity">← 返回社区</button>
      <h1>发布养老笔记</h1>
      <button @click="submitPost">发布</button>
    </header>

    <main class="publish-main">
      <section class="form-card">
        <h2>基础内容</h2>

        <label>
          标题
          <input v-model="form.title" placeholder="例如：实地探访鼓楼区这家养老机构" />
        </label>

        <label>
          正文
          <textarea
            v-model="form.content"
            placeholder="记录你的探院经历、照护经验、路线体验或避坑建议"
          ></textarea>
        </label>

        <div class="grid-row">
          <label>
            笔记类型
            <select v-model="form.postType">
              <option value="visit">探院笔记</option>
              <option value="care">照护经验</option>
              <option value="review">机构评价</option>
              <option value="guide">养老避坑</option>
              <option value="route">探视路线</option>
              <option value="question">问题求助</option>
            </select>
          </label>

          <label>
            护理类型
            <select v-model="form.careType">
              <option value="">未标注</option>
              <option value="selfCare">基本自理</option>
              <option value="semiCare">半失能</option>
              <option value="nursing">失能护理</option>
              <option value="dementia">认知症照护</option>
            </select>
          </label>
        </div>

        <div class="grid-row">
          <label>
            关联机构
            <select v-model="form.institutionId" @change="syncDistrict">
              <option :value="null">不关联机构</option>
              <option
                v-for="item in institutions"
                :key="item.id"
                :value="item.id"
              >
                {{ item.name }} · {{ item.district }}
              </option>
            </select>
          </label>

          <label>
            区县
            <input v-model="form.district" placeholder="例如：鼓楼区" />
          </label>
        </div>

        <div class="grid-row">
          <label>
            预算下限
            <input v-model.number="form.budgetMin" type="number" placeholder="例如：3000" />
          </label>

          <label>
            预算上限
            <input v-model.number="form.budgetMax" type="number" placeholder="例如：7000" />
          </label>
        </div>

        <div class="grid-row">
          <label>
            探访日期
            <input v-model="form.visitDate" type="date" />
          </label>

          <label class="check-label">
            <input v-model="form.isFieldVisit" type="checkbox" />
            是否实地探访
          </label>
        </div>
      </section>

      <section class="form-card">
        <h2>评分维度</h2>

        <div class="rating-grid">
          <label v-for="item in ratingFields" :key="item.key">
            {{ item.label }}
            <input
              v-model.number="form[item.key]"
              type="number"
              min="0"
              max="5"
              step="0.1"
              placeholder="0-5"
            />
          </label>
        </div>
      </section>

      <section class="form-card">
        <h2>图片与标签</h2>

        <label>
  上传图片
  <input
    type="file"
    multiple
    accept="image/*"
    @change="handleImageUpload"
  />
</label>

<div v-if="uploading" class="uploading-box">
  图片上传中...
</div>

<div v-if="uploadedImages.length" class="image-preview-grid">
  <div
    v-for="url in uploadedImages"
    :key="url"
    class="image-preview-item"
  >
  <img :src="normalizeFileUrl(url)" alt="" />
    <button @click="removeUploadedImage(url)">删除</button>
  </div>
</div>

<label>
  图片 URL，每行一个，可选
  <textarea
    v-model="imageText"
    placeholder="也可以粘贴网络图片 URL，每行一个。"
  ></textarea>
</label>

        <label>
          标签，用逗号分隔
          <input
            v-model="tagText"
            placeholder="例如：探院笔记, 半失能护理, 医疗便利"
          />
        </label>

        <div v-if="errorMessage" class="error-box">
          {{ errorMessage }}
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { isLoggedIn } from '../utils/auth'
const router = useRouter()

const institutions = ref([])
const imageText = ref('')
const tagText = ref('探院笔记')
const errorMessage = ref('')
const uploading = ref(false)
const uploadedImages = ref([])

const form = reactive({
  title: '',
  content: '',
  postType: 'visit',
  institutionId: null,
  district: '',
  careType: '',
  budgetMin: null,
  budgetMax: null,
  visitDate: '',
  isFieldVisit: false,

  overallRating: 4.0,
  environmentRating: 4.0,
  careRating: 4.0,
  medicalRating: 4.0,
  lifeRating: 4.0,
  visitRating: 4.0,
  priceTransparencyRating: 4.0
})

const ratingFields = [
  { key: 'overallRating', label: '综合评分' },
  { key: 'environmentRating', label: '环境舒适度' },
  { key: 'careRating', label: '护理服务' },
  { key: 'medicalRating', label: '医疗便利性' },
  { key: 'lifeRating', label: '生活配套' },
  { key: 'visitRating', label: '探视便利性' },
  { key: 'priceTransparencyRating', label: '价格透明度' }
]

onMounted(() => {
  loadInstitutions()
})

async function loadInstitutions() {
  try {
    const response = await axios.get('/api/community/institutions/options')
    institutions.value = response.data || []
  } catch (error) {
    console.error('加载机构选项失败：', error)
  }
}

function syncDistrict() {
  const item = institutions.value.find(inst => Number(inst.id) === Number(form.institutionId))

  if (item && item.district) {
    form.district = item.district
  }
}

async function handleImageUpload(event) {
  const files = Array.from(event.target.files || [])

  if (!files.length) {
    return
  }

  uploading.value = true
  errorMessage.value = ''

  try {
    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)

      const response = await axios.post('/api/community/upload/image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      if (response.data && response.data.success && response.data.url) {
        uploadedImages.value.push(response.data.url)
      } else {
        errorMessage.value = response.data.message || '部分图片上传失败。'
      }
    }
  } catch (error) {
    console.error('图片上传失败：', error)
    errorMessage.value = '图片上传失败，请检查后端上传接口。'
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function removeUploadedImage(url) {
  uploadedImages.value = uploadedImages.value.filter(item => item !== url)
}

async function submitPost() {
  errorMessage.value = ''


  if (!isLoggedIn()) {
  router.push({
    path: '/login',
    query: {
      redirect: '/community/publish'
    }
  })
  return
}  

  if (!form.title.trim()) {
    errorMessage.value = '请输入标题。'
    return
  }

  if (!form.content.trim()) {
    errorMessage.value = '请输入正文。'
    return
  }

  const manualImageUrls = imageText.value
  .split('\n')
  .map(item => item.trim())
  .filter(Boolean)

const imageUrls = [
  ...uploadedImages.value,
  ...manualImageUrls
]

  const tags = tagText.value
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)

  try {
    const payload = {
      ...form,
      institutionId: form.institutionId ? Number(form.institutionId) : null,
      visitDate: form.visitDate || null,
      imageUrls,
      tags
    }

    const response = await axios.post('/api/community/posts', payload)

    if (response.data && response.data.success) {
  alert(response.data.message || '发布成功，等待管理员审核。')
  router.push('/community')
} else {
  router.push('/community')
}
  } catch (error) {
    console.error('发布失败：', error)
    errorMessage.value = '发布失败，请检查后端接口。'
  }
}

function goCommunity() {
  router.push('/community')
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
.publish-page {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(180deg, #f7f8f6 0%, #eef3f5 100%);
  color: #1f2933;
}

.publish-topbar {
  min-height: 82px;
  padding: 0 32px;
  display: grid;
  grid-template-columns: 150px 1fr 150px;
  align-items: center;
  background: linear-gradient(135deg, #e8845b, #2e7d6b);
  color: #fff;
}

.publish-topbar h1 {
  margin: 0;
  text-align: center;
}

.publish-topbar button {
  height: 40px;
  border: none;
  border-radius: 999px;
  background: rgba(255,255,255,0.18);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.publish-topbar button:last-child {
  background: #fff;
  color: #2e7d6b;
}

.publish-main {
  max-width: 980px;
  margin: 0 auto;
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-card {
  border-radius: 26px;
  background: rgba(255,255,255,0.96);
  border: 1px solid #d9e0e8;
  box-shadow: 0 14px 34px rgba(31, 41, 51, 0.08);
  padding: 24px;
}

.form-card h2 {
  margin: 0 0 18px;
  color: #2e7d6b;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
  color: #52606d;
  font-weight: 900;
}

input,
select,
textarea {
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
  font-size: 14px;
}

input,
select {
  height: 42px;
}

textarea {
  min-height: 120px;
  padding-top: 12px;
  resize: vertical;
}

input:focus,
select:focus,
textarea:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46,125,107,0.12);
}

.grid-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.check-label {
  justify-content: center;
  flex-direction: row;
  align-items: center;
  border-radius: 14px;
  background: #f5f7f9;
  height: 42px;
  margin-top: 25px;
}

.check-label input {
  width: 18px;
  height: 18px;
}

.rating-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.error-box {
  border-radius: 14px;
  background: rgba(217,83,79,0.1);
  color: #d9534f;
  padding: 12px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .publish-topbar {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 18px;
  }

  .publish-topbar h1 {
    text-align: left;
  }

  .grid-row,
  .rating-grid {
    grid-template-columns: 1fr;
  }
}

.uploading-box {
  margin-bottom: 14px;
  border-radius: 14px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 12px;
  font-weight: 900;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.image-preview-item {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #d9e0e8;
  background: #f5f7f9;
}

.image-preview-item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.image-preview-item button {
  position: absolute;
  right: 8px;
  top: 8px;
  border: none;
  border-radius: 999px;
  padding: 5px 9px;
  background: rgba(217, 83, 79, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 760px) {
  .image-preview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}


</style>