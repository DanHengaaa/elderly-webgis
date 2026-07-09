<template>
  <div class="institution-detail-page">
    <header class="detail-topbar">
      <button class="back-btn" @click="goBack">← 返回地图</button>

      <div class="detail-topbar-title">
        <strong>机构详情</strong>
        <span>养老机构完整信息画像</span>
      </div>
    </header>

    <main v-if="loading" class="detail-loading">
      正在加载机构详情...
    </main>

    <main v-else-if="errorMessage" class="detail-error">
      {{ errorMessage }}
      <button class="primary-btn" @click="loadDetail">重新加载</button>
    </main>

    <main v-else-if="detail" class="detail-content">
      <section class="detail-hero">
        <div class="detail-cover">
          <img
            v-if="detail.coverImageUrl"
            :src="normalizeFileUrl(detail.coverImageUrl)"
            alt="机构封面图"
          />
          <div v-else class="detail-cover-placeholder">
            机构实景图
          </div>
        </div>

        <div class="detail-hero-info">
          <div class="detail-title-row">
            <div>
              <h1>{{ detail.name }}</h1>
              <p>{{ detail.address || '暂无地址' }}</p>
            </div>

            <span class="detail-status" :class="{ offline: detail.status !== 1 }">
              {{ detail.statusText || '未知状态' }}
            </span>
          </div>

          <div class="detail-tags">
            <span class="tag primary">{{ detail.institutionCategoryText || '未知性质' }}</span>
            <span v-if="detail.gradeLevel" class="tag">{{ detail.gradeLevel }}</span>
            <span class="tag">{{ detail.priceTierText || '未设置价位' }}</span>
            <span v-if="detail.hasPanorama" class="tag accent">支持全景看房</span>
          </div>

          <div class="hero-metrics">
            <div class="metric-card">
              <span>综合评分</span>
              <strong>{{ formatScore(detail.ratingAvg) }}</strong>
              <small>{{ detail.ratingCount ?? 0 }} 条评价</small>
            </div>

            <div class="metric-card">
              <span>床位情况</span>
              <strong>{{ detail.availableBeds ?? '-' }} / {{ detail.totalBeds ?? '-' }}</strong>
              <small>空余 / 总床位</small>
            </div>

            <div class="metric-card">
              <span>基础月费</span>
              <strong>{{ formatFee(detail.monthlyFeeBase) }}</strong>
              <small>仅供参考</small>
            </div>

            <div class="metric-card">
              <span>所在区县</span>
              <strong>{{ detail.district || '未知' }}</strong>
              <small>{{ detail.city || '' }}</small>
            </div>
          </div>
        </div>
      </section>

      <section class="detail-layout">
        <div class="detail-main-column">
          <section class="detail-section">
            <div class="section-title">
              <h2>基本信息</h2>
              <p>机构登记、位置、联系方式与运营状态</p>
            </div>

            <div class="info-table">
              <InfoItem label="机构名称" :value="detail.name" />
              <InfoItem label="机构性质" :value="detail.institutionCategoryText" />
              <InfoItem label="机构等级" :value="detail.gradeLevel" />
              <InfoItem label="基础月费" :value="formatFee(detail.monthlyFeeBase)" />
              <InfoItem label="总床位" :value="formatNullable(detail.totalBeds)" />
              <InfoItem label="空余床位" :value="formatNullable(detail.availableBeds)" />
              <InfoItem label="联系人" :value="detail.contactPerson" />
              <InfoItem label="联系电话" :value="detail.contactPhone" />
              <InfoItem label="省份" :value="detail.province" />
              <InfoItem label="城市" :value="detail.city" />
              <InfoItem label="区县" :value="detail.district" />
              <InfoItem label="详细地址" :value="detail.address" />
              <InfoItem label="上架状态" :value="detail.statusText" />
              <InfoItem label="创建时间" :value="detail.createdAt" />
              <InfoItem label="更新时间" :value="detail.updatedAt" />
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">
              <h2>机构介绍</h2>
              <p>机构简介与服务定位</p>
            </div>

            <p v-if="detail.intro" class="intro-text">
              {{ detail.intro }}
            </p>

            <p v-else class="empty-detail-text">
              暂无机构介绍。
            </p>
          </section>

          <section class="detail-section">
            <div class="section-title">
              <h2>照护服务能力</h2>
              <p>展示该机构可提供的照护服务类型</p>
            </div>

            <div v-if="availableServices.length > 0" class="service-grid">
              <div
                v-for="service in availableServices"
                :key="service.id"
                class="service-card"
              >
                <strong>{{ formatServiceType(service.serviceType) }}</strong>
                <p>{{ service.serviceDetail || '暂无服务说明' }}</p>
              </div>
            </div>

            <p v-else class="empty-detail-text">
              暂无服务能力数据。
            </p>
          </section>

          <section class="detail-section">
            <div class="section-title">
              <h2>设施设备</h2>
              <p>展示机构已有的生活照护、康复和无障碍设施</p>
            </div>

            <div v-if="detail.facilities && detail.facilities.length > 0" class="facility-grid">
              <div
                v-for="facility in detail.facilities"
                :key="facility.id"
                class="facility-card"
              >
                <div class="facility-icon">✓</div>
                <div>
                  <strong>{{ facility.facilityName || '未命名设施' }}</strong>
                  <p>{{ facility.facilityDesc || '暂无设施说明' }}</p>
                </div>
              </div>
            </div>

            <p v-else class="empty-detail-text">
              暂无设施设备数据。
            </p>
          </section>

          <section class="detail-section community-section">
            <div class="section-title-row">
              <div>
                <h2>社区口碑与探院笔记</h2>
                <p>来自用户真实探院记录、照护经验和社区互动内容</p>
              </div>

              <button @click="goCommunityByInstitution">
                查看更多
              </button>
            </div>

            <div v-if="communityLoading" class="community-summary-card">
              AI 正在生成社区口碑摘要...
            </div>

            <div v-else-if="communitySummary" class="community-summary-card">
              <div class="summary-head">
                <strong>AI 社区口碑摘要</strong>
                <span>{{ communitySummary.generatedBy }} 生成</span>
              </div>

              <p>{{ communitySummary.summary }}</p>

              <div class="summary-meta">
                <span>社区笔记 {{ communitySummary.postCount }} 篇</span>
                <span v-if="communitySummary.avgRating">平均评分 {{ communitySummary.avgRating }}</span>
              </div>
            </div>

            <div v-if="communityPosts.length" class="community-post-list">
              <article
                v-for="post in communityPosts"
                :key="post.id"
                class="community-post-mini"
                @click="goCommunityPost(post.id)"
              >
                <div>
                  <span>{{ post.postTypeText }}</span>
                  <h3>{{ post.title }}</h3>
                  <p>{{ post.contentSummary }}</p>
                </div>

                <strong v-if="post.overallRating">
                  {{ Number(post.overallRating).toFixed(1) }}
                </strong>
              </article>
            </div>

            <div v-else class="community-empty">
              暂无社区笔记，欢迎发布第一篇探院记录。
            </div>
          </section>
        </div>

        <aside class="detail-side-column">
          <section class="side-card">
            <h3>联系与位置</h3>

            <div
              v-if="hasValidCoordinate"
              ref="detailMiniMapRef"
              class="detail-mini-map"
            ></div>

            <div v-else class="detail-mini-map-empty">
              暂无机构位置坐标，无法显示地图
            </div>

            <div class="side-info-list">
              <InfoItem label="所在城市" :value="detail.city" />
              <InfoItem label="所在区县" :value="detail.district" />
              <InfoItem label="详细地址" :value="detail.address" />
              <InfoItem label="联系人" :value="detail.contactPerson" />
              <InfoItem label="联系电话" :value="detail.contactPhone" />
            </div>
          </section>

          <section class="side-card">
            <h3>图片资料</h3>

            <div v-if="imageList.length > 0" class="image-grid">
              <img
                v-for="(image, index) in imageList"
                :key="index"
                :src="normalizeFileUrl(image)"
                alt="机构图片"
              />
            </div>

            <p v-else class="empty-detail-text">
              暂无图片资料。
            </p>
          </section>




        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Map from 'ol/Map.js'
