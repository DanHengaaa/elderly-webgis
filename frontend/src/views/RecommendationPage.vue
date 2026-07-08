<template>
  <div class="recommend-page">
    <header class="recommend-topbar">
  <button class="recommend-back-btn" @click="goBackMap">
    ← 返回地图
  </button>

  <div class="recommend-topbar-title">
    <h1>智能推荐</h1>
    <p>根据老人需求、预算、空间环境和探视可达性推荐合适的养老机构</p>
  </div>
</header>

    <main class="recommend-layout">
      <section class="recommend-form-card">
        <h2>入住需求问卷</h2>

        <div class="form-grid">
          <label>
            <span>老人姓名</span>
            <input v-model="form.elderlyName" placeholder="例如：张先生" />
          </label>

          <label>
            <span>年龄</span>
            <input v-model.number="form.age" type="number" placeholder="例如：78" />
          </label>

          <label>
            <span>护理需求</span>
            <select v-model="form.careLevel">
              <option value="selfCare">基本自理</option>
              <option value="semiCare">半失能 / 需要协助</option>
              <option value="nursing">失能护理</option>
              <option value="dementia">认知症照护</option>
            </select>
          </label>

          <label>
            <span>意向区县</span>
            <select v-model="form.preferredDistrict">
              <option value="不限">不限</option>
              <option value="玄武区">玄武区</option>
              <option value="秦淮区">秦淮区</option>
              <option value="建邺区">建邺区</option>
              <option value="鼓楼区">鼓楼区</option>
              <option value="浦口区">浦口区</option>
              <option value="栖霞区">栖霞区</option>
              <option value="雨花台区">雨花台区</option>
              <option value="江宁区">江宁区</option>
              <option value="六合区">六合区</option>
              <option value="溧水区">溧水区</option>
              <option value="高淳区">高淳区</option>
            </select>
          </label>

          <label>
            <span>最低预算 / 月</span>
            <input v-model.number="form.budgetMin" type="number" placeholder="例如：3000" />
          </label>

          <label>
            <span>最高预算 / 月</span>
            <input v-model.number="form.budgetMax" type="number" placeholder="例如：7000" />
          </label>

          <label>
            <span>机构性质</span>
            <select v-model="form.preferredCategory">
              <option :value="null">不限</option>
              <option :value="1">公办公营</option>
              <option :value="2">公办民营</option>
              <option :value="3">民营</option>
            </select>
          </label>

          <label>
            <span>机构等级</span>
            <select v-model="form.gradeLevel">
              <option value="不限">不限</option>
              <option value="一级">一级</option>
              <option value="二级">二级</option>
              <option value="三级">三级</option>
              <option value="四级">四级</option>
              <option value="五级">五级</option>
            </select>
          </label>
        </div>

        <label class="check-row">
          <input v-model="form.hasAvailableBeds" type="checkbox" />
          <span>优先推荐有空余床位的机构</span>
        </label>

        <div class="recommend-section-title">探视起点</div>

        <div class="start-search-row">
          <input
            v-model="startKeyword"
            placeholder="输入探视起点，例如：新街口 / 河海大学"
            @keyup.enter="searchStart"
          />

          <button @click="searchStart">
            搜索起点
          </button>
        </div>

        <div v-if="startInfo" class="start-info-box">
          <span>当前起点</span>
          <strong>{{ startInfo.formattedAddress }}</strong>
          <p>{{ startInfo.lon.toFixed(6) }}, {{ startInfo.lat.toFixed(6) }}</p>
        </div>

        <div class="recommend-section-title">偏好权重</div>

        <div class="priority-list">
          <label>
            <span>医疗资源优先</span>
            <input v-model.number="form.medicalPriority" type="range" min="1" max="5" />
            <strong>{{ form.medicalPriority }}</strong>
          </label>

          <label>
            <span>生活便利优先</span>
            <input v-model.number="form.lifePriority" type="range" min="1" max="5" />
            <strong>{{ form.lifePriority }}</strong>
          </label>

          <label>
            <span>绿色休闲优先</span>
            <input v-model.number="form.greenPriority" type="range" min="1" max="5" />
            <strong>{{ form.greenPriority }}</strong>
          </label>

          <label>
            <span>探视方便优先</span>
            <input v-model.number="form.visitPriority" type="range" min="1" max="5" />
            <strong>{{ form.visitPriority }}</strong>
          </label>
        </div>

        <button
          class="recommend-submit-btn"
          :disabled="loading"
          @click="submitRecommendation"
        >
          {{ loading ? '正在生成推荐...' : '生成推荐方案' }}
        </button>

        <div v-if="errorMessage" class="recommend-error">
          {{ errorMessage }}
        </div>
      </section>

      <section class="recommend-result-card">
        <div class="recommend-result-header">
          <div>
            <h2>推荐结果</h2>
            <p v-if="responseData">{{ responseData.message }}</p>
            <p v-else>请先填写左侧问卷并生成推荐方案</p>
          </div>

          <strong v-if="responseData">
            {{ responseData.total }} 家
          </strong>
        </div>

        <div v-if="!responseData && !loading" class="recommend-empty">
          推荐结果将在这里展示。系统会综合预算、床位、医疗资源、生活便利、绿色休闲和探视可达性进行排序。
        </div>

        <div v-if="loading" class="recommend-empty">
          正在计算推荐分数，请稍候...
        </div>

        <div v-if="responseData" class="recommend-list">
          <article
            v-for="item in responseData.items"
            :key="item.id"
            class="recommend-item"
          >
            <div class="recommend-item-main">
              <div class="recommend-score-box">
                <strong>{{ Math.round(item.finalScore * 100) }}</strong>
                <span>推荐分</span>
              </div>

              <div class="recommend-info">
                <div class="recommend-title-row">
                  <h3>{{ item.name }}</h3>
                  <span :class="['match-tag', item.matchLevel]">
                    {{ item.matchLevelText }}
                  </span>
                </div>

                <p class="recommend-address">
                  {{ item.district }} · {{ item.address || '暂无地址' }}
                </p>

                <div class="recommend-meta">
                  <span>{{ item.institutionCategoryText }}</span>
                  <span>{{ item.gradeLevel || '暂无等级' }}</span>
                  <span>床位 {{ item.availableBeds ?? '未知' }}/{{ item.totalBeds ?? '未知' }}</span>
                  <span>月费 {{ formatMoney(item.monthlyFeeBase) }}</span>
                  <span>评分 {{ item.ratingAvg ?? '暂无' }}</span>
                </div>

                <div class="score-grid">
                  <div>
                    <span>预算</span>
                    <strong>{{ toPercent(item.budgetScore) }}</strong>
                  </div>
                  <div>
                    <span>医疗</span>
                    <strong>{{ toPercent(item.medicalScore) }}</strong>
                  </div>
                  <div>
                    <span>生活</span>
                    <strong>{{ toPercent(item.lifeScore) }}</strong>
                  </div>
                  <div>
                    <span>环境</span>
                    <strong>{{ toPercent(item.greenScore) }}</strong>
                  </div>
                  <div>
                    <span>探视</span>
                    <strong>{{ toPercent(item.visitScore) }}</strong>
                  </div>
                </div>

                <div class="recommend-reasons">
                  <span
                    v-for="reason in item.reasons"
                    :key="reason"
                  >
                    {{ reason }}
                  </span>
                </div>
              </div>
            </div>

            <div class="recommend-actions">
              <button @click="goDetail(item.id)">
                查看详情
              </button>

              <button class="secondary" @click="goMap(item)">
                地图定位
              </button>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const responseData = ref(null)
