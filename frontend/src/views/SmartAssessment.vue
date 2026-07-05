<template>
  <div class="smart-assessment-page">
    <header class="assessment-topbar">
      <button class="back-btn" @click="goMap">← 返回地图</button>

      <div class="assessment-title">
        <strong>智能评估中心</strong>
        <span>医疗生命线分析 · 周边环境图层叠加 · 生活便利性与休闲生态评估</span>
      </div>

      <button class="primary-btn" @click="runAllAnalysis">
        综合分析
      </button>
    </header>

    <main class="assessment-layout">
      <section class="assessment-left">
        <div class="assessment-card">
          <div class="section-title">
            <h2>空间智能评估</h2>
            <p>
              请选择一个养老机构，系统会基于周边医疗、生活服务、公园景点等 POI 数据，
              生成医疗生命线、生活便利度和绿色休闲指数。
            </p>
          </div>

          <label class="assessment-form-item">
            选择养老机构
            <select
              v-model="selectedInstitutionId"
              @change="handleInstitutionSelectChange"
            >
              <option value="">请选择机构</option>
              <option
                v-for="item in institutions"
                :key="item.id"
                :value="String(item.id)"
              >
                {{ item.name }}（{{ item.district || '未知区县' }}）
              </option>
            </select>
          </label>

          <div v-if="selectedInstitution" class="selected-institution-box">
            <span>当前选择</span>
            <strong>{{ selectedInstitution.name }}</strong>
            <p>{{ selectedInstitution.address || '暂无地址' }}</p>
          </div>

          <div class="analysis-action-grid">
            <button class="primary-btn full-btn" @click="runMedicalAnalysis">
              医疗生命线分析
            </button>

            <button class="primary-btn full-btn" @click="runEnvironmentAnalysis">
              周边环境分析
            </button>

            <button class="secondary-btn full-btn" @click="runAllAnalysis">
              综合空间分析
            </button>
          </div>

          <div class="assessment-note">
            <strong>周边环境评价逻辑</strong>
            <p>
              生活便利性以 600 米“15 分钟生活圈”为核心，统计生活服务设施数量和类型多样性；
              休闲生态重点评估最近公园景点距离、3 公里内公园景点数量和类型多样性。
            </p>
          </div>
        </div>

        <div class="assessment-card">
          <div class="section-title">
            <h2>图层控制</h2>
          </div>

          <div class="assessment-layer-control">
            <label>
              <input
                type="checkbox"
                v-model="showMedicalLayer"
                @change="syncLayerVisibility"
              />
              <span class="legend-dot medical-dot"></span>
              医疗设施
            </label>

            <label>
              <input
                type="checkbox"
                v-model="showLifeLayer"
                @change="syncLayerVisibility"
              />
              <span class="legend-dot life-dot"></span>
              生活服务
            </label>

            <label>
              <input
                type="checkbox"
                v-model="showParkLayer"
                @change="syncLayerVisibility"
              />
              <span class="legend-dot park-dot"></span>
              公园景点
            </label>
          </div>
        </div>

        <div class="assessment-card">
          <div class="section-title">
            <h2>指标说明</h2>
          </div>

          <div class="indicator-explain-list">
            <div>
              <strong>生活便利度指数</strong>
              <p>综合 600 米生活圈内生活服务设施数量、设施类型多样性和最近生活服务距离。</p>
            </div>

            <div>
              <strong>绿色休闲指数</strong>
              <p>综合最近公园景点距离、3 公里内公园景点数量和类型多样性。</p>
            </div>

            <div>
              <strong>综合环境宜居指数</strong>
              <p>由生活便利度指数和绿色休闲指数加权合成，用于评价机构周边日常生活和休闲生态环境。</p>
            </div>
          </div>
        </div>
      </section>

      <section class="assessment-right">
        <div class="assessment-map-card">
          <div class="assessment-map-header">
            <div>
              <h2>空间分析地图</h2>
              <p v-if="selectedInstitution">
                当前选择机构：{{ selectedInstitution.name }}
              </p>
              <p v-else>
                地图已显示南京市养老机构点，可在地图上点击选择机构
              </p>
            </div>

            <div class="assessment-map-legend">
              <span><i class="legend-institution"></i>养老机构</span>
              <span><i class="legend-institution-selected"></i>已选机构</span>
              <span><i class="legend-medical"></i>医疗设施</span>
              <span><i class="legend-key-medical"></i>重点医院</span>
              <span><i class="legend-life"></i>生活服务</span>
              <span><i class="legend-park"></i>公园景点</span>
            </div>
          </div>

          <div class="assessment-map-body">
            <div ref="analysisMapRef" class="assessment-map"></div>

            <button
              v-if="medicalAnalysisResult"
              class="medical-drawer-toggle"
              @click.stop="toggleMedicalDrawer"
            >
              <span>{{ medicalDrawerOpen ? '收起' : '展开' }}</span>
              <strong>医疗设施 {{ nearbyMedicalCount }}</strong>
            </button>

            <button
              v-if="environmentAnalysisResult"
              class="environment-drawer-toggle"
              @click.stop="toggleEnvironmentDrawer"
            >
              <span>{{ environmentDrawerOpen ? '收起' : '展开' }}</span>
              <strong>周边环境 {{ nearbyEnvironmentCount }}</strong>
            </button>

            <aside
              v-if="medicalAnalysisResult"
              class="map-medical-drawer"
              :class="{ open: medicalDrawerOpen }"
              @click.stop
            >
              <div class="map-medical-drawer-header">
                <div>
                  <h3>周边医疗设施</h3>
                  <p>按距离从近到远展示，点击可在地图中定位。</p>
                </div>

                <button @click="medicalDrawerOpen = false">×</button>
              </div>

              <div
                v-if="!medicalAnalysisResult.nearbyMedicalPois || medicalAnalysisResult.nearbyMedicalPois.length === 0"
                class="map-medical-empty"
              >
                暂无周边医疗设施数据。
              </div>

              <div v-else class="medical-poi-list map-drawer-medical-list">
                <article
                  v-for="poi in medicalAnalysisResult.nearbyMedicalPois"
                  :key="poi.id"
                  class="medical-poi-card"
                  :class="{ active: selectedMapPoiKey === `medical-${poi.id}` }"
                  @click="focusMedicalPoi(poi, true)"
                >
                  <div>
                    <h3>{{ poi.name || '未命名医疗设施' }}</h3>
                    <p>{{ poi.address || '暂无地址' }}</p>
                  </div>

                  <div class="medical-poi-meta">
                    <span v-if="poi.keyMedical" class="key-medical-badge">重点</span>
                    <strong>{{ formatDistance(poi.distanceKm) }}</strong>
                    <small>{{ getMedicalType(poi) }}</small>
                  </div>
                </article>
              </div>
            </aside>

            <aside
              v-if="environmentAnalysisResult"
              class="map-environment-drawer"
              :class="{ open: environmentDrawerOpen }"
              @click.stop
            >
              <div class="map-medical-drawer-header">
                <div>
                  <h3>周边环境资源</h3>
                  <p>生活服务与公园景点图层叠加，点击可在地图中定位。</p>
                </div>

                <button @click="environmentDrawerOpen = false">×</button>
              </div>

              <div class="environment-drawer-tabs">
                <button
                  :class="{ active: environmentDrawerTab === 'life' }"
                  @click="environmentDrawerTab = 'life'"
                >
                  生活服务 {{ lifePoiCount }}
                </button>

                <button
                  :class="{ active: environmentDrawerTab === 'park' }"
                  @click="environmentDrawerTab = 'park'"
                >
                  公园景点 {{ parkPoiCount }}
                </button>
              </div>

              <div
                v-if="activeEnvironmentPoiList.length === 0"
                class="map-medical-empty"
              >
                暂无对应周边环境资源。
              </div>

              <div v-else class="medical-poi-list map-drawer-medical-list">
                <article
                  v-for="poi in activeEnvironmentPoiList"
                  :key="`${poi.category}-${poi.id}`"
                  class="medical-poi-card environment-poi-card"
                  :class="{ active: selectedMapPoiKey === getEnvironmentPoiKey(poi) }"
                  @click="focusEnvironmentPoi(poi, true)"
                >
                  <div>
                    <h3>{{ poi.name || '未命名资源' }}</h3>
                    <p>{{ poi.address || '暂无地址' }}</p>
                  </div>

                  <div class="medical-poi-meta">
                    <span
                      class="environment-badge"
                      :class="poi.category === 2 ? 'life' : 'park'"
                    >
                      {{ poi.categoryText }}
                    </span>
                    <strong>{{ formatDistance(poi.distanceKm) }}</strong>
                    <small>{{ getEnvironmentType(poi) }}</small>
                  </div>
                </article>
              </div>
            </aside>

            <div
              ref="analysisPopupRef"
              class="assessment-map-popup"
              v-show="mapPopupData"
              @click.stop
            >
              <button class="assessment-popup-close" @click="closeMapPopup">×</button>

              <div v-if="mapPopupData">
                <div class="assessment-popup-title">
                  {{ mapPopupData.title }}
                </div>

                <div
                  class="assessment-popup-badge"
                  :class="mapPopupData.badgeClass"
                >
                  {{ mapPopupData.badge }}
                </div>

                <div class="assessment-popup-row" v-if="mapPopupData.type">
                  <span>类型</span>
                  <strong>{{ mapPopupData.type }}</strong>
                </div>

                <div class="assessment-popup-row" v-if="mapPopupData.address">
                  <span>地址</span>
                  <strong>{{ mapPopupData.address }}</strong>
                </div>

                <div class="assessment-popup-row" v-if="mapPopupData.distance">
                  <span>距离</span>
                  <strong>{{ mapPopupData.distance }}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="assessment-state">
          正在进行空间分析...
        </div>

        <div v-else-if="errorMessage" class="assessment-state error">
          {{ errorMessage }}
        </div>

        <template v-if="medicalAnalysisResult || environmentAnalysisResult">
          <section class="assessment-summary-grid">
            <div v-if="medicalAnalysisResult" class="summary-card">
              <span>医疗资源可达性指数</span>
              <strong>{{ formatIndex(medicalAnalysisResult.medicalAccessibilityIndex) }}</strong>
              <small>{{ medicalAnalysisResult.medicalAccessibilityLevelText }}</small>
            </div>

            <div
              v-if="medicalAnalysisResult"
              class="summary-card"
              :class="medicalAnalysisResult.emergencyResponseLevel"
            >
              <span>急救响应能力</span>
              <strong>{{ medicalAnalysisResult.emergencyResponseText }}</strong>
              <small>预计 {{ formatMinutes(medicalAnalysisResult.estimatedEmergencyMinutes) }}</small>
            </div>

            <div v-if="environmentAnalysisResult" class="summary-card">
              <span>生活便利度指数</span>
              <strong>{{ formatIndex(environmentAnalysisResult.lifeConvenienceIndex) }}</strong>
              <small>{{ environmentAnalysisResult.lifeConvenienceLevelText }}</small>
            </div>

            <div v-if="environmentAnalysisResult" class="summary-card">
              <span>绿色休闲指数</span>
              <strong>{{ formatIndex(environmentAnalysisResult.greenLeisureIndex) }}</strong>
              <small>{{ environmentAnalysisResult.greenLeisureLevelText }}</small>
            </div>

            <div v-if="environmentAnalysisResult" class="summary-card">
              <span>综合环境宜居指数</span>
              <strong>{{ formatIndex(environmentAnalysisResult.environmentSuitabilityIndex) }}</strong>
              <small>{{ environmentAnalysisResult.environmentSuitabilityLevelText }}</small>
            </div>

            <div v-if="environmentAnalysisResult" class="summary-card">
              <span>最近公园景点</span>
              <strong>{{ environmentAnalysisResult.nearestParkName || '暂无' }}</strong>
              <small>{{ formatDistance(environmentAnalysisResult.nearestParkDistanceKm) }}</small>
            </div>
          </section>

          <section v-if="environmentAnalysisResult" class="assessment-card">
            <div class="section-title">
              <h2>周边环境圈层统计</h2>
              <p>统计养老机构周边不同范围内的生活服务设施与公园景点数量。</p>
            </div>

            <div class="radius-table environment-radius-table">
              <div class="radius-table-header">
                <span>范围</span>
                <span>生活服务</span>
                <span>公园景点</span>
              </div>

              <div
                v-for="item in environmentAnalysisResult.radiusSummaries"
                :key="item.radiusMeter"
                class="radius-table-row"
              >
                <span>{{ item.radiusText }}</span>
                <strong>{{ item.lifeServiceCount }}</strong>
                <strong>{{ item.parkCount }}</strong>
              </div>
            </div>
          </section>

          <section v-if="medicalAnalysisResult" class="assessment-card">
            <div class="section-title">
              <h2>医疗资源圈层统计</h2>
              <p>统计养老机构周边不同半径范围内的医疗设施和重点医院数量。</p>
            </div>

            <div class="radius-table">
              <div class="radius-table-header">
                <span>范围</span>
                <span>医疗设施数量</span>
                <span>重点医院数量</span>
              </div>

              <div
                v-for="item in medicalAnalysisResult.radiusSummaries"
                :key="item.radiusMeter"
                class="radius-table-row"
              >
                <span>{{ item.radiusText }}</span>
                <strong>{{ item.medicalCount }}</strong>
                <strong>{{ item.keyMedicalCount }}</strong>
              </div>
            </div>
          </section>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Map from 'ol/Map.js'