import View from 'ol/View.js'
import TileLayer from 'ol/layer/Tile.js'
import VectorLayer from 'ol/layer/Vector.js'
import XYZ from 'ol/source/XYZ.js'
import VectorSource from 'ol/source/Vector.js'
import Feature from 'ol/Feature.js'
import Point from 'ol/geom/Point.js'
import { fromLonLat } from 'ol/proj.js'
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style.js'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const detailMiniMapRef = ref(null)
const communityPosts = ref([])
const communitySummary = ref(null)
const communityLoading = ref(false)

let detailMiniMap = null

const tiandituTk = '01c5845db0eb91889d42399c5a5b4f16'

const InfoItem = defineComponent({
  name: 'InfoItem',
  props: {
    label: {
      type: String,
      required: true
    },
    value: {
      type: [String, Number, Boolean],
      default: ''
    }
  },
  setup(props) {
    return () =>
      h('div', { class: 'info-item' }, [
        h('span', props.label),
        h(
          'strong',
          props.value === null || props.value === undefined || props.value === ''
            ? '暂无'
            : String(props.value)
        )
      ])
  }
})

const availableServices = computed(() => {
  if (!detail.value || !detail.value.services) {
    return []
  }

  return detail.value.services.filter(service => service.isAvailable !== false)
})

