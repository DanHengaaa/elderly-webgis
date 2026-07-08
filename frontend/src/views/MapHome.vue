<template>
  <div class="map-page">
    <header class="app-header">
      <div class="header-left">
        <div class="logo-mark">养</div>
        <div class="brand-text">
          <div class="brand-title">养老服务智慧决策 WebGIS 平台</div>
          <div class="brand-subtitle">面向银发经济的养老资源空间适配评估系统</div>
        </div>
      </div>

      <nav class="desktop-nav">
        <button class="nav-item active">地图找院</button>
        <button class="nav-item" @click="router.push('/assessment')">智能评估</button>
        <button class="nav-item" @click="router.push('/recommendations')">我的推荐</button>
        <button class="nav-item" @click="router.push('/ai-assistant')">AI 伴诊</button>
        <button class="nav-item" @click="router.push('/community')">社区</button>
      </nav>

      <div class="header-right">
        <button class="elder-mode-btn" @click="toggleElderMode">
          {{ elderMode ? '退出老年模式' : '老年模式' }}
        </button>
      </div>

      <div class="user-menu">
  <button class="user-btn" @click="handleUserClick">
    {{ currentUser ? (currentUser.nickname || currentUser.username) : '用户' }}
  </button>

  <div v-if="showUserMenu && currentUser" class="user-dropdown">
    <div class="user-info">
      <strong>{{ currentUser.nickname || currentUser.username }}</strong>
      <span>{{ currentUser.roleName || currentUser.roleCode }}</span>
    </div>

    <button v-if="currentUser.roleCode === 'ADMIN'" @click="goAdmin">
      管理员后台
    </button>

    <button v-if="currentUser.roleCode === 'INSTITUTION'" @click="goInstitutionConsole">
      机构工作台
    </button>

    <button v-if="currentUser.roleCode === 'CUSTOMER'" @click="goCustomerCenter">
      个人中心
    </button>

    <button @click="logout">
      退出登录
    </button>
  </div>
</div>


    </header>

    <section class="search-toolbar">
      <div class="search-main">
        <input
          v-model="filters.keyword"
          class="main-search-input"
          placeholder="搜索养老机构名称或地址"
          @keyup.enter="loadInstitutions"
        />
        <button class="voice-btn" title="语音输入">🎙</button>
        <button class="primary-btn" @click="loadInstitutions">搜索</button>
      </div>

      <div class="origin-input-group">
        <input
          v-model="originAddress"
          class="origin-input"
          placeholder="设置居住地 / 子女通勤起点"
        />
      </div>

      <div class="travel-mode-group">
        <button
          v-for="mode in travelModes"
          :key="mode.value"
          class="pill-btn"
          :class="{ active: activeTravelMode === mode.value }"
          @click="activeTravelMode = mode.value"
        >
          {{ mode.label }}
        </button>
      </div>

      <div class="isochrone-group">
        <button
          v-for="item in isochroneOptions"
          :key="item.value"
          class="pill-btn"
          :class="{ active: activeIsochrone === item.value }"
          @click="activeIsochrone = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <main id="map" class="map-container"></main>

<!-- 探视可达性分析控件 -->
<div class="visit-accessibility-widget" @click.stop>
  <div class="visit-widget-header">
    <div>
      <h3>探视可达性分析</h3>
      <p>设置探视起点，生成 15 / 30 / 60 分钟等时圈</p>
    </div>

    <button
      class="visit-pick-btn"
      :class="{ active: visitPickMode }"
      @click="toggleVisitPickMode"
    >
      {{ visitPickMode ? '正在选点' : '地图选点' }}
    </button>
  </div>

  <div class="visit-search-row">
    <input
      v-model="visitSearchKeyword"
      placeholder="输入起点，例如：河海大学 / 新街口"
      @keyup.enter="searchVisitStart"
    />

    <button class="primary-btn" @click="searchVisitStart">
      搜索
    </button>
  </div>

  <div v-if="visitStart" class="visit-start-box">
    <span>当前起点</span>
    <strong>{{ visitStart.name }}</strong>
    <p>{{ visitStart.lon.toFixed(6) }}, {{ visitStart.lat.toFixed(6) }}</p>
  </div>

  <button
    class="primary-btn full-btn"
    :disabled="!visitStart || visitLoading"
    @click="runVisitAccessibilityAnalysis"
  >
    {{ visitLoading ? '正在分析...' : '生成等时圈' }}
  </button>

  <div v-if="visitErrorMessage" class="visit-error">
    {{ visitErrorMessage }}
  </div>

  <div v-if="visitAnalysisResult" class="visit-result-box">
    <div class="visit-ring-legend">
      <span><i class="ring-15"></i>15分钟</span>
      <span><i class="ring-30"></i>30分钟</span>
      <span><i class="ring-60"></i>60分钟</span>
    </div>

    <p>
      可达机构：
      <strong>{{ visitAnalysisResult.reachableInstitutions.length }}</strong>
      家
    </p>

    <div class="visit-reachable-list">
      <article
        v-for="item in visitAnalysisResult.reachableInstitutions.slice(0, 6)"
        :key="item.id"
        @click="focusReachableInstitution(item)"
      >
        <strong>{{ item.name }}</strong>
        <span>{{ item.estimatedMinutes }} 分钟 · {{ item.reachabilityLevelText }}</span>
      </article>
    </div>
  </div>
