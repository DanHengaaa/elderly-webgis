<template>
  <div class="admin-page">
    <header class="admin-topbar">
      <button class="back-btn" @click="goAdmin">← 返回后台</button>
      <div class="top-title">
        <h1>机构入驻审核</h1>
        <p>查看机构方提交的完整入驻信息，并进行通过或驳回处理</p>
      </div>
      <select v-model="status" @change="loadApplications">
        <option value="pending">待审核</option>
        <option value="approved">已通过</option>
        <option value="rejected">已驳回</option>
      </select>
    </header>

    <main class="admin-main">
      <section v-if="loading" class="empty-card">
        正在加载机构入驻申请...
      </section>

      <section v-else-if="errorMessage" class="empty-card error-card">
        {{ errorMessage }}
      </section>

      <section v-else-if="applications.length === 0" class="empty-card">
        当前暂无对应状态的入驻申请。
      </section>

      <article v-for="item in applications" :key="item.id" class="review-card">
        <div class="card-head">
          <div>
            <span class="status-tag" :class="item.status">{{ item.statusText || statusText(item.status) }}</span>
            <h2>{{ item.institutionName || '未填写机构名称' }}</h2>
            <p>{{ fullAddress(item) }}</p>
          </div>

          <div class="head-actions" v-if="item.status === 'pending' || status === 'pending'">
            <button :disabled="actionLoadingId === item.id" @click="approve(item.id)">
              {{ actionLoadingId === item.id ? '处理中...' : '通过申请' }}
            </button>
            <button :disabled="actionLoadingId === item.id" class="danger" @click="reject(item.id)">
              驳回申请
            </button>
          </div>
        </div>

        <section class="detail-section">
          <h3>一、基础展示信息</h3>
          <div class="info-grid">
            <div><span>机构名称</span><strong>{{ item.institutionName || '未填写' }}</strong></div>
            <div><span>省市区县</span><strong>{{ item.province || '未填' }} {{ item.city || '' }} {{ item.district || '' }}</strong></div>
            <div><span>机构性质</span><strong>{{ item.institutionCategoryText || categoryText(item.institutionCategory) }}</strong></div>
            <div><span>机构等级</span><strong>{{ item.gradeLevel || '未设置' }}</strong></div>
            <div><span>总床位数</span><strong>{{ displayValue(item.totalBeds) }}</strong></div>
            <div><span>空余床位数</span><strong>{{ displayValue(item.availableBeds) }}</strong></div>
            <div><span>月费起步价</span><strong>{{ moneyText(item.monthlyFeeBase) }}</strong></div>
            <div><span>价格档位</span><strong>{{ item.priceTierText || priceTierText(item.priceTier) }}</strong></div>
            <div><span>联系人</span><strong>{{ item.contactPerson || '未填写' }}</strong></div>
            <div><span>联系电话</span><strong>{{ item.contactPhone || '未填写' }}</strong></div>
            <div><span>是否提供全景</span><strong>{{ item.hasPanorama ? '是' : '否' }}</strong></div>
            <div><span>申请时间</span><strong>{{ formatTime(item.createdAt) }}</strong></div>
          </div>
        </section>

        <section class="detail-section">
          <h3>二、地址与空间点位</h3>
          <div class="info-grid">
            <div><span>地址名称</span><strong>{{ item.addressName || '未填写' }}</strong></div>
            <div><span>详细地址</span><strong>{{ item.address || '未填写' }}</strong></div>
            <div><span>点位来源</span><strong>{{ pointSourceText(item.pointSource) }}</strong></div>
            <div><span>经纬度</span><strong>{{ lonLatText(item) }}</strong></div>
          </div>
        </section>

        <section class="detail-section">
          <h3>三、机构展示内容</h3>

          <div class="text-block">
            <span>机构介绍</span>
            <p>{{ item.intro || '未填写机构介绍。' }}</p>
          </div>

          <div class="text-block">
            <span>补充说明</span>
            <p>{{ item.description || '未填写补充说明。' }}</p>
          </div>

          <div class="image-area">
            <div v-if="item.coverImageUrl" class="cover-box">
              <span>封面图片</span>
              <img :src="normalizeFileUrl(item.coverImageUrl)" alt="机构封面" />
            </div>

            <div class="gallery-box">
              <span>机构相册</span>
              <div v-if="parseImages(item.images).length" class="gallery-grid">
                <img
                  v-for="url in parseImages(item.images)"
                  :key="url"
                  :src="normalizeFileUrl(url)"
                  alt="机构相册"
                />
              </div>
              <p v-else>未上传机构相册。</p>
            </div>
          </div>
        </section>

        <section class="detail-section two-col-section">
          <div>
            <h3>四、服务项目</h3>
            <div v-if="item.services && item.services.length" class="mini-list">
              <article v-for="(service, index) in item.services" :key="index">
                <strong>{{ serviceTypeText(service.serviceType) }}</strong>
                <p>{{ service.serviceDetail || '未填写服务说明' }}</p>
                <span>{{ service.isAvailable === false ? '暂不可提供' : '可提供' }}</span>
              </article>
            </div>
            <p v-else class="muted">未填写服务项目。</p>
          </div>

          <div>
            <h3>五、设施设备</h3>
            <div v-if="item.facilities && item.facilities.length" class="mini-list">
              <article v-for="(facility, index) in item.facilities" :key="index">
                <strong>{{ facility.facilityName || '未填写设施名称' }}</strong>
                <p>{{ facility.facilityDesc || '未填写设施说明' }}</p>
              </article>
            </div>
            <p v-else class="muted">未填写设施设备。</p>
          </div>
        </section>

        <section class="detail-section">
          <h3>六、资质材料</h3>
          <div class="material-grid">
            <div class="material-card">
              <span>营业执照</span>
              <a v-if="item.licenseUrl" :href="normalizeFileUrl(item.licenseUrl)" target="_blank">
                {{ getFileName(item.licenseUrl) }}
              </a>
              <p v-else>未上传</p>
            </div>

            <div class="material-card">
              <span>备案证明</span>
              <a v-if="item.recordCertificateUrl" :href="normalizeFileUrl(item.recordCertificateUrl)" target="_blank">
                {{ getFileName(item.recordCertificateUrl) }}
              </a>
              <p v-else>未上传</p>
            </div>

            <div class="material-card full">
              <span>其他材料</span>
              <div v-if="parseMaterials(item.otherMaterialUrl).length" class="file-list">
                <a
                  v-for="url in parseMaterials(item.otherMaterialUrl)"
                  :key="url"
                  :href="normalizeFileUrl(url)"
                  target="_blank"
                >
                  {{ getFileName(url) }}
                </a>
              </div>
              <p v-else>未上传</p>
            </div>
          </div>
        </section>

        <section v-if="item.status === 'rejected' || item.rejectReason" class="reject-box">
          <strong>驳回原因：</strong>{{ item.rejectReason || '未填写' }}
        </section>
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
const loading = ref(false)
const errorMessage = ref('')
const actionLoadingId = ref(null)