const imageList = computed(() => {
  if (!detail.value) {
    return []
  }

  const result = []

  if (detail.value.coverImageUrl) {
    result.push(detail.value.coverImageUrl)
  }

  const parsedImages = parseImages(detail.value.images)

  parsedImages.forEach(image => {
    if (image && !result.includes(image)) {
      result.push(image)
    }
  })

  return result
})

const institutionLon = computed(() => {
  if (!detail.value) {
    return null
  }

  return Number(detail.value.lonWgs84 || detail.value.lon)
})

const institutionLat = computed(() => {
  if (!detail.value) {
    return null
  }

  return Number(detail.value.latWgs84 || detail.value.lat)
})

const hasValidCoordinate = computed(() => {
  return Number.isFinite(institutionLon.value) && Number.isFinite(institutionLat.value)
})

async function loadDetail() {
  const id = route.params.id

  if (!id) {
    errorMessage.value = '缺少机构 ID'
    return
  }

  loading.value = true
  errorMessage.value = ''
  destroyMiniMap()

  try {
    const response = await axios.get(`/api/institutions/${id}`)
    detail.value = response.data
  } catch (error) {
    console.error('加载机构详情失败：', error)
    errorMessage.value = '加载机构详情失败，请检查后端接口或数据库字段。'
  } finally {
    loading.value = false
  }

  await nextTick()

  requestAnimationFrame(() => {
    initOrUpdateMiniMap()

    setTimeout(() => {
      if (detailMiniMap) {
        detailMiniMap.updateSize()
      }
    }, 200)
  })
}

function buildTiandituUrls(layerName) {
  const subdomains = ['0', '1', '2', '3', '4', '5', '6', '7']

  return subdomains.map(subdomain => {
    return `https://t${subdomain}.tianditu.gov.cn/${layerName}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layerName}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${tiandituTk}`
  })
}

function createTiandituTileLayer(layerName, zIndex, opacity = 1) {
  return new TileLayer({
    opacity,
    zIndex,
    source: new XYZ({
      urls: buildTiandituUrls(layerName),
      wrapX: true,
      transition: 0
    })
  })
}

function initOrUpdateMiniMap() {
  if (!hasValidCoordinate.value) {
    console.warn('机构缺少有效经纬度，无法初始化详情页小地图')
    destroyMiniMap()
    return
  }

  if (!detailMiniMapRef.value) {
    console.warn('详情页小地图容器尚未渲染，暂不初始化')
    return
  }

  const coordinate = fromLonLat([institutionLon.value, institutionLat.value])

  if (detailMiniMap) {
    detailMiniMap.getView().setCenter(coordinate)
    detailMiniMap.getView().setZoom(15)

    const markerLayer = detailMiniMap
      .getLayers()
      .getArray()
      .find(layer => layer.get('layerName') === 'institution-marker-layer')

    if (markerLayer) {
      const source = markerLayer.getSource()
      source.clear()
      source.addFeature(createInstitutionMarker(coordinate))
    }

    detailMiniMap.updateSize()
    return
  }

  const markerSource = new VectorSource({
    features: [createInstitutionMarker(coordinate)]
  })

  const markerLayer = new VectorLayer({
    source: markerSource,
    zIndex: 20,
    style: new Style({
      image: new CircleStyle({
        radius: 10,
        fill: new Fill({ color: '#E8845B' }),
        stroke: new Stroke({ color: '#FFFFFF', width: 3 })
      })
    })
  })

  markerLayer.set('layerName', 'institution-marker-layer')

  detailMiniMap = new Map({
    target: detailMiniMapRef.value,
    layers: [
      createTiandituTileLayer('vec', 1, 1),
      createTiandituTileLayer('cva', 2, 1),
      markerLayer
    ],
    view: new View({
      center: coordinate,
      zoom: 15,
      minZoom: 10,
      maxZoom: 18
    }),
    controls: []
  })

  detailMiniMap.updateSize()

  setTimeout(() => {
    if (detailMiniMap) {
      detailMiniMap.updateSize()
    }
  }, 300)
}
function createInstitutionMarker(coordinate) {
  return new Feature({
    geometry: new Point(coordinate)
  })
}

function destroyMiniMap() {
  if (detailMiniMap) {
    detailMiniMap.setTarget(undefined)
    detailMiniMap = null
  }
}