</div>


<!-- 地图浮动图层控制按钮 -->
<!-- 地图浮动图层控制按钮：只控制可选 POI 图层 -->
<div class="map-layer-control" @click.stop>
  <button class="map-layer-toggle-btn" @click="layerDropdownOpen = !layerDropdownOpen">
    <span class="layer-icon">▦</span>
    <span>图层</span>
    <span v-if="getActiveLayerCount() > 0" class="layer-count-badge">
      {{ getActiveLayerCount() }}
    </span>
  </button>

  <div v-if="layerDropdownOpen" class="map-layer-dropdown">
    <div class="layer-dropdown-header">
      <div>
        <h3>图层控制</h3>
        <p>选择需要叠加显示的周边资源图层</p>
      </div>

      <button class="layer-dropdown-close" @click="layerDropdownOpen = false">
        ×
      </button>
    </div>

    <div class="layer-dropdown-section">
      <h4>周边资源图层</h4>

      <label class="layer-dropdown-item">
        <input
          type="checkbox"
          v-model="showMedicalPoiLayer"
          @change="togglePoiLayer('medical')"
        />
        <span class="legend-dot medical-dot"></span>
        <span class="layer-dropdown-text">
          <strong>医疗设施</strong>
          <small>医院、卫生院、专科医疗等</small>
        </span>
      </label>

      <label class="layer-dropdown-item">
        <input
          type="checkbox"
          v-model="showLifePoiLayer"
          @change="togglePoiLayer('life')"
        />
        <span class="legend-dot life-dot"></span>
        <span class="layer-dropdown-text">
          <strong>生活服务</strong>
          <small>超市、药店、银行、社区服务等</small>
        </span>
      </label>

      <label class="layer-dropdown-item">
        <input
          type="checkbox"
          v-model="showParkPoiLayer"
          @change="togglePoiLayer('park')"
        />
        <span class="legend-dot park-dot"></span>
        <span class="layer-dropdown-text">
          <strong>公园景点</strong>
          <small>公园、景区、绿地休闲资源</small>
        </span>
      </label>
    </div>

    <div class="layer-dropdown-footer">
      <button class="secondary-btn" @click="hideAllPoiLayers">
        隐藏全部
      </button>
      <button class="primary-btn" @click="showAllPoiLayers">
        显示全部
      </button>
    </div>
  </div>
</div>

    <!-- POI 点击弹窗 -->
    <div id="poi-popup" class="poi-popup">
      <button class="poi-popup-close" @click="closePoiPopup">×</button>

      <div v-if="selectedPoi" class="poi-popup-content">
        <div class="poi-popup-title">
          {{ selectedPoi.name || '未命名 POI' }}
        </div>

        <div class="poi-popup-category" :class="selectedPoi.categoryClass">
          {{ selectedPoi.categoryText }}
        </div>

        <div class="poi-popup-row">
          <span class="poi-popup-label">地址</span>
          <span class="poi-popup-value">
            {{ selectedPoi.address || '暂无地址' }}
          </span>
        </div>

        <div class="poi-popup-row" v-if="selectedPoi.typeFull">
          <span class="poi-popup-label">类型</span>
          <span class="poi-popup-value">
            {{ selectedPoi.typeFull }}
          </span>
        </div>

        <div class="poi-popup-row" v-if="selectedPoi.district">
          <span class="poi-popup-label">区县</span>
          <span class="poi-popup-value">
            {{ selectedPoi.district }}
          </span>
        </div>
      </div>
    </div>

    <aside
      class="side-panel filter-panel"
      :class="{ 'mobile-open': filterPanelOpen }"
    >
      <div class="panel-header">
        <div>
          <h2>机构筛选</h2>
