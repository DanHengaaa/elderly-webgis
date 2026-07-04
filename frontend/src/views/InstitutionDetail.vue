<template>
  <div class="institution-detail-page">
    <header class="detail-topbar">
      <button class="back-btn" @click="goBack">← 返回地图</button>

      <div class="detail-topbar-title">
        <strong>机构详情</strong>
        <span>养老机构完整信息画像</span>
      </div>

      <button class="primary-btn" @click="askAi">
        问 AI 伴诊
      </button>
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
            :src="detail.coverImageUrl"
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

          <div class="detail-actions">
            <button class="primary-btn">预约 / 联系</button>
            <button class="secondary-btn">加入对比</button>
            <button class="secondary-btn">收藏机构</button>
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
                <strong>{{ service.serviceType || '未命名服务' }}</strong>
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

          <section class="detail-section">
            <div class="section-title">
              <h2>用户评价</h2>
              <p>来自用户对机构服务、医疗响应、硬件和餐饮的反馈</p>
            </div>

            <div v-if="detail.reviews && detail.reviews.length > 0" class="review-list">
              <article
                v-for="review in detail.reviews"
                :key="review.id"
                class="review-card"
              >
                <div class="review-header">
                  <strong>用户 {{ review.userId || '匿名' }}</strong>
                  <span>{{ review.createdAt || '暂无时间' }}</span>
                </div>

                <div class="review-score-row">
                  <span>综合 {{ formatScore(review.ratingAvg) }}</span>
                  <span>医疗 {{ formatScore(review.ratingMedical) }}</span>
                  <span>硬件 {{ formatScore(review.ratingHardware) }}</span>
                  <span>餐饮 {{ formatScore(review.ratingFood) }}</span>
                  <span>护理 {{ formatScore(review.ratingService) }}</span>
                </div>

                <p>{{ review.content || '用户未填写文字评价。' }}</p>
              </article>
            </div>

            <p v-else class="empty-detail-text">
              暂无用户评价。
            </p>
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
                :src="image"
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

  try {
    const response = await axios.get(`/api/institutions/${id}`)
    detail.value = response.data

    await nextTick()
    initOrUpdateMiniMap()
  } catch (error) {
    console.error('加载机构详情失败：', error)
    errorMessage.value = '加载机构详情失败，请检查后端接口或数据库字段。'
  } finally {
    loading.value = false
  }
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
      crossOrigin: 'anonymous',
      wrapX: true
    })
  })
}

function initOrUpdateMiniMap() {
  if (!hasValidCoordinate.value || !detailMiniMapRef.value) {
    destroyMiniMap()
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

  setTimeout(() => {
    if (detailMiniMap) {
      detailMiniMap.updateSize()
    }
  }, 100)
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

function goBack() {
  router.push('/map')
}

function askAi() {
  alert(`AI 伴诊功能后续接入：${detail.value?.name || ''}`)
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
  () => {
    loadDetail()
  }
)

onMounted(() => {
  loadDetail()
})

onBeforeUnmount(() => {
  destroyMiniMap()
})
</script>