import View from 'ol/View.js'
import Overlay from 'ol/Overlay.js'
import TileLayer from 'ol/layer/Tile.js'
import VectorLayer from 'ol/layer/Vector.js'
import XYZ from 'ol/source/XYZ.js'
import VectorSource from 'ol/source/Vector.js'
import Feature from 'ol/Feature.js'
import Point from 'ol/geom/Point.js'
import { fromLonLat } from 'ol/proj.js'
import { boundingExtent } from 'ol/extent.js'
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style.js'
import axios from 'axios'

const router = useRouter()

const institutions = ref([])
const selectedInstitutionId = ref('')
const selectedMapPoiKey = ref('')

const medicalAnalysisResult = ref(null)
const environmentAnalysisResult = ref(null)

const loading = ref(false)
const errorMessage = ref('')

const analysisMapRef = ref(null)
const analysisPopupRef = ref(null)
const mapPopupData = ref(null)

const medicalDrawerOpen = ref(false)
const environmentDrawerOpen = ref(false)
const environmentDrawerTab = ref('life')

const showMedicalLayer = ref(true)
const showLifeLayer = ref(true)
const showParkLayer = ref(true)

let analysisMap = null
let institutionSource = null
let medicalPoiSource = null
let lifePoiSource = null
let parkPoiSource = null