<p>按区县、性质、价位和护理类型筛选机构</p>
        </div>
        <button class="panel-close-btn" @click="filterPanelOpen = false">×</button>
      </div>

      <div class="panel-section">
        <h3>机构筛选</h3>

        <label class="form-item">
          区县
          <select v-model="filters.district" @change="loadInstitutions">
            <option value="">全部区县</option>
            <option value="鼓楼区">鼓楼区</option>
            <option value="玄武区">玄武区</option>
            <option value="秦淮区">秦淮区</option>
            <option value="建邺区">建邺区</option>
            <option value="栖霞区">栖霞区</option>
            <option value="江宁区">江宁区</option>
            <option value="浦口区">浦口区</option>
            <option value="六合区">六合区</option>
            <option value="溧水区">溧水区</option>
            <option value="高淳区">高淳区</option>
          </select>
        </label>

        <label class="form-item">
          机构性质
          <select v-model="filters.institutionCategory" @change="loadInstitutions">
            <option value="">全部性质</option>
            <option value="1">公办公营</option>
            <option value="2">公办民营</option>
            <option value="3">民营</option>
          </select>
        </label>

        <label class="form-item">
          价格档位
          <select v-model="filters.priceTier" @change="loadInstitutions">
            <option value="">全部价位</option>
            <option value="1">经济型</option>
            <option value="2">标准型</option>
            <option value="3">高端型</option>
          </select>
        </label>

        <label class="form-item">
          护理类型
          <select v-model="filters.serviceType" @change="loadInstitutions">
            <option value="">全部护理类型</option>
            <option value="自理">自理</option>
            <option value="半失能">半失能</option>
            <option value="失能">失能</option>
            <option value="失智">失智</option>
            <option value="认知症照护">认知症照护</option>
            <option value="术后康复">术后康复</option>
          </select>
        </label>

        <label class="check-item">
          <input
            type="checkbox"
            v-model="filters.hasAvailableBeds"
            @change="loadInstitutions"
          />
          仅看有空余床位
        </label>

        <button class="secondary-btn full-btn" @click="resetFilters">
          重置筛选
        </button>
      </div>

      
     
    </aside>

    <aside
      class="side-panel result-panel"
      :class="{ 'mobile-open': listPanelOpen }"
    >
      <div class="panel-header">
        <div>
          <h2>机构列表</h2>
          <p>共找到 {{ institutions.length }} 家机构</p>
        </div>
        <button class="panel-close-btn" @click="listPanelOpen = false">×</button>
      </div>

      <div v-if="loading" class="state-box">
        正在加载养老机构数据...
      </div>

      <div v-else-if="institutions.length === 0" class="state-box">
        暂无符合条件的机构，请调整筛选条件
      </div>

      <div v-else class="institution-list">
        <article
          v-for="item in institutions"
          :key="item.id"
          class="institution-card"
          :class="{ active: selectedInstitution && selectedInstitution.id === item.id }"
          @click="focusInstitution(item)"
        >
          <div class="card-cover">
            <img
              v-if="item.coverImageUrl"
              :src="item.coverImageUrl"
              alt="机构实景图"
            />
            <div v-else class="cover-placeholder">
              机构实景
            </div>
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h3>{{ item.name }}</h3>
              <span class="score-badge">{{ formatScore(item.ratingAvg) }}</span>
            </div>

            <div class="tag-row">
              <span class="tag primary">{{ item.institutionCategoryText || '未知性质' }}</span>
              <span v-if="item.gradeLevel" class="tag">{{ item.gradeLevel }}</span>
              <span class="tag">{{ item.priceTierText || '未设置价位' }}</span>
            </div>

            <div class="info-grid">
              <div>
                <span class="info-label">区县</span>
                <strong>{{ item.district || '未知' }}</strong>
              </div>
              <div>
                <span class="info-label">空余床位</span>
                <strong>{{ item.availableBeds ?? '-' }} / {{ item.totalBeds ?? '-' }}</strong>
              </div>
              <div>
                <span class="info-label">月费</span>
                <strong>{{ formatFee(item.monthlyFeeBase) }}</strong>
              </div>
              <div>
                <span class="info-label">可达性</span>
                <strong class="accessibility-text">待计算</strong>
              </div>
            </div>

            <div class="address-line">
              {{ item.address || '暂无详细地址' }}
            </div>

            <div class="card-actions">
              <button class="small-btn primary-small" @click.stop="focusInstitution(item)">
                地图定位
              </button>
              <button class="small-btn" @click.stop="openInstitutionDetail(item)">
  查看详情
</button>
              <button class="small-btn" @click.stop="addToCompare(item)">
                加入对比
              </button>
            </div>
          </div>
        </article>
      </div>
    </aside>

    <section
      v-if="selectedInstitution"
      class="selected-card"
    >
      <div class="selected-header">
        <div>
          <h3>{{ selectedInstitution.name }}</h3>
          <p>{{ selectedInstitution.address || '暂无地址' }}</p>
        </div>
        <button @click="selectedInstitution = null">×</button>
      </div>

      <div class="selected-meta">
        <span>{{ selectedInstitution.institutionCategoryText || '未知性质' }}</span>
        <span>{{ selectedInstitution.gradeLevel || '暂无等级' }}</span>
        <span>{{ formatFee(selectedInstitution.monthlyFeeBase) }}</span>
      </div>

      <div class="selected-actions">
  <button class="primary-btn">问 AI 伴诊</button>
  <button class="secondary-btn">加入对比</button>
  <button class="secondary-btn" @click="openInstitutionDetail(selectedInstitution)">
    完整详情
  </button>
</div>
    </section>

    <div class="mobile-actions">
      <button @click="filterPanelOpen = true">筛选图层</button>
      <button @click="listPanelOpen = true">机构列表 {{ institutions.length }}</button>
      <button @click="toggleCommunityHeatLayer">
  社区热度