const startKeyword = ref('')
const startInfo = ref(null)
const communityCountMap = ref({})

const form = reactive({
  elderlyName: '',
  age: 78,
  careLevel: 'semiCare',

  budgetMin: 3000,
  budgetMax: 7000,

  preferredDistrict: '不限',
  preferredCategory: null,
  gradeLevel: '不限',
  hasAvailableBeds: true,

  startLon: null,
  startLat: null,
  startName: '',

  medicalPriority: 5,
  lifePriority: 4,
  greenPriority: 3,
  visitPriority: 5,

  limit: 20
})

async function searchStart() {
  if (!startKeyword.value.trim()) {
    errorMessage.value = '请输入探视起点关键词。'
    return
  }

  errorMessage.value = ''

  try {
    const response = await axios.get('/api/geo/geocode', {
      params: {
        keyword: startKeyword.value.trim()
      }
    })

    startInfo.value = response.data

    form.startLon = Number(response.data.lon)
    form.startLat = Number(response.data.lat)
    form.startName = response.data.formattedAddress || startKeyword.value.trim()
  } catch (error) {
    console.error('探视起点搜索失败：', error)
    errorMessage.value = '探视起点搜索失败，请检查天地图 key 或换一个更明确的南京地址。'
  }
}

async function submitRecommendation() {
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      ...form,
      preferredCategory: form.preferredCategory === 'null' ? null : form.preferredCategory
    }

    const response = await axios.post('/api/recommendations', payload)

    responseData.value = response.data
  } catch (error) {
    console.error('智能推荐失败：', error)
    errorMessage.value = '智能推荐失败，请检查后端接口、数据库字段或 pgRouting 路网配置。'
  } finally {
    loading.value = false
  }
}

