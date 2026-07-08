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

    <section class="combined-top-panel" @click.stop>
      <div class="combined-section institution-search-section">
        <div class="combined-section-header">
          <div>
            <h3>机构搜索</h3>
            <p>按名称或地址快速查找养老机构</p>
          </div>
        </div>

        <div class="combined-search-row">
          <input
            v-model="filters.keyword"
            class="combined-input"
            placeholder="搜索养老机构名称或地址"
            @keyup.enter="loadInstitutions"
          />
          <button class="combined-primary-btn" @click="loadInstitutions">
            搜索
          </button>
        </div>
      </div>

      <div class="combined-divider"></div>

      <div class="combined-section visit-analysis-section">
        <div class="combined-section-header visit-header">
          <div>
            <h3>探视可达性分析</h3>
            <p>设置探视起点，生成 15 / 30 / 60 分钟等时圈</p>
          </div>

          <button
            class="combined-map-btn"
            :class="{ active: visitPickMode }"
            @click="toggleVisitPickMode"
          >
            {{ visitPickMode ? '正在选点' : '地图选点' }}
          </button>
        </div>

        <div class="combined-search-row visit-search-row-compact">
          <input
            v-model="visitSearchKeyword"
            class="combined-input visit-input"
            placeholder="输入起点，例如：河海大学 / 新街口"
            @keyup.enter="searchVisitStart"
          />

          <button class="combined-secondary-btn" @click="searchVisitStart">
            搜索
          </button>

          <button
            class="combined-primary-btn"
            :disabled="!visitStart || visitLoading"
            @click="runVisitAccessibilityAnalysis"
          >
            {{ visitLoading ? '分析中' : '生成等时圈' }}
          </button>

          <button
            v-if="visitAnalysisResult"
            class="combined-clear-btn"
            @click="clearVisitAccessibilityLayer"
          >
            取消图层
          </button>
        </div>

        <div
          v-if="visitStart || visitErrorMessage || visitAnalysisResult"
          class="combined-visit-status"
        >
          <div v-if="visitStart" class="visit-start-inline">
            <span>当前起点</span>
            <strong>{{ visitStart.name }}</strong>
          </div>

          <div v-if="visitErrorMessage" class="visit-error-inline">
            {{ visitErrorMessage }}
          </div>

          <div v-if="visitAnalysisResult" class="visit-result-inline">
            <span><i class="ring-15"></i>15分钟</span>
            <span><i class="ring-30"></i>30分钟</span>
            <span><i class="ring-60"></i>60分钟</span>
            <strong>
              可达 {{ visitAnalysisResult.reachableInstitutions.length }} 家
            </strong>
          </div>
        </div>
      </div>
    </section>

    <main id="map" class="map-container"></main>

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

    <!-- 地图右侧浮动图层按钮：放在机构列表框上方的地图空白区域，不放进机构列表内部 -->
    <div class="floating-layer-control" @click.stop>
      <button class="floating-layer-btn" @click="layerDropdownOpen = !layerDropdownOpen">
        <span class="layer-icon">▦</span>
        <span>图层</span>
        <span v-if="getActiveLayerCount() > 0" class="layer-count-badge">
          {{ getActiveLayerCount() }}
        </span>
      </button>

      <div v-if="layerDropdownOpen" class="floating-layer-dropdown" @click.stop>
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

    <aside
      class="side-panel result-panel"
      :class="{ 'mobile-open': listPanelOpen }"
    >
      <div class="panel-header result-panel-header result-panel-header-spaced" @click.stop>
        <button class="panel-close-btn result-panel-close" @click="listPanelOpen = false">×</button>

        <div class="result-panel-title">
          <h2>机构列表</h2>
          <p>共找到 {{ institutions.length }} 家机构</p>
        </div>
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
        <button @click="clearSelectedInstitution">×</button>
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

    clearSelectedInstitution()
    return
  }

  clearSelectedInstitution()
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


function clearSelectedInstitution() {
  selectedInstitution.value = null

  if (selectedFeature) {
    selectedFeature.setStyle(normalStyle)
    selectedFeature = null
  }
}

function selectInstitution(item) {
  closePoiPopup()
  selectedInstitution.value = item
}