</button>
    </div>
  </div>
</template>

<script setup>
import GeoJSON from 'ol/format/GeoJSON.js'
import { nextTick, onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import Map from 'ol/Map.js'
import View from 'ol/View.js'
import Overlay from 'ol/Overlay.js'
import TileLayer from 'ol/layer/Tile.js'
import VectorLayer from 'ol/layer/Vector.js'
import XYZ from 'ol/source/XYZ.js'
import TileWMS from 'ol/source/TileWMS.js'
import VectorSource from 'ol/source/Vector.js'
import Feature from 'ol/Feature.js'
import Point from 'ol/geom/Point.js'
import { fromLonLat, toLonLat } from 'ol/proj.js'
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style.js'
import axios from 'axios'
import { getCurrentUser, clearAuth } from '../utils/auth'
const institutions = ref([])
const router = useRouter()
const selectedInstitution = ref(null)
const selectedPoi = ref(null)
const loading = ref(false)

const filterPanelOpen = ref(false)
const listPanelOpen = ref(false)
const elderMode = ref(false)

const originAddress = ref('')
const activeTravelMode = ref('drive')
const activeIsochrone = ref(30)

const showTiandituBase = ref(true)
const showInstitutionWms = ref(true)

const showMedicalPoiLayer = ref(false)
const showLifePoiLayer = ref(false)
const showParkPoiLayer = ref(false)

const layerDropdownOpen = ref(false)

const visitSearchKeyword = ref('')
const visitStart = ref(null)
const visitPickMode = ref(false)
const visitLoading = ref(false)
const visitErrorMessage = ref('')
const visitAnalysisResult = ref(null)

let visitStartLayer = null
let visitIsochroneLayer = null
let visitReachableInstitutionLayer = null


const geoJsonFormat = new GeoJSON()




let map = null
let markerSource = null
let tiandituVectorLayer = null
let tiandituLabelLayer = null
let institutionWmsLayer = null

let medicalPoiLayer = null
let lifePoiLayer = null
let parkPoiLayer = null

let poiPopupOverlay = null
let selectedFeature = null
let communityHeatLayer = null
let communityHeatVisible = false


const tiandituTk = '01c5845db0eb91889d42399c5a5b4f16'

// 机构图层名。如果你使用视图，就改成 elderly:v_institutions_wgs84
const geoserverInstitutionLayerName = 'elderly:institutions'

// POI 图层名。如果 GeoServer 里发布的是 elderly:poi，就改成 elderly:poi
const geoserverPoiLayerName = 'elderly:v_poi_wgs84'

// GeoServer 样式名。你已经创建了 poi_category_style，就保持这个。
const geoserverPoiStyleName = 'poi_category_style'

const filters = reactive({
  keyword: '',
  district: '',
  institutionCategory: '',
  priceTier: '',
  serviceType: '',
  hasAvailableBeds: false
})

const travelModes = [
  { label: '驾车', value: 'drive' },
  { label: '公交', value: 'bus' },
  { label: '步行', value: 'walk' }
]

const isochroneOptions = [
  { label: '15分钟', value: 15 },
  { label: '30分钟', value: 30 },
  { label: '60分钟', value: 60 }
]

const poiLayerConfig = {
  medical: {
    category: 1,
    visibleRef: showMedicalPoiLayer,
    layerGetter: () => medicalPoiLayer
  },
  life: {
    category: 2,
    visibleRef: showLifePoiLayer,
    layerGetter: () => lifePoiLayer
  },
  park: {
    category: 3,
    visibleRef: showParkPoiLayer,
    layerGetter: () => parkPoiLayer
  }
}

const normalStyle = new Style({
  image: new CircleStyle({
    radius: 8,
    fill: new Fill({ color: '#2E7D6B' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 2 })
  })
})

const selectedStyle = new Style({
  image: new CircleStyle({
    radius: 12,
    fill: new Fill({ color: '#E8845B' }),
    stroke: new Stroke({ color: '#FFFFFF', width: 3 })
  })
})

const showUserMenu = ref(false)

const currentUser = computed(() => {
  return getCurrentUser()
})

function handleUserClick() {
  if (!currentUser.value) {
    router.push('/login')
    return
  }

  showUserMenu.value = !showUserMenu.value
}

function goAdmin() {
  showUserMenu.value = false
  router.push('/admin')
}

function goInstitutionConsole() {
  showUserMenu.value = false
  router.push('/institution-console')
}

function goCustomerCenter() {
  showUserMenu.value = false
  router.push('/map')
}

function logout() {
  clearAuth()
  showUserMenu.value = false
  router.push('/login')
}


function buildTiandituUrls(layerName) {
  const subdomains = ['0', '1', '2', '3', '4', '5', '6', '7']

  return subdomains.map(subdomain => {
    return `https://t${subdomain}.tianditu.gov.cn/${layerName}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layerName}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${tiandituTk}`
  })
}

function createTiandituTileLayer(layerName, zIndex, opacity = 1) {
  return new TileLayer({
    visible: showTiandituBase.value,
    opacity,
    zIndex,
    source: new XYZ({
      urls: buildTiandituUrls(layerName),
      wrapX: true,
      transition: 0
    })
  })
}

function createPoiWmsLayer(type) {
  const config = poiLayerConfig[type]

  return new TileLayer({
    visible: false,
    opacity: 0.9,
    zIndex: 20,
    source: new TileWMS({
      url: '/geoserver/elderly/wms',
      params: {
        LAYERS: geoserverPoiLayerName,
        TILED: true,
        CQL_FILTER: `category=${config.category}`,
        STYLES: geoserverPoiStyleName
      },
      serverType: 'geoserver',
      crossOrigin: 'anonymous'
    })
  })
}

async function loadCommunityHeatLayer() {
  try {
    const response = await axios.get('/api/community/heat')
    const points = response.data || []

    const features = points
      .filter(item => item.lon && item.lat)
      .map(item => {
        const feature = new Feature({
          geometry: new Point(fromLonLat([Number(item.lon), Number(item.lat)])),
          institutionId: item.institutionId,
          institutionName: item.institutionName,
          postCount: item.postCount,
          avgRating: item.avgRating
        })

        return feature
      })

    const source = new VectorSource({
      features
    })

    communityHeatLayer = new VectorLayer({
      source,
      zIndex: 80,
      visible: false,
      style: feature => {
        const count = Number(feature.get('postCount') || 1)
        const radius = Math.min(10 + count * 3, 28)

        return new Style({
          image: new CircleStyle({
            radius,
            fill: new Fill({
              color: 'rgba(232, 132, 91, 0.35)'
            }),
            stroke: new Stroke({
              color: 'rgba(232, 132, 91, 0.95)',
              width: 2
            })
          }),
          text: new Text({
            text: String(count),
            fill: new Fill({
              color: '#ffffff'
            }),
            stroke: new Stroke({
              color: 'rgba(0,0,0,0.35)',
              width: 2
            }),
            font: 'bold 13px sans-serif'
          })
        })
      }
    })

    map.addLayer(communityHeatLayer)
  } catch (error) {
    console.error('加载社区热度图层失败：', error)
  }
}

function toggleCommunityHeatLayer() {
  communityHeatVisible = !communityHeatVisible

  if (communityHeatLayer) {
    communityHeatLayer.setVisible(communityHeatVisible)
  }
}

function initMap() {
  markerSource = new VectorSource()

  const markerLayer = new VectorLayer({
    source: markerSource,
    style: normalStyle,
    zIndex: 30
  })

visitIsochroneLayer = new VectorLayer({
  source: new VectorSource(),
  zIndex: 8,
  style: feature => {
    const minutes = feature.get('minutes')

    if (minutes === 15) {
      return new Style({
        fill: new Fill({ color: 'rgba(58, 158, 92, 0.20)' }),
        stroke: new Stroke({ color: '#3A9E5C', width: 2 })
      })
    }

    if (minutes === 30) {
      return new Style({
        fill: new Fill({ color: 'rgba(232, 132, 91, 0.18)' }),
        stroke: new Stroke({ color: '#E8845B', width: 2 })
      })
    }

    return new Style({
      fill: new Fill({ color: 'rgba(217, 83, 79, 0.14)' }),
      stroke: new Stroke({ color: '#D9534F', width: 2 })
    })
  }
})

visitReachableInstitutionLayer = new VectorLayer({
  source: new VectorSource(),
  zIndex: 35,
  style: new Style({
    image: new CircleStyle({
      radius: 10,
      fill: new Fill({ color: '#7B61FF' }),
      stroke: new Stroke({ color: '#FFFFFF', width: 3 })
    })
  })
})

visitStartLayer = new VectorLayer({
  source: new VectorSource(),
  zIndex: 45,
  style: new Style({
    image: new CircleStyle({
      radius: 12,
      fill: new Fill({ color: '#111827' }),
      stroke: new Stroke({ color: '#FFFFFF', width: 3 })
    })
  })
})



  tiandituVectorLayer = createTiandituTileLayer('vec', 1, 1)
  tiandituLabelLayer = createTiandituTileLayer('cva', 2, 1)

  medicalPoiLayer = createPoiWmsLayer('medical')
  lifePoiLayer = createPoiWmsLayer('life')
  parkPoiLayer = createPoiWmsLayer('park')

  institutionWmsLayer = new TileLayer({
    visible: showInstitutionWms.value,
    opacity: 0.32,
    zIndex: 10,
    source: new TileWMS({
      url: '/geoserver/elderly/wms',
      params: {
        LAYERS: geoserverInstitutionLayerName,
        TILED: true,
        CQL_FILTER: 'status=1'
      },
      serverType: 'geoserver',
      crossOrigin: 'anonymous'
    })
  })

  map = new Map({
    target: 'map',
   layers: [
  tiandituVectorLayer,
  tiandituLabelLayer,
  visitIsochroneLayer,
  institutionWmsLayer,
  medicalPoiLayer,
  lifePoiLayer,
  parkPoiLayer,
  markerLayer,
  visitReachableInstitutionLayer,
  visitStartLayer
],
    view: new View({
      center: fromLonLat([118.7969, 32.0603]),
      zoom: 10,
      minZoom: 8,
      maxZoom: 18
    })
  })
  loadCommunityHeatLayer()

  const popupElement = document.getElementById('poi-popup')
  poiPopupOverlay = new Overlay({
    element: popupElement,
    positioning: 'bottom-center',
    stopEvent: true,
    offset: [0, -12]
  })
  map.addOverlay(poiPopupOverlay)

map.on('singleclick', async event => {
  if (visitPickMode.value) {
  const [lon, lat] = toLonLat(event.coordinate)

  setVisitStart({
    name: '地图选点起点',
    lon,
    lat
  })

  visitPickMode.value = false
  await runVisitAccessibilityAnalysis()
  return
}
  
  
  layerDropdownOpen.value = false
  closePoiPopup()

    const feature = map.forEachFeatureAtPixel(event.pixel, currentFeature => currentFeature)

    if (feature) {
      const institution = feature.get('institution')

      if (institution) {
        selectInstitution(institution)
        setSelectedFeature(feature)
        return
      }
    }

    await queryPoiFeatureInfo(event)
  })

  window.addEventListener('resize', updateMapSize)
}

async function queryPoiFeatureInfo(event) {
  const visiblePoiLayers = getVisiblePoiLayers()

  if (visiblePoiLayers.length === 0) {
    return
  }

  const view = map.getView()
  const resolution = view.getResolution()
  const projection = view.getProjection()

  for (const poiLayer of visiblePoiLayers) {
    const source = poiLayer.getSource()

    const url = source.getFeatureInfoUrl(
      event.coordinate,
      resolution,
      projection,
      {
        INFO_FORMAT: 'application/json',
        FEATURE_COUNT: 5
      }
    )

    if (!url) {
      continue
    }

    try {
      const response = await fetch(url)

      if (!response.ok) {
        continue
      }

      const data = await response.json()

      if (data.features && data.features.length > 0) {
        const poi = normalizePoiFeature(data.features[0])
        selectedPoi.value = poi
        poiPopupOverlay.setPosition(event.coordinate)
        return
      }
    } catch (error) {
      console.error('POI GetFeatureInfo 查询失败：', error)
    }
  }
}

function getVisiblePoiLayers() {
  const result = []

  if (showMedicalPoiLayer.value && medicalPoiLayer) {
    result.push(medicalPoiLayer)
  }

  if (showLifePoiLayer.value && lifePoiLayer) {
    result.push(lifePoiLayer)
  }

  if (showParkPoiLayer.value && parkPoiLayer) {
    result.push(parkPoiLayer)
  }

  return result
}

function normalizePoiFeature(feature) {
  const props = feature.properties || {}
  const category = Number(props.category)

  return {
    id: props.id,
    name: props.name || props.NAME || '未命名 POI',
    address: props.address || props.ADDRESS || '暂无地址',
    district: props.district || props.DISTRICT || '',
    typeFull: props.type_full || props.TYPE_FULL || '',
    category,
    categoryText: getPoiCategoryText(category),
    categoryClass: getPoiCategoryClass(category)
  }
}

function getPoiCategoryText(category) {
  if (category === 1) {
    return '医疗设施'
  }

  if (category === 2) {
    return '生活服务'
  }

  if (category === 3) {
    return '公园景点'
  }

  return '其他 POI'
}

function getPoiCategoryClass(category) {
  if (category === 1) {
    return 'medical'
  }

  if (category === 2) {
    return 'life'
  }

  if (category === 3) {
    return 'park'
  }

  return 'other'
}

function closePoiPopup() {
  selectedPoi.value = null

  if (poiPopupOverlay) {
    poiPopupOverlay.setPosition(undefined)
  }
}

async function loadInstitutions() {
  loading.value = true

  try {
    const params = {
      limit: 500
    }

    if (filters.keyword) {
      params.keyword = filters.keyword
    }

    if (filters.district) {
      params.district = filters.district
    }

    if (filters.institutionCategory) {
      params.institutionCategory = Number(filters.institutionCategory)
    }

    if (filters.priceTier) {
      params.priceTier = Number(filters.priceTier)
    }

    if (filters.serviceType) {
      params.serviceType = filters.serviceType
    }

    if (filters.hasAvailableBeds) {
      params.hasAvailableBeds = true
    }

    const response = await axios.get('/api/institutions', { params })

    institutions.value = response.data
    renderMarkers(response.data)
    updateWmsFilter()
  } catch (error) {
    console.error('加载机构失败：', error)
    alert('加载机构失败，请检查后端服务、接口路径或浏览器控制台')
  } finally {
    loading.value = false
  }
}

function renderMarkers(data) {
  markerSource.clear()
  selectedFeature = null

  data.forEach(item => {
    const lon = Number(item.lon)
    const lat = Number(item.lat)

    if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
      return
    }

    const feature = new Feature({
      geometry: new Point(fromLonLat([lon, lat])),
      institution: item
    })

    feature.setId(item.id)
    markerSource.addFeature(feature)
  })
}