function toPercent(value) {
  if (value === null || value === undefined) {
    return '—'
  }

  return `${Math.round(value * 100)}`
}

function formatMoney(value) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return `${Number(value).toLocaleString()} 元/月`
}

function goDetail(id) {
  router.push(`/institutions/${id}`)
}

function goMap(item) {
  if (!item || !item.id) {
    router.push('/map')
    return
  }

  router.push({
    path: '/map',
    query: {
      institutionId: item.id,
      focusInstitutionId: item.id,
      lon: item.lon ?? '',
      lat: item.lat ?? ''
    }
  })
}
function goBackMap() {
  router.push('/map')
}


</script>
<style scoped>
.recommend-page {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top left, rgba(46, 125, 107, 0.12), transparent 34%),
    linear-gradient(180deg, #f3f7f6 0%, #eef3f5 100%);
  color: #1f2933;
}

/* 顶部导航 */
.recommend-topbar {
  min-height: 96px;
  padding: 0 36px;
  background: linear-gradient(135deg, #2e7d6b 0%, #5fa891 100%);
  color: #ffffff;
  display: grid;
  grid-template-columns: 160px 1fr 160px;
  align-items: center;
  box-shadow: 0 10px 28px rgba(46, 125, 107, 0.24);
}

.recommend-back-btn {
  width: fit-content;
  height: 42px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.18s ease;
}

.recommend-back-btn:hover {
  background: rgba(255, 255, 255, 0.22);
  transform: translateY(-1px);
}

.recommend-topbar-title {
  text-align: center;
}

.recommend-topbar h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 1px;
}

.recommend-topbar p {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
}

.recommend-layout {
  max-width: 1520px;
  margin: 0 auto;
  padding: 26px 26px 48px;
  display: grid;
  grid-template-columns: 430px minmax(0, 1fr);
  gap: 24px;
}

/* 左侧问卷卡片 */
.recommend-form-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d9e0e8;
  border-radius: 26px;
  padding: 24px;
  box-shadow: 0 14px 34px rgba(31, 41, 51, 0.10);
  height: fit-content;
  position: sticky;
  top: 22px;
}

.recommend-form-card h2,
.recommend-result-card h2 {
  margin: 0;
  color: #2e7d6b;
  font-size: 23px;
  font-weight: 900;
}

/* 表单 */
.form-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-grid label span,
.priority-list label span {
  color: #52606d;
  font-size: 13px;
  font-weight: 800;
}

.form-grid input,
.form-grid select,
.start-search-row input {
  width: 100%;
  height: 44px;
  box-sizing: border-box;
  border: 1px solid #d9e0e8;
  border-radius: 13px;
  padding: 0 13px;
  outline: none;
  color: #1f2933;
  background: #ffffff;
  font-size: 14px;
  transition: all 0.18s ease;
}

.form-grid input:focus,
.form-grid select:focus,
.start-search-row input:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

/* 勾选项 */
.check-row {
  margin-top: 17px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #52606d;
  font-size: 14px;
  font-weight: 700;
}

.check-row input {
  width: 16px;
  height: 16px;
  accent-color: #2e7d6b;
}

/* 小标题 */
.recommend-section-title {
  margin-top: 23px;
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 4px solid #2e7d6b;
  color: #1f2933;
  font-size: 16px;
  font-weight: 900;
}

/* 起点搜索 */
.start-search-row {
  display: grid;
  grid-template-columns: 1fr 90px;
  gap: 10px;
}

.start-search-row button {
  border: none;
  border-radius: 13px;
  background: #2e7d6b;
  color: #ffffff;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.18s ease;
}

.start-search-row button:hover {
  background: #236454;
  transform: translateY(-1px);
}

.start-info-box {
  margin-top: 12px;
  border-radius: 16px;
  background: #e3f1ed;
  padding: 13px;
  border: 1px solid rgba(46, 125, 107, 0.12);
}

.start-info-box span {
  color: #52606d;
  font-size: 13px;
  font-weight: 700;
}

.start-info-box strong {
  display: block;
  margin-top: 5px;
  color: #2e7d6b;
  line-height: 1.5;
}

.start-info-box p {
  margin: 5px 0 0;
  color: #52606d;
  font-size: 13px;
}

/* 权重滑块 */
.priority-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.priority-list label {
  display: grid;
  grid-template-columns: 102px 1fr 28px;
  align-items: center;
  gap: 10px;
}

.priority-list input {
  width: 100%;
  accent-color: #2e7d6b;
}

.priority-list strong {
  color: #2e7d6b;
  font-size: 16px;
}

/* 提交按钮 */
.recommend-submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 24px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #2e7d6b, #5fa891);
  color: #ffffff;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(46, 125, 107, 0.22);
  transition: all 0.18s ease;
}