onMounted(() => {
  loadApplications()
})

async function loadApplications() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await axios.get('/api/admin/institution-applications', {
      params: {
        status: status.value
      }
    })

    applications.value = response.data || []
  } catch (error) {
    console.error('加载机构入驻申请失败：', error)
    errorMessage.value = getErrorMessage(error, '机构入驻申请加载失败，请确认后端已添加管理员机构审核接口。')
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  if (!confirm('确定通过该机构入驻申请吗？通过后机构会发布到主页地图、机构列表、推荐和分析模块。')) {
    return
  }

  actionLoadingId.value = id

  try {
    const response = await axios.post(`/api/admin/institution-applications/${id}/approve`)
    alert(response.data?.message || '审核通过成功。')
    await loadApplications()
  } catch (error) {
    console.error('审核通过失败：', error)
    alert(getErrorMessage(error, '审核通过失败，请检查后端接口或数据库字段。'))
  } finally {
    actionLoadingId.value = null
  }
}

async function reject(id) {
  const reason = window.prompt('请输入驳回原因：', '资质材料不完整或信息不符合要求。')

  if (reason === null) {
    return
  }

  actionLoadingId.value = id

  try {
    const response = await axios.post(`/api/admin/institution-applications/${id}/reject`, {
      reason
    })
    alert(response.data?.message || '已驳回该申请。')
    await loadApplications()
  } catch (error) {
    console.error('驳回失败：', error)
    alert(getErrorMessage(error, '驳回失败，请检查后端接口或数据库字段。'))
  } finally {
    actionLoadingId.value = null
  }
}

function fullAddress(item) {
  return `${item.province || ''} ${item.city || ''} ${item.district || ''} ${item.address || ''}`.trim() || '未填写地址'
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '未填写' : value
}

function moneyText(value) {
  if (value === null || value === undefined || value === '') {
    return '未填写'
  }
  return `${value} 元/月起`
}

function lonLatText(item) {
  if (item.lon === null || item.lon === undefined || item.lat === null || item.lat === undefined) {
    return '未设置'
  }

  return `${Number(item.lon).toFixed(6)}, ${Number(item.lat).toFixed(6)}`
}

function statusText(value) {
  switch (value) {
    case 'pending':
      return '待审核'
    case 'approved':
      return '已通过'
    case 'rejected':
      return '已驳回'
    default:
      return '未知'
  }
}

function categoryText(value) {
  switch (Number(value)) {
    case 1:
      return '公办公营'
    case 2:
      return '公办民营'
    case 3:
      return '民营'
    default:
      return '未设置'
  }
}

function priceTierText(value) {
  switch (Number(value)) {
    case 1:
      return '3000 元以下'
    case 2:
      return '3000-5000 元'
    case 3:
      return '5000-8000 元'
    case 4:
      return '8000 元以上'
    default:
      return '未设置'
  }
}

function pointSourceText(value) {
  switch (value) {
    case 'tianditu':
      return '天地图地址匹配'
    case 'map_pick':
      return '地图点选'
    default:
      return '未标注'
  }
}