function focusInstitution(item) {
  closePoiPopup()

  const lon = Number(item.lon)
  const lat = Number(item.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
    return
  }

  selectInstitution(item)

  const feature = markerSource.getFeatureById(item.id)
  if (feature) {
    setSelectedFeature(feature)
  }

  map.getView().animate({
    center: fromLonLat([lon, lat]),
    zoom: 14,
    duration: 500
  })

  if (window.innerWidth <= 768) {
    listPanelOpen.value = false
  }
}

function openInstitutionDetail(item) {
  if (!item || !item.id) {
    return
  }

  closePoiPopup()
  router.push({
    name: 'InstitutionDetail',
    params: {
      id: item.id
    }
  })
}


function selectInstitution(item) {
  closePoiPopup()
  selectedInstitution.value = item
}

function setSelectedFeature(feature) {
  if (selectedFeature) {
    selectedFeature.setStyle(normalStyle)
  }

  feature.setStyle(selectedStyle)
  selectedFeature = feature
}

function addToCompare(item) {
  alert(`已加入对比：${item.name}`)
}

function toggleTiandituBase() {
  if (tiandituVectorLayer) {
    tiandituVectorLayer.setVisible(showTiandituBase.value)
  }

  if (tiandituLabelLayer) {
    tiandituLabelLayer.setVisible(showTiandituBase.value)
  }
}