function setSelectedFeature(feature) {
  if (selectedFeature) {
    selectedFeature.setStyle(normalStyle)
  }

  if (!feature) {
    selectedFeature = null
    return
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

function clearVisitAccessibilityLayer() {
  visitAnalysisResult.value = null
  visitErrorMessage.value = ''

  if (visitIsochroneLayer) {
    visitIsochroneLayer.getSource().clear()
  }

  if (visitReachableInstitutionLayer) {
    visitReachableInstitutionLayer.getSource().clear()
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

.map-page {
  position: relative;
}

.app-header {
  top: 14px;
  min-height: 66px;
  padding: 10px 18px;
  border-radius: 22px;
}

.logo-mark {
  width: 46px;
  height: 46px;
  border-radius: 12px;
}

.brand-title {
  font-size: 19px;
  line-height: 1.2;
}

.brand-subtitle {
  margin-top: 3px;
  font-size: 12px;
}

.combined-top-panel {
  position: absolute;
  top: 94px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 850;
  width: min(920px, calc(100vw - 500px));
  min-height: 66px;
  display: grid;
  grid-template-columns: 300px 1px minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
  padding: 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(46, 125, 107, 0.14);
  box-shadow: 0 14px 34px rgba(30, 80, 70, 0.16);
  backdrop-filter: blur(12px);
}

.combined-section {
  min-width: 0;
}

.combined-section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 6px;
}

.combined-section-header h3 {
  margin: 0;
  color: #1f6f61;
  font-size: 15px;
  font-weight: 900;
}

.combined-section-header p {
  margin: 2px 0 0;
  color: #72837e;
  font-size: 11px;
  line-height: 1.35;
}

.combined-divider {
  width: 1px;
  min-height: 100%;
  background: #e3eeea;
}

.combined-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.combined-input {
  flex: 1;
  min-width: 0;
  height: 38px;
  box-sizing: border-box;
  border: 1px solid #d9e6e2;
  border-radius: 12px;
  padding: 0 12px;
  outline: none;
  background: #fff;
  color: #233b36;
  font-size: 14px;
}

.combined-input:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 3px rgba(46, 125, 107, 0.12);
}

.combined-primary-btn,
.combined-secondary-btn,
.combined-map-btn {
  height: 38px;
  border-radius: 12px;
  border: none;
  padding: 0 16px;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.combined-primary-btn {
  background: #2e7d6b;
  color: #fff;
}

.combined-primary-btn:hover {
  background: #256b5b;
}

.combined-primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.combined-secondary-btn {
  background: #eef7f4;
  color: #2e7d6b;
  border: 1px solid #d9e9e4;
}

.combined-secondary-btn:hover {
  background: #e2f1ed;
}

.combined-clear-btn {
  height: 38px;
  border-radius: 12px;
  border: 1px solid #ffd0c7;
  padding: 0 14px;
  background: #fff4f1;
  color: #c25743;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.combined-clear-btn:hover {
  background: #ffe9e3;
}

.combined-map-btn {
  height: 34px;
  padding: 0 14px;
  background: #eef7f4;
  color: #2e7d6b;
  border: 1px solid #d9e9e4;
}

.combined-map-btn.active {
  background: #2e7d6b;
  color: #fff;
}

.visit-search-row-compact {
  align-items: center;
}

.combined-visit-status {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.visit-start-inline,
.visit-result-inline,
.visit-error-inline {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
}

.visit-start-inline {
  background: #f3faf7;
  color: #2f5f55;
  border: 1px solid #e0efe9;
}

.visit-start-inline span {
  color: #7b8d87;
}

.visit-start-inline strong {
  max-width: 210px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #1f6f61;
}

.visit-result-inline {
  background: #f7fbfa;
  color: #4f625d;
  border: 1px solid #e4efeb;
}

.visit-result-inline span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.visit-result-inline i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.visit-result-inline strong {
  color: #1f6f61;
}

.visit-error-inline {
  background: #fff4f1;
  color: #c25743;
  border: 1px solid #ffd7cf;
}


/* 右侧图层按钮：移动到机构列表框上方空白区域 */
.map-layer-control.right-side-layer-control {
  position: absolute !important;
  top: 212px !important;
  right: 52px !important;
  left: auto !important;
  bottom: auto !important;
  transform: none !important;
  z-index: 840;
  display: flex;
  justify-content: flex-end;
}

.right-side-layer-control .map-layer-toggle-btn {
  min-width: 108px;
  height: 44px;
  padding: 0 18px;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(30, 80, 70, 0.15);
}

.right-side-layer-control .map-layer-dropdown {
  top: 54px !important;
  right: 0 !important;
  left: auto !important;
  transform: none !important;
}

/* 机构列表标题右对齐，靠近右侧边缘显示 */
.result-panel .panel-header.result-panel-header {
  justify-content: flex-end !important;
  align-items: flex-start;
  gap: 12px;
}

.result-panel-title {
  margin-left: auto;
  text-align: right;
}

.result-panel-title h2,
.result-panel-title p {
  text-align: right;
}

.result-panel-title h2 {
  margin-left: auto;
}

.result-panel-close {
  order: -1;
  margin-right: auto;
}

@media (min-width: 901px) {
  .result-panel-close {
    display: none !important;
  }
}

@media (max-width: 1280px) {
  .combined-top-panel {
    width: min(760px, calc(100vw - 420px));
    grid-template-columns: 1fr;
  }

  .combined-divider {
    width: 100%;
    height: 1px;
    min-height: 1px;
  }
}

@media (max-width: 980px) {
  .combined-top-panel {
    left: 16px;
    right: 16px;
    width: auto;
    transform: none;
    top: 88px;
  }

  .combined-search-row {
    flex-wrap: wrap;
  }

  .combined-input {
    flex-basis: 100%;
  }
}

@media (max-width: 640px) {
  .combined-top-panel {
    top: 86px;
    padding: 12px;
    border-radius: 20px;
  }

  .combined-primary-btn,
  .combined-secondary-btn,
  .combined-clear-btn {
    flex: 1;
    padding: 0 12px;
  }

  .combined-map-btn {
    height: 32px;
  }

  .combined-section-header {
    flex-direction: column;
  }
}


@media (max-width: 1280px) {
  .map-layer-control.right-side-layer-control {
    top: 214px !important;
    right: 34px !important;
  }
}

@media (max-width: 980px) {
  .map-layer-control.right-side-layer-control {
    top: auto !important;
    right: 16px !important;
    bottom: 86px !important;
  }

  .right-side-layer-control .map-layer-dropdown {
    top: auto !important;
    bottom: 54px !important;
    right: 0 !important;
  }

  .result-panel .panel-header.result-panel-header {
    justify-content: space-between !important;
  }

  .result-panel-close {
    display: inline-flex !important;
  }
}


/* 图层控制放入右侧机构列表标题栏，避免遮挡机构卡片 */
.result-panel-header-with-layer {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  gap: 12px !important;
  padding: 0 2px 14px !important;
  margin-bottom: 10px !important;
}

.result-panel-title {
  flex: 1;
  margin-left: auto;
  text-align: right;
}

.result-panel-title h2,
.result-panel-title p {
  text-align: right;
}

.panel-layer-btn {
  flex-shrink: 0;
  min-width: 92px;
  height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(46, 125, 107, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  color: #2e7d6b;
  font-size: 15px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(31, 92, 78, 0.12);
}

.panel-layer-btn:hover {
  background: #eef7f4;
}

.layer-icon {
  font-size: 16px;
  line-height: 1;
}

.layer-count-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ff8a65;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 900;
}

.panel-layer-dropdown {
  position: relative;
  z-index: 2;
  margin: -2px 0 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(46, 125, 107, 0.14);
  box-shadow: 0 12px 30px rgba(31, 92, 78, 0.12);
}

.layer-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf4f1;
}

.layer-dropdown-header h3 {
  margin: 0;
  color: #1f6f61;
  font-size: 17px;
  font-weight: 900;
}

.layer-dropdown-header p {
  margin: 4px 0 0;
  color: #71847e;
  font-size: 12px;
}

.layer-dropdown-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: #eef7f4;
  color: #2e7d6b;
  font-size: 18px;
  font-weight: 900;
  cursor: pointer;
}

.layer-dropdown-section {
  padding-top: 12px;
}

.layer-dropdown-section h4 {
  margin: 0 0 10px;
  color: #244c44;
  font-size: 14px;
  font-weight: 900;
}

.layer-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid #f0f5f3;
}

.layer-dropdown-item:last-child {
  border-bottom: none;
}

.layer-dropdown-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.layer-dropdown-text strong {
  color: #244c44;
  font-size: 14px;
}

.layer-dropdown-text small {
  color: #7a8b86;
  font-size: 12px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.medical-dot {
  background: #e64a19;
}

.life-dot {
  background: #1976d2;
}

.park-dot {
  background: #43a047;
}

.layer-dropdown-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #edf4f1;
}

/* 取消旧图层按钮的悬浮影响 */
.map-layer-control.right-side-layer-control,
.right-side-layer-control {
  display: none !important;
}

@media (max-width: 980px) {
  .result-panel-header-with-layer {
    justify-content: space-between !important;
  }

  .panel-layer-btn {
    min-width: 82px;
    height: 36px;
    padding: 0 12px;
    font-size: 14px;
  }

  .result-panel-title h2 {
    font-size: 22px;
  }
}


/* ===== 右侧图层按钮与机构列表标题修正版 =====
   1. 图层按钮放在右侧机构列表框上方的地图空白区域，不进入机构列表内部。
   2. 机构列表标题右对齐，但保留顶部和右侧留白，不再顶格。 */

.floating-layer-control {
  position: absolute;
  top: 136px;
  right: 52px;
  z-index: 870;
  display: flex;
  justify-content: flex-end;
  pointer-events: auto;
}

.floating-layer-btn {
  min-width: 104px;
  height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(46, 125, 107, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  color: #2e7d6b;
  font-size: 16px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 12px 30px rgba(30, 80, 70, 0.16);
  backdrop-filter: blur(10px);
}

.floating-layer-btn:hover {
  background: #eef7f4;
}

.floating-layer-dropdown {
  position: absolute;
  top: 52px;
  right: 0;
  width: 310px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(46, 125, 107, 0.14);
  box-shadow: 0 18px 42px rgba(31, 92, 78, 0.16);
  z-index: 871;
}

.result-panel .panel-header.result-panel-header-spaced {
  display: flex !important;
  align-items: flex-start !important;
  justify-content: flex-end !important;
  gap: 12px !important;
  padding: 22px 24px 18px 18px !important;
  margin: 0 0 8px !important;
  box-sizing: border-box;
}

.result-panel-title {
  flex: 1;
  min-width: 0;
  margin-left: auto;
  padding-right: 8px;
  text-align: right;
}

.result-panel-title h2 {
  margin: 0;
  padding: 0;
  color: #1f2933;
  font-size: 25px;
  font-weight: 900;
  line-height: 1.2;
  text-align: right;
}

.result-panel-title p {
  margin: 7px 0 0;
  padding: 0;
  color: #697b76;
  font-size: 14px;
  line-height: 1.3;
  text-align: right;
}

.result-panel-close {
  order: -1;
  margin-right: auto;
}

/* 不再使用之前放进机构列表标题栏里的图层按钮 */
.result-panel-header-with-layer,
.panel-layer-btn,
.panel-layer-dropdown {
  display: none !important;
}

/* 旧的悬浮图层容器也禁用，避免出现第二个图层按钮 */
.map-layer-control.right-side-layer-control,
.right-side-layer-control {
  display: none !important;
}

@media (min-width: 901px) {
  .result-panel-close {
    display: none !important;
  }
}

@media (max-width: 1280px) {
  .floating-layer-control {
    top: 156px;
    right: 34px;
  }
}

@media (max-width: 980px) {
  .floating-layer-control {
    top: auto;
    right: 16px;
    bottom: 88px;
  }

  .floating-layer-dropdown {
    top: auto;
    bottom: 52px;
    right: 0;
    width: min(310px, calc(100vw - 32px));
  }

  .result-panel .panel-header.result-panel-header-spaced {
    justify-content: space-between !important;
    padding: 18px 20px 14px !important;
  }

  .result-panel-close {
    display: inline-flex !important;
  }

  .result-panel-title h2 {
    font-size: 22px;
  }
}


/* ===== 顶部标题栏右侧留白修正 =====
   让右侧“体验客户”按钮和外框右边保持留白，留白参考左侧距离。 */
.app-header {
  position: absolute !important;
  top: 14px !important;
  left: 34px !important;
  right: 34px !important;
  width: auto !important;
  max-width: none !important;
  min-height: 66px !important;
  box-sizing: border-box !important;
  display: flex !important;
  align-items: center !important;
  gap: 22px !important;
  padding: 10px 24px !important;
  border-radius: 22px !important;
  z-index: 1000 !important;
}

.header-left {
  flex: 0 0 auto !important;
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  min-width: 0 !important;
}

.desktop-nav {
  flex: 1 1 auto !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 10px !important;
  min-width: 0 !important;
}

.header-right {
  flex: 0 0 auto !important;
  display: flex !important;
  align-items: center !important;
  margin-left: auto !important;
}

.user-menu {
  flex: 0 0 auto !important;
  margin-left: 14px !important;
  margin-right: 0 !important;
  position: relative !important;
  z-index: 1100 !important;
}

.user-btn {
  max-width: 150px !important;
  white-space: nowrap !important;
}

.user-dropdown {
  z-index: 1200 !important;
}

@media (max-width: 980px) {
  .app-header {
    left: 16px !important;
    right: 16px !important;
    padding: 10px 16px !important;
    gap: 12px !important;
  }

  .desktop-nav {
    justify-content: flex-start !important;
    overflow-x: auto !important;
  }

  .user-menu {
    margin-left: 8px !important;
  }
}

</style>