let institutionLayer = null
let medicalPoiLayer = null
let lifePoiLayer = null
let parkPoiLayer = null

let popupOverlay = null
let hasFittedInitialInstitutionMap = false

const tiandituTk = '01c5845db0eb91889d42399c5a5b4f16'

const selectedInstitution = computed(() => {
  if (!selectedInstitutionId.value) {
    return null
  }

  return institutions.value.find(item => String(item.id) === String(selectedInstitutionId.value)) || null
})

const nearbyMedicalCount = computed(() => {
  if (!medicalAnalysisResult.value || !medicalAnalysisResult.value.nearbyMedicalPois) {
    return 0
  }

  return medicalAnalysisResult.value.nearbyMedicalPois.length
})

const lifePoiCount = computed(() => {
  if (!environmentAnalysisResult.value || !environmentAnalysisResult.value.lifeServicePois) {
    return 0
  }

  return environmentAnalysisResult.value.lifeServicePois.length
})

const parkPoiCount = computed(() => {
  if (!environmentAnalysisResult.value || !environmentAnalysisResult.value.parkPois) {
    return 0
  }

  return environmentAnalysisResult.value.parkPois.length
})

const nearbyEnvironmentCount = computed(() => {
  return lifePoiCount.value + parkPoiCount.value
})