function toggleInstitutionWms() {
  if (institutionWmsLayer) {
    institutionWmsLayer.setVisible(showInstitutionWms.value)
  }
}

function togglePoiLayer(type) {
  const config = poiLayerConfig[type]

  if (!config) {
    return
  }

  const layer = config.layerGetter()
  const visible = config.visibleRef.value

  if (!layer) {
    return
  }

  layer.setVisible(visible)

  if (visible) {
    layer.getSource().updateParams({
      _t: Date.now()
    })
  }

  closePoiPopup()
}

function getActiveLayerCount() {
  let count = 0

  if (showMedicalPoiLayer.value) {
    count += 1
  }

  if (showLifePoiLayer.value) {
    count += 1
  }

  if (showParkPoiLayer.value) {
    count += 1
  }

  return count
}

function showAllPoiLayers() {
  showMedicalPoiLayer.value = true
  showLifePoiLayer.value = true
  showParkPoiLayer.value = true

  togglePoiLayer('medical')
  togglePoiLayer('life')
  togglePoiLayer('park')
}

function hideAllPoiLayers() {
  showMedicalPoiLayer.value = false
  showLifePoiLayer.value = false
  showParkPoiLayer.value = false

  togglePoiLayer('medical')
  togglePoiLayer('life')
  togglePoiLayer('park')

  closePoiPopup()
}