.recommend-submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(46, 125, 107, 0.28);
}

.recommend-submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.recommend-error {
  margin-top: 14px;
  border-radius: 14px;
  background: rgba(217, 83, 79, 0.10);
  color: #d9534f;
  padding: 12px;
  line-height: 1.6;
  font-size: 14px;
}

/* 右侧结果区 */
.recommend-result-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d9e0e8;
  border-radius: 26px;
  padding: 24px;
  box-shadow: 0 14px 34px rgba(31, 41, 51, 0.08);
  min-height: calc(100vh - 150px);
}

.recommend-result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #eef2f5;
  padding-bottom: 18px;
}

.recommend-result-header p {
  margin: 8px 0 0;
  color: #52606d;
  line-height: 1.6;
}

.recommend-result-header strong {
  min-width: 76px;
  height: 42px;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.recommend-empty {
  margin-top: 24px;
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(46, 125, 107, 0.08), rgba(95, 168, 145, 0.04)),
    #f7faf9;
  color: #52606d;
  padding: 42px 32px;
  line-height: 1.8;
  text-align: center;
  border: 1px dashed #b8c9c3;
}

/* 推荐列表 */
.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 20px;
}

.recommend-item {
  border: 1px solid #d9e0e8;
  border-radius: 24px;
  padding: 18px;
  background: #ffffff;
  transition: all 0.2s ease;
}

.recommend-item:hover {
  box-shadow: 0 12px 28px rgba(31, 41, 51, 0.12);
  transform: translateY(-1px);
}

.recommend-item-main {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 18px;
}

.recommend-score-box {
  width: 78px;
  height: 78px;
  border-radius: 24px;
  background: linear-gradient(135deg, #2e7d6b, #5fa891);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.recommend-score-box strong {
  font-size: 29px;
  line-height: 1;
}

.recommend-score-box span {
  margin-top: 6px;
  font-size: 12px;
}

.recommend-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.recommend-title-row h3 {
  margin: 0;
  color: #1f2933;
  font-size: 20px;
  font-weight: 900;
}

.match-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 900;
}

.match-tag.excellent {
  background: #e3f1ed;
  color: #2e7d6b;
}

.match-tag.good {
  background: rgba(58, 158, 92, 0.12);
  color: #3a9e5c;
}

.match-tag.medium {
  background: rgba(232, 132, 91, 0.14);
  color: #e8845b;
}

.match-tag.weak {
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.recommend-address {
  margin: 8px 0 0;
  color: #52606d;
  line-height: 1.6;
}

.recommend-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 11px;
}

.recommend-meta span {
  background: #f5f7f9;
  color: #52606d;
  border-radius: 999px;
  padding: 6px 11px;
  font-size: 13px;
  font-weight: 700;
}

/* 分数网格 */
.score-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(70px, 1fr));
  gap: 10px;
  margin-top: 15px;
}

.score-grid div {
  border-radius: 15px;
  background: #f5f7f9;
  padding: 10px;
  text-align: center;
}

.score-grid span {
  display: block;
  color: #52606d;
  font-size: 12px;
  font-weight: 700;
}

.score-grid strong {
  display: block;
  margin-top: 5px;
  color: #2e7d6b;
  font-size: 18px;
}

/* 推荐理由 */
.recommend-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.recommend-reasons span {
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 7px 11px;
  font-size: 13px;
  font-weight: 700;
}

/* 操作按钮 */
.recommend-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.recommend-actions button {
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 13px;
  background: #2e7d6b;
  color: #ffffff;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.18s ease;
}

.recommend-actions button:hover {
  background: #236454;
  transform: translateY(-1px);
}

.recommend-actions button.secondary {
  background: #f5f7f9;
  color: #2e7d6b;
}

.recommend-actions button.secondary:hover {
  background: #e3f1ed;
}

/* 响应式 */
@media (max-width: 1180px) {
  .recommend-layout {
    grid-template-columns: 1fr;
  }

  .recommend-form-card {
    position: static;
  }
}

@media (max-width: 760px) {
  .recommend-topbar {
    min-height: auto;
    padding: 18px;
    grid-template-columns: 1fr;
    gap: 14px;
    justify-items: start;
  }

  .recommend-topbar-title {
    text-align: left;
  }

  .recommend-topbar h1 {
    font-size: 24px;
  }

  .recommend-back-btn {
    height: 38px;
    padding: 0 15px;
  }

  .recommend-layout {
    padding: 14px 14px 36px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .recommend-item-main {
    grid-template-columns: 1fr;
  }

  .score-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .recommend-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .priority-list label {
    grid-template-columns: 96px 1fr 24px;
  }
}
</style>