const activeEnvironmentPoiList = computed(() => {
  if (!environmentAnalysisResult.value) {
    return []
  }

  if (environmentDrawerTab.value === 'life') {
    return environmentAnalysisResult.value.lifeServicePois || []
  }

  return environmentAnalysisResult.value.parkPois || []
})

const institutionStyle = new Style({
  image: new CircleStyle({
    radius: 8,
    fill: new Fill({ color: '#2E7D6B' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const selectedInstitutionStyle = new Style({
  image: new CircleStyle({
    radius: 12,
    fill: new Fill({ color: '#E8845B' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 3 })
  })
})

const medicalPoiStyle = new Style({
  image: new CircleStyle({
    radius: 7,
    fill: new Fill({ color: '#3A7BD5' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const keyMedicalPoiStyle = new Style({
  image: new CircleStyle({
    radius: 9,
    fill: new Fill({ color: '#D9534F' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const lifePoiStyle = new Style({
  image: new CircleStyle({
    radius: 7,
    fill: new Fill({ color: '#E8845B' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const parkPoiStyle = new Style({
  image: new CircleStyle({
    radius: 8,
    fill: new Fill({ color: '#3A9E5C' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const selectedPoiStyle = new Style({
  image: new CircleStyle({
    radius: 12,
    fill: new Fill({ color: '#7B61FF' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 3 })
  })
})

async function loadInstitutions() {
  try {
    const response = await axios.get('/api/institutions', {
      params: {
        limit: 1000
      }
    })

    institutions.value = response.data || []

    await nextTick()

    initOrUpdateMap()
    renderInstitutionMarkers()
  } catch (error) {
    console.error('加载机构列表失败：', error)
    errorMessage.value = '加载机构列表失败，请检查后端服务。'
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
      wrapX: true,
      transition: 0
    })
  })
}

function initOrUpdateMap() {
  if (!analysisMapRef.value) {
    return
  }

  if (analysisMap) {
    analysisMap.updateSize()
    return
  }

  institutionSource = new VectorSource()
  medicalPoiSource = new VectorSource()
  lifePoiSource = new VectorSource()
  parkPoiSource = new VectorSource()

  institutionLayer = new VectorLayer({
    source: institutionSource,
    zIndex: 40,
    style: feature => {
      const institution = feature.get('institution')

      if (institution && String(institution.id) === String(selectedInstitutionId.value)) {
        return selectedInstitutionStyle
      }

      return institutionStyle
    }
  })

  medicalPoiLayer = new VectorLayer({
    source: medicalPoiSource,
    zIndex: 30,
    visible: showMedicalLayer.value,
    style: feature => {
      if (feature.get('poiKey') === selectedMapPoiKey.value) {
        return selectedPoiStyle
      }

      if (feature.get('keyMedical')) {
        return keyMedicalPoiStyle
      }

      return medicalPoiStyle
    }
  })

  lifePoiLayer = new VectorLayer({
    source: lifePoiSource,
    zIndex: 28,
    visible: showLifeLayer.value,
    style: feature => {
      if (feature.get('poiKey') === selectedMapPoiKey.value) {
        return selectedPoiStyle
      }

      return lifePoiStyle
    }
  })

  parkPoiLayer = new VectorLayer({
    source: parkPoiSource,
    zIndex: 27,
    visible: showParkLayer.value,
    style: feature => {
      if (feature.get('poiKey') === selectedMapPoiKey.value) {
        return selectedPoiStyle
      }

      return parkPoiStyle
    }
  })

  analysisMap = new Map({
    target: analysisMapRef.value,
    layers: [
      createTiandituTileLayer('vec', 1, 1),
      createTiandituTileLayer('cva', 2, 1),
      parkPoiLayer,
      lifePoiLayer,
      medicalPoiLayer,
      institutionLayer
    ],
    view: new View({
      center: fromLonLat([118.7969, 32.0603]),
      zoom: 10,
      minZoom: 8,
      maxZoom: 18
    })
  })

  popupOverlay = new Overlay({
    element: analysisPopupRef.value,
    positioning: 'bottom-center',
    stopEvent: true,
    offset: [0, -14]
  })

  analysisMap.addOverlay(popupOverlay)

  analysisMap.on('singleclick', event => {
    const feature = analysisMap.forEachFeatureAtPixel(
      event.pixel,
      currentFeature => currentFeature,
      {
        hitTolerance: 6
      }
    )

    if (!feature) {
      closeMapPopup()
      return
    }

    const featureType = feature.get('featureType')

    if (featureType === 'institution') {
      const institution = feature.get('institution')
      if (institution) {
        selectInstitutionFromMap(institution, event.coordinate)
      }
      return
    }

    if (featureType === 'medical') {
      const poi = feature.get('poi')
      if (poi) {
        selectMedicalPoiFromMap(poi, event.coordinate)
      }
      return
    }

    if (featureType === 'life' || featureType === 'park') {
      const poi = feature.get('poi')
      if (poi) {
        selectEnvironmentPoiFromMap(poi, event.coordinate)
      }
    }
  })

  setTimeout(() => {
    if (analysisMap) {
      analysisMap.updateSize()
    }
  }, 200)
}

function renderInstitutionMarkers() {
  if (!institutionSource) {
    return
  }

  institutionSource.clear()

  const points = []

  institutions.value.forEach(item => {
    const lon = Number(item.lon)
    const lat = Number(item.lat)

    if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
      return
    }

    const coordinate = fromLonLat([lon, lat])
    points.push(coordinate)

    const feature = new Feature({
      geometry: new Point(coordinate),
      featureType: 'institution',
      institution: item
    })

    feature.setId(`institution-${item.id}`)
    institutionSource.addFeature(feature)
  })

  if (!hasFittedInitialInstitutionMap && points.length > 0 && analysisMap) {
    hasFittedInitialInstitutionMap = true

    if (points.length === 1) {
      analysisMap.getView().setCenter(points[0])
      analysisMap.getView().setZoom(13)
    } else {
      analysisMap.getView().fit(boundingExtent(points), {
        padding: [80, 80, 80, 80],
        maxZoom: 11,
        duration: 300
      })
    }
  }

  if (institutionLayer) {
    institutionLayer.changed()
  }
}

function handleInstitutionSelectChange() {
  closeMapPopup()
  selectedMapPoiKey.value = ''

  if (!selectedInstitutionId.value) {
    refreshAllVectorLayers()
    return
  }

  const institution = selectedInstitution.value

  if (institution) {
    selectInstitution(institution, true, true)
  }
}

function selectInstitutionFromMap(institution, coordinate) {
  selectedInstitutionId.value = String(institution.id)
  selectedMapPoiKey.value = ''
  refreshAllVectorLayers()
  showInstitutionPopup(institution, coordinate)
}

function selectInstitution(institution, shouldFocus = true, shouldShowPopup = false) {
  if (!institution) {
    return
  }

  selectedInstitutionId.value = String(institution.id)
  selectedMapPoiKey.value = ''

  refreshAllVectorLayers()

  const lon = Number(institution.lon)
  const lat = Number(institution.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat) || !analysisMap) {
    return
  }

  const coordinate = fromLonLat([lon, lat])

  if (shouldFocus) {
    analysisMap.getView().animate({
      center: coordinate,
      zoom: 14,
      duration: 400
    })
  }

  if (shouldShowPopup) {
    showInstitutionPopup(institution, coordinate)
  }
}

function showInstitutionPopup(institution, coordinate) {
  mapPopupData.value = {
    kind: 'institution',
    title: institution.name || '未命名养老机构',
    badge: '养老机构',
    badgeClass: 'institution',
    address: institution.address || '暂无地址',
    type: institution.institutionCategoryText || '',
    distance: ''
  }

  if (popupOverlay) {
    popupOverlay.setPosition(coordinate)
  }
}

async function runMedicalAnalysis() {
  if (!selectedInstitutionId.value) {
    errorMessage.value = '请先选择一个养老机构。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  closeMapPopup()

  try {
    const response = await axios.get(`/api/analysis/medical-lifeline/${selectedInstitutionId.value}`)
    medicalAnalysisResult.value = response.data
    medicalDrawerOpen.value = true
    environmentDrawerOpen.value = false

    await nextTick()

    initOrUpdateMap()
    renderMedicalPois()
    focusAnalysisExtent()
  } catch (error) {
    console.error('医疗生命线分析失败：', error)
    errorMessage.value = '医疗生命线分析失败，请检查后端接口、POI 数据或空间字段。'
  } finally {
    loading.value = false
  }
}

async function runEnvironmentAnalysis() {
  if (!selectedInstitutionId.value) {
    errorMessage.value = '请先选择一个养老机构。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  closeMapPopup()

  try {
    const response = await axios.get(`/api/analysis/environment/${selectedInstitutionId.value}`)
    environmentAnalysisResult.value = response.data
    environmentDrawerOpen.value = true
    medicalDrawerOpen.value = false
    environmentDrawerTab.value = 'life'

    await nextTick()

    initOrUpdateMap()
    renderEnvironmentPois()
    focusAnalysisExtent()
  } catch (error) {
    console.error('周边环境分析失败：', error)
    errorMessage.value = '周边环境分析失败，请检查后端接口、POI 数据或空间字段。'
  } finally {
    loading.value = false
  }
}

async function runAllAnalysis() {
  if (!selectedInstitutionId.value) {
    errorMessage.value = '请先选择一个养老机构。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  closeMapPopup()

  try {
    const [medicalResponse, environmentResponse] = await Promise.all([
      axios.get(`/api/analysis/medical-lifeline/${selectedInstitutionId.value}`),
      axios.get(`/api/analysis/environment/${selectedInstitutionId.value}`)
    ])

    medicalAnalysisResult.value = medicalResponse.data
    environmentAnalysisResult.value = environmentResponse.data

    medicalDrawerOpen.value = false
    environmentDrawerOpen.value = true
    environmentDrawerTab.value = 'life'

    await nextTick()

    initOrUpdateMap()
    renderMedicalPois()
    renderEnvironmentPois()
    focusAnalysisExtent()
  } catch (error) {
    console.error('综合空间分析失败：', error)
    errorMessage.value = '综合空间分析失败，请检查后端接口、POI 数据或空间字段。'
  } finally {
    loading.value = false
  }
}

function renderMedicalPois() {
  if (!medicalPoiSource || !medicalAnalysisResult.value) {
    return
  }

  medicalPoiSource.clear()

  const list = medicalAnalysisResult.value.nearbyMedicalPois || []

  list.forEach(poi => {
    const lon = Number(poi.lon)
    const lat = Number(poi.lat)

    if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
      return
    }

    const coordinate = fromLonLat([lon, lat])
    const poiKey = `medical-${poi.id}`

    const feature = new Feature({
      geometry: new Point(coordinate),
      featureType: 'medical',
      keyMedical: poi.keyMedical,
      poiKey,
      poi
    })

    feature.setId(poiKey)
    medicalPoiSource.addFeature(feature)
  })

  refreshAllVectorLayers()
}

function renderEnvironmentPois() {
  if (!environmentAnalysisResult.value) {
    return
  }

  if (lifePoiSource) {
    lifePoiSource.clear()
  }

  if (parkPoiSource) {
    parkPoiSource.clear()
  }

  const lifeList = environmentAnalysisResult.value.lifeServicePois || []
  const parkList = environmentAnalysisResult.value.parkPois || []

  lifeList.forEach(poi => {
    addEnvironmentFeature(poi, 'life', lifePoiSource)
  })

  parkList.forEach(poi => {
    addEnvironmentFeature(poi, 'park', parkPoiSource)
  })

  refreshAllVectorLayers()
}

function addEnvironmentFeature(poi, featureType, source) {
  if (!source || !poi) {
    return
  }

  const lon = Number(poi.lon)
  const lat = Number(poi.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
    return
  }

  const coordinate = fromLonLat([lon, lat])
  const poiKey = getEnvironmentPoiKey(poi)

  const feature = new Feature({
    geometry: new Point(coordinate),
    featureType,
    poiKey,
    poi
  })

  feature.setId(poiKey)
  source.addFeature(feature)
}

function focusAnalysisExtent() {
  if (!analysisMap) {
    return
  }

  const points = []

  const institution = selectedInstitution.value

  if (institution) {
    const lon = Number(institution.lon)
    const lat = Number(institution.lat)

    if (Number.isFinite(lon) && Number.isFinite(lat)) {
      points.push(fromLonLat([lon, lat]))
    }
  }

  if (medicalAnalysisResult.value) {
    const medicalList = medicalAnalysisResult.value.nearbyMedicalPois || []
    collectPoiPoints(medicalList, points)
  }

  if (environmentAnalysisResult.value) {
    collectPoiPoints(environmentAnalysisResult.value.lifeServicePois || [], points)
    collectPoiPoints(environmentAnalysisResult.value.parkPois || [], points)
  }

  if (points.length === 1) {
    analysisMap.getView().animate({
      center: points[0],
      zoom: 14,
      duration: 400
    })
  }

  if (points.length > 1) {
    analysisMap.getView().fit(boundingExtent(points), {
      padding: [90, 420, 90, 90],
      duration: 500,
      maxZoom: 15
    })
  }

  analysisMap.updateSize()
}

function collectPoiPoints(list, points) {
  list.forEach(poi => {
    const lon = Number(poi.lon)
    const lat = Number(poi.lat)

    if (Number.isFinite(lon) && Number.isFinite(lat)) {
      points.push(fromLonLat([lon, lat]))
    }
  })
}

function selectMedicalPoiFromMap(poi, coordinate) {
  selectedMapPoiKey.value = `medical-${poi.id}`
  refreshAllVectorLayers()
  showMedicalPoiPopup(poi, coordinate)
}

function focusMedicalPoi(poi, shouldShowPopup = true) {
  if (!analysisMap || !poi) {
    return
  }

  const lon = Number(poi.lon)
  const lat = Number(poi.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
    return
  }

  const coordinate = fromLonLat([lon, lat])
  selectedMapPoiKey.value = `medical-${poi.id}`

  refreshAllVectorLayers()

  analysisMap.getView().animate({
    center: coordinate,
    zoom: 16,
    duration: 400
  })

  if (shouldShowPopup) {
    showMedicalPoiPopup(poi, coordinate)
  }
}

function showMedicalPoiPopup(poi, coordinate) {
  mapPopupData.value = {
    kind: 'medical',
    title: poi.name || '未命名医疗设施',
    badge: poi.keyMedical ? '重点医院' : '医疗设施',
    badgeClass: poi.keyMedical ? 'key-medical' : 'medical',
    address: poi.address || '暂无地址',
    type: getMedicalType(poi),
    distance: formatDistance(poi.distanceKm)
  }

  if (popupOverlay) {
    popupOverlay.setPosition(coordinate)
  }
}

function selectEnvironmentPoiFromMap(poi, coordinate) {
  selectedMapPoiKey.value = getEnvironmentPoiKey(poi)
  refreshAllVectorLayers()
  showEnvironmentPoiPopup(poi, coordinate)
}

function focusEnvironmentPoi(poi, shouldShowPopup = true) {
  if (!analysisMap || !poi) {
    return
  }

  const lon = Number(poi.lon)
  const lat = Number(poi.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
    return
  }

  const coordinate = fromLonLat([lon, lat])
  selectedMapPoiKey.value = getEnvironmentPoiKey(poi)

  refreshAllVectorLayers()

  analysisMap.getView().animate({
    center: coordinate,
    zoom: 16,
    duration: 400
  })

  if (shouldShowPopup) {
    showEnvironmentPoiPopup(poi, coordinate)
  }
}

function showEnvironmentPoiPopup(poi, coordinate) {
  const isLife = poi.category === 2

  mapPopupData.value = {
    kind: isLife ? 'life' : 'park',
    title: poi.name || '未命名资源',
    badge: poi.categoryText || (isLife ? '生活服务' : '公园景点'),
    badgeClass: isLife ? 'life' : 'park',
    address: poi.address || '暂无地址',
    type: getEnvironmentType(poi),
    distance: formatDistance(poi.distanceKm)
  }

  if (popupOverlay) {
    popupOverlay.setPosition(coordinate)
  }
}

function closeMapPopup() {
  mapPopupData.value = null

  if (popupOverlay) {
    popupOverlay.setPosition(undefined)
  }
}

function refreshAllVectorLayers() {
  if (institutionLayer) {
    institutionLayer.changed()
  }

  if (medicalPoiLayer) {
    medicalPoiLayer.changed()
  }

  if (lifePoiLayer) {
    lifePoiLayer.changed()
  }

  if (parkPoiLayer) {
    parkPoiLayer.changed()
  }
}

function syncLayerVisibility() {
  if (medicalPoiLayer) {
    medicalPoiLayer.setVisible(showMedicalLayer.value)
  }

  if (lifePoiLayer) {
    lifePoiLayer.setVisible(showLifeLayer.value)
  }

  if (parkPoiLayer) {
    parkPoiLayer.setVisible(showParkLayer.value)
  }

  closeMapPopup()
}

function toggleMedicalDrawer() {
  medicalDrawerOpen.value = !medicalDrawerOpen.value

  if (medicalDrawerOpen.value) {
    environmentDrawerOpen.value = false
  }
}

function toggleEnvironmentDrawer() {
  environmentDrawerOpen.value = !environmentDrawerOpen.value

  if (environmentDrawerOpen.value) {
    medicalDrawerOpen.value = false
  }
}

function getEnvironmentPoiKey(poi) {
  return `environment-${poi.category}-${poi.id}`
}

function getMedicalType(poi) {
  if (!poi) {
    return '医疗设施'
  }

  return poi.poiSmall || poi.typeFull || poi.poiMid || '医疗设施'
}

function getEnvironmentType(poi) {
  if (!poi) {
    return '周边资源'
  }

  return poi.poiSmall || poi.typeFull || poi.poiMid || poi.categoryText || '周边资源'
}

function goMap() {
  router.push('/map')
}

function formatIndex(value) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return Number(value).toFixed(3)
}

function formatDistance(value) {
  if (value === null || value === undefined) {
    return '暂无距离'
  }

  return `${Number(value).toFixed(2)} km`
}

function formatMinutes(value) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return `${Number(value).toFixed(1)} 分钟`
}

onMounted(async () => {
  await nextTick()
  initOrUpdateMap()
  await loadInstitutions()
})

onBeforeUnmount(() => {
  if (analysisMap) {
    analysisMap.setTarget(undefined)
    analysisMap = null
  }
})
</script>