function parseImages(rawImages) {
  if (!rawImages) {
    return []
  }

  try {
    const parsed = JSON.parse(rawImages)

    if (Array.isArray(parsed)) {
      return parsed.filter(item => typeof item === 'string' && item.trim() !== '')
    }

    if (typeof parsed === 'string' && parsed.trim() !== '') {
      return [parsed]
    }

    return []
  } catch {
    return []
  }
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

function goBack() {
  router.push('/map')
}


function formatServiceType(value) {
  if (value === null || value === undefined || value === '') {
    return '未命名服务'
  }

  const text = String(value).trim()

  const serviceTypeMap = {
    selfCare: '基本自理',
    self_care: '基本自理',
    SELF_CARE: '基本自理',
    semiCare: '半失能照护',
    semi_care: '半失能照护',
    SEMI_CARE: '半失能照护',
    nursing: '失能护理',
    NURSING: '失能护理',
    dementia: '认知症照护',
    DEMENTIA: '认知症照护',
    rehab: '康复护理',
    REHAB: '康复护理',
    medicalCare: '医养结合',
    medical_care: '医养结合',
    medical: '医养结合',
    hospice: '安宁疗护',
    HOSPICE: '安宁疗护'
  }

  return serviceTypeMap[text] || text
}

function formatFee(value) {
  if (value === null || value === undefined || Number(value) === 0) {
    return '暂无'
  }

  return `${Number(value).toFixed(0)} 元/月`
}

function formatScore(value) {
  if (value === null || value === undefined || Number(value) === 0) {
    return '暂无'
  }

  return `${Number(value).toFixed(1)} 分`
}

function formatNullable(value) {
  if (value === null || value === undefined || value === '') {
    return '暂无'
  }

  return String(value)
}

watch(
  () => route.params.id,
  async () => {
    await loadDetail()
    fetchCommunityPosts()
    fetchCommunitySummary()
  }
)

onMounted(async () => {
  await loadDetail()
  fetchCommunityPosts()
  fetchCommunitySummary()
})

async function fetchCommunityPosts() {
  try {
    const response = await axios.get(`/api/community/institutions/${route.params.id}/posts`)
    communityPosts.value = response.data || []
  } catch (error) {
    console.error('加载机构社区笔记失败：', error)
  }
}

async function fetchCommunitySummary() {
  communityLoading.value = true

  try {
    const response = await axios.get(`/api/community/institutions/${route.params.id}/summary`, {
      params: {
        providerId: 'deepseek'
      }
    })

    communitySummary.value = response.data
  } catch (error) {
    console.error('加载机构社区口碑摘要失败：', error)
  } finally {
    communityLoading.value = false
  }
}

function goCommunityPost(id) {
  router.push(`/community/posts/${id}`)
}

function goCommunityByInstitution() {
  router.push({
    path: '/community',
    query: {
      institutionId: route.params.id
    }
  })
}


onBeforeUnmount(() => {
  destroyMiniMap()
})
</script>

<style scoped>
.community-section {
  margin-top: 22px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.section-title-row h2 {
  margin: 0;
  color: #2e7d6b;
  font-size: 22px;
  font-weight: 900;
}

.section-title-row p {
  margin: 6px 0 0;
  color: #52606d;
}

.section-title-row button {
  height: 38px;
  border: none;
  border-radius: 999px;
  padding: 0 16px;
  background: #2e7d6b;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.community-summary-card {
  margin-top: 16px;
  border-radius: 20px;
  background: #f7faf9;
  border: 1px solid #d9e0e8;
  padding: 18px;
  color: #52606d;
  line-height: 1.8;
}

.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.summary-head strong {
  color: #2e7d6b;
  font-size: 16px;
}

.summary-head span {
  color: #9aa5b1;
  font-size: 12px;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.summary-meta span {
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 800;
}

.community-post-list {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.community-post-mini {
  border: 1px solid #d9e0e8;
  border-radius: 18px;
  padding: 14px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}

.community-post-mini:hover {
  background: #f7faf9;
}

.community-post-mini span {
  color: #e8845b;
  font-size: 13px;
  font-weight: 900;
}

.community-post-mini h3 {
  margin: 6px 0;
  font-size: 16px;
}

.community-post-mini p {
  margin: 0;
  color: #52606d;
  line-height: 1.6;
  font-size: 14px;
}

.community-post-mini strong {
  color: #2e7d6b;
  font-size: 22px;
}

.community-empty {
  margin-top: 16px;
  border-radius: 18px;
  background: #f5f7f9;
  padding: 22px;
  text-align: center;
  color: #7b8794;
}
</style>