function updateWmsFilter() {
  if (!institutionWmsLayer) {
    return
  }

  const cql = ['status=1']

  if (filters.district) {
    cql.push(`district='${filters.district}'`)
  }

  if (filters.institutionCategory) {
    cql.push(`institution_category=${Number(filters.institutionCategory)}`)
  }

  if (filters.priceTier) {
    cql.push(`price_tier=${Number(filters.priceTier)}`)
  }

  institutionWmsLayer.getSource().updateParams({
    CQL_FILTER: cql.join(' AND ')
  })
}

function resetFilters() {
  filters.keyword = ''
  filters.district = ''
  filters.institutionCategory = ''
  filters.priceTier = ''
  filters.serviceType = ''
  filters.hasAvailableBeds = false

  loadInstitutions()
}

function toggleElderMode() {
  elderMode.value = !elderMode.value
  document.body.classList.toggle('elder-mode', elderMode.value)
  updateMapSize()
}

function updateMapSize() {
  nextTick(() => {
    if (map) {
      map.updateSize()
    }
  })
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

  return `${Number(value).toFixed(1)}分`
}

onMounted(async () => {
  initMap()
  await loadInstitutions()
  updateMapSize()
})

function toggleVisitPickMode() {
  visitPickMode.value = !visitPickMode.value

  if (visitPickMode.value) {
    visitErrorMessage.value = '请在地图上点击一个位置作为探视起点。'
  } else {
    visitErrorMessage.value = ''
  }
}

async function searchVisitStart() {
  if (!visitSearchKeyword.value.trim()) {
    visitErrorMessage.value = '请输入起点关键词。'
    return
  }

  visitLoading.value = true
  visitErrorMessage.value = ''

  try {
    const response = await axios.get('/api/geo/geocode', {
      params: {
        keyword: visitSearchKeyword.value.trim()
      }
    })

    setVisitStart({
      name: response.data.formattedAddress || visitSearchKeyword.value.trim(),
      lon: Number(response.data.lon),
      lat: Number(response.data.lat)
    })

    await runVisitAccessibilityAnalysis()
  } catch (error) {
    console.error('起点搜索失败：', error)
    visitErrorMessage.value = '起点搜索失败，请检查天地图 key 或换一个更明确的南京地址。'
  } finally {
    visitLoading.value = false
  }
}