function serviceTypeText(value) {
  switch (value) {
    case 'selfCare':
      return '基本自理'
    case 'semiCare':
      return '半失能照护'
    case 'nursing':
      return '失能护理'
    case 'dementia':
      return '认知症照护'
    case 'rehab':
      return '康复护理'
    case 'medical':
      return '医养结合'
    default:
      return value || '未设置服务类型'
  }
}

function parseImages(value) {
  return parseUrlList(value)
}

function parseMaterials(value) {
  return parseUrlList(value)
}

function parseUrlList(value) {
  if (!value) {
    return []
  }

  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }

  const text = String(value).trim()

  if (!text) {
    return []
  }

  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) {
      return parsed.filter(Boolean)
    }
  } catch (error) {
    // 兼容旧数据：逗号分隔或单个 URL
  }

  return text
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

function normalizeFileUrl(url) {
  if (!url) {
    return ''
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

function getFileName(url) {
  if (!url) {
    return '文件'
  }

  const parts = String(url).split('/')
  return parts[parts.length - 1] || '文件'
}

function formatTime(value) {
  if (!value) {
    return '未记录'
  }

  return String(value).replace('T', ' ').slice(0, 16)
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || fallback
}

function goAdmin() {
  router.push('/admin')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4faf8 0%, #eef3f5 100%);
  color: #20312d;
}

.admin-topbar {
  min-height: 86px;
  padding: 16px 32px;
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
  display: grid;
  grid-template-columns: 150px 1fr 170px;
  align-items: center;
  gap: 18px;
}

.top-title h1 {
  margin: 0;
  text-align: center;
}

.top-title p {
  margin: 6px 0 0;
  text-align: center;
  opacity: 0.9;
  font-size: 13px;
}

.back-btn,
.admin-topbar select {
  height: 40px;
  border: none;
  border-radius: 999px;
  padding: 0 14px;
  font-weight: 900;
}

.back-btn {
  background: #fff;
  color: #2e7d6b;
  cursor: pointer;
}

.admin-main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px;
}

.empty-card,
.review-card {
  background: #fff;
  border-radius: 26px;
  border: 1px solid #d9e8e4;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 14px 32px rgba(31, 80, 70, 0.09);
}

.error-card {
  color: #d9534f;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  border-bottom: 1px solid #edf3f1;
  padding-bottom: 18px;
  margin-bottom: 18px;
}

.status-tag {
  display: inline-block;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 6px 11px;
  font-weight: 900;
  font-size: 13px;
}

.status-tag.approved {
  background: #e3f1ed;
  color: #2e7d6b;
}

.status-tag.rejected {
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.card-head h2 {
  margin: 12px 0 8px;
  font-size: 25px;
  color: #1f5f52;
}

.card-head p {
  margin: 0;
  color: #5f716c;
  line-height: 1.7;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.head-actions button {
  height: 40px;
  border: none;
  border-radius: 999px;
  padding: 0 18px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.head-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.head-actions button.danger {
  background: #d9534f;
}

.detail-section {
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
  background: #f8fbfa;
  border: 1px solid #e3eeea;
}

.detail-section h3 {
  margin: 0 0 14px;
  color: #2e7d6b;
  font-size: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.info-grid div {
  min-height: 58px;
  padding: 12px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e4efeb;
}

.info-grid span,
.text-block span,
.cover-box span,
.gallery-box span,
.material-card span {
  display: block;
  margin-bottom: 6px;
  color: #71837e;
  font-size: 13px;
}

.info-grid strong {
  color: #213c36;
  line-height: 1.5;
  word-break: break-all;
}

.text-block {
  margin-bottom: 14px;
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e4efeb;
}

.text-block p,
.gallery-box p,
.material-card p,
.muted {
  margin: 0;
  color: #5f716c;
  line-height: 1.75;
}

.image-area {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
}

.cover-box,
.gallery-box {
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e4efeb;
}

.cover-box img {
  width: 100%;
  height: 210px;
  border-radius: 16px;
  object-fit: cover;
  display: block;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.gallery-grid img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 14px;
  background: #edf4f1;
}

.two-col-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.mini-list {
  display: grid;
  gap: 10px;
}

.mini-list article {
  padding: 13px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e4efeb;
}

.mini-list strong {
  color: #213c36;
}

.mini-list p {
  margin: 6px 0;
  color: #5f716c;
  line-height: 1.65;
}

.mini-list span {
  color: #2e7d6b;
  font-size: 13px;
  font-weight: 900;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.material-card {
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e4efeb;
}

.material-card.full {
  grid-column: 1 / -1;
}

.material-card a,
.file-list a {
  display: inline-block;
  margin: 4px 10px 4px 0;
  color: #2e7d6b;
  font-weight: 900;
  text-decoration: none;
  word-break: break-all;
}

.reject-box {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(217, 83, 79, 0.1);
  color: #b6423e;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .admin-topbar,
  .card-head,
  .image-area,
  .two-col-section,
  .material-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .head-actions {
    flex-wrap: wrap;
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-main {
    padding: 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