function setVisitStart(start) {
  if (!start || !Number.isFinite(start.lon) || !Number.isFinite(start.lat)) {
    visitErrorMessage.value = '起点坐标无效。'
    return
  }

  visitStart.value = start
  renderVisitStartMarker()

  if (map) {
    map.getView().animate({
      center: fromLonLat([start.lon, start.lat]),
      zoom: 13,
      duration: 400
    })
  }
}

function renderVisitStartMarker() {
  if (!visitStartLayer || !visitStart.value) {
    return
  }

  const source = visitStartLayer.getSource()
  source.clear()

  source.addFeature(
    new Feature({
      geometry: new Point(fromLonLat([visitStart.value.lon, visitStart.value.lat])),
      name: visitStart.value.name
    })
  )
}

async function runVisitAccessibilityAnalysis() {
  if (!visitStart.value) {
    visitErrorMessage.value = '请先设置探视起点。'
    return
  }

  visitLoading.value = true
  visitErrorMessage.value = ''

  try {
    const response = await axios.get('/api/analysis/visit-accessibility', {
      params: {
        lon: visitStart.value.lon,
        lat: visitStart.value.lat,
        startName: visitStart.value.name,
        mode: 'driving'
      }
    })

    visitAnalysisResult.value = response.data
    renderVisitAccessibilityResult(response.data)
  } catch (error) {
    console.error('探视可达性分析失败：', error)
    visitErrorMessage.value = '探视可达性分析失败，请检查 pgRouting、road_network_vertices_pgr 或路网 cost_s 字段。'
  } finally {
    visitLoading.value = false
  }
}

function renderVisitAccessibilityResult(result) {
  if (!result) {
    return
  }

  renderVisitIsochroneRings(result.rings || [])
  renderReachableInstitutions(result.reachableInstitutions || [])
}

function renderVisitIsochroneRings(rings) {
  if (!visitIsochroneLayer) {
    return
  }

  const source = visitIsochroneLayer.getSource()
  source.clear()

  rings.forEach(ring => {
    if (!ring.polygonGeoJson) {
      return
    }

    try {
      const geometry = geoJsonFormat.readGeometry(
        JSON.parse(ring.polygonGeoJson),
        {
          dataProjection: 'EPSG:4326',
          featureProjection: 'EPSG:3857'
        }
      )

      const feature = new Feature({
        geometry,
        minutes: ring.minutes,
        level: ring.level,
        levelText: ring.levelText
      })

      source.addFeature(feature)
    } catch (error) {
      console.error('解析等时圈 GeoJSON 失败：', error)
    }
  })
}

function renderReachableInstitutions(list) {
  if (!visitReachableInstitutionLayer) {
    return
  }

  const source = visitReachableInstitutionLayer.getSource()
  source.clear()

  list.forEach(item => {
    const lon = Number(item.lon)
    const lat = Number(item.lat)

    if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
      return
    }

    const feature = new Feature({
      geometry: new Point(fromLonLat([lon, lat])),
      institution: item,
      visitReachable: true
    })

    source.addFeature(feature)
  })
}

function focusReachableInstitution(item) {
  if (!item || !map) {
    return
  }

  const lon = Number(item.lon)
  const lat = Number(item.lat)

  if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
    return
  }

  map.getView().animate({
    center: fromLonLat([lon, lat]),
    zoom: 15,
    duration: 400
  })
}





</script>

<style scoped>
.user-menu {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.user-btn {
  height: 44px;
  border: 1px solid #d9e0e8;
  border-radius: 999px;
  padding: 0 20px;
  background: #ffffff;
  color: #1f2933;
  font-weight: 800;
  cursor: pointer;
}

.user-btn:hover {
  border-color: #2e7d6b;
  color: #2e7d6b;
  background: #f7faf9;
}

.user-dropdown {
  position: absolute;
  right: 0;
  top: 54px;
  width: 190px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #d9e0e8;
  box-shadow: 0 16px 36px rgba(31, 41, 51, 0.14);
  padding: 10px;
  z-index: 999;
}

.user-info {
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f5;
  margin-bottom: 6px;
}

.user-info strong {
  display: block;
  color: #1f2933;
  font-size: 15px;
}

.user-info span {
  display: block;
  margin-top: 4px;
  color: #7b8794;
  font-size: 12px;
}

.user-dropdown button {
  width: 100%;
  height: 36px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #52606d;
  font-weight: 800;
  text-align: left;
  padding: 0 12px;
  cursor: pointer;
}

.user-dropdown button:hover {
  background: #e3f1ed;
  color: #2e7d6b;
}
</style>