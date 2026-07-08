<template>
  <div class="console-page">
    <header class="console-header">
      <div>
        <h1>机构工作台</h1>
        <p>维护机构账号、入驻申请和机构详情信息</p>
      </div>
      <div class="header-actions">
        <button @click="goHome">返回首页</button>
        <button @click="logout">退出登录</button>
      </div>
    </header>

    <main class="console-main">
      <aside class="side-nav">
        <button :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">工作台首页</button>
        <button :class="{ active: activeTab === 'account' }" @click="activeTab = 'account'">机构账号资料</button>
        <button :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">修改密码</button>
        <button :class="{ active: activeTab === 'institution' }" @click="activeTab = 'institution'">机构详情维护</button>
        <button :class="{ active: activeTab === 'applications' }" @click="activeTab = 'applications'">入驻申请记录</button>
      </aside>

      <section class="content-area">
        <section v-if="activeTab === 'overview'" class="card hero-card">
          <h2>欢迎使用机构工作台</h2>
          <p>机构账号可以提交入驻申请，审核通过后维护机构详情。机构设置了有效点位后，会进入主页地图、地图找院、探视可达性分析、智能推荐和 AI 伴诊候选范围。</p>
          <div class="quick-actions">
            <button @click="goApplication">提交/更新入驻申请</button>
            <button @click="activeTab = 'institution'">维护机构详情</button>
          </div>
          <div class="status-box">
            <strong>当前绑定机构：</strong>
            <span v-if="institutionOptions.length">已绑定 {{ institutionOptions.length }} 家审核通过机构，当前维护：{{ institution.name || '请选择机构' }}</span>
            <span v-else>暂未绑定审核通过的机构</span>
          </div>
        </section>

        <section v-if="activeTab === 'account'" class="card">
          <h2>机构账号资料</h2>
          <div class="form-grid">
            <label>
              用户名
              <input v-model="accountForm.username" disabled />
            </label>
            <label>
              昵称/联系人名称
              <input v-model="accountForm.nickname" />
            </label>
            <label>
              手机号
              <input v-model="accountForm.phone" />
            </label>
            <label>
              邮箱
              <input v-model="accountForm.email" />
            </label>
          </div>
          <button class="primary" @click="saveAccount">保存账号资料</button>
        </section>

        <section v-if="activeTab === 'password'" class="card">
          <h2>修改密码</h2>
          <div class="form-grid">
            <label>
              原密码
              <input v-model="passwordForm.oldPassword" type="password" />
            </label>
            <label>
              新密码
              <input v-model="passwordForm.newPassword" type="password" />
            </label>
            <label>
              确认新密码
              <input v-model="passwordForm.confirmPassword" type="password" />
            </label>
          </div>
          <button class="primary" @click="savePassword">修改密码</button>
        </section>

        <section v-if="activeTab === 'institution'" class="card">
          <div class="section-title-row">
            <div>
              <h2>机构详情维护</h2>
              <p>这些字段会同步到机构详情页、地图主页和推荐候选库。</p>
            </div>
            <div class="institution-toolbar">
              <select
                v-if="institutionOptions.length > 1"
                v-model.number="selectedInstitutionId"
                @change="reloadInstitution"
              >
                <option
                  v-for="item in institutionOptions"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.name }} · {{ item.district || '未设置区县' }}
                </option>
              </select>
              <button @click="reloadInstitution">刷新信息</button>
            </div>
          </div>

          <div v-if="!institution.id" class="empty-box">
当前账号还没有审核通过的绑定机构。一个机构账号可以提交多家机构的入驻申请；审核通过后，可在这里选择要维护的机构。
            <button @click="goApplication">去提交入驻申请</button>
          </div>

          <div v-else>
            <div class="form-grid three">
              <label>
                机构名称
                <input v-model="institutionForm.name" />
              </label>
              <label>
                机构性质
                <select v-model.number="institutionForm.institutionCategory">
                  <option :value="1">公办公营</option>
                  <option :value="2">公办民营</option>
                  <option :value="3">民营</option>
                </select>
              </label>
              <label>
                机构等级
                <select v-model="institutionForm.gradeLevel">
                  <option value="一级">一级</option>
                  <option value="二级">二级</option>
                  <option value="三级">三级</option>
                  <option value="四级">四级</option>
                  <option value="五级">五级</option>
                  <option value="未设置">未设置</option>
                </select>
              </label>
            </div>

            <div class="form-grid three">
              <label>
                省份
                <select v-model="institutionForm.province" @change="handleProvinceChange">
                  <option value="江苏省">江苏省</option>
                </select>
              </label>
              <label>
                城市
                <select v-model="institutionForm.city" @change="handleCityChange">
                  <option value="南京市">南京市</option>
                </select>
              </label>
              <label>
                区县
                <select v-model="institutionForm.district">
                  <option value="">请选择区县</option>
                  <option v-for="item in districtOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
            </div>

            <div class="address-tools">
              <input v-model="addressKeyword" placeholder="输入地址关键词，用天地图匹配点位" @keyup.enter="matchByTianditu" />
              <button @click="matchByTianditu">天地图匹配</button>
              <button :class="{ active: pickMode }" @click="togglePickMode">{{ pickMode ? '正在点选' : '地图点选' }}</button>
            </div>

            <div class="form-grid">
              <label>
                地址名称
                <input v-model="institutionForm.addressName" />
              </label>
              <label>
                详细地址
                <input v-model="institutionForm.address" />
              </label>
            </div>

            <div class="point-box">
              当前点位：
              <span v-if="institutionForm.lon && institutionForm.lat">{{ institutionForm.lon.toFixed(6) }}, {{ institutionForm.lat.toFixed(6) }}</span>
              <span v-else>未设置</span>
            </div>
            <div id="console-location-map" class="location-map"></div>

            <div class="form-grid three">
              <label>
                总床位数
                <input v-model.number="institutionForm.totalBeds" type="number" min="0" />
              </label>
              <label>
                空余床位数
                <input v-model.number="institutionForm.availableBeds" type="number" min="0" />
              </label>
              <label>
                月费起步价
                <input v-model.number="institutionForm.monthlyFeeBase" type="number" min="0" />
              </label>
            </div>

            <div class="form-grid three">
              <label>
                价格档位
                <select v-model.number="institutionForm.priceTier">
                  <option :value="1">3000 元以下</option>
                  <option :value="2">3000-5000 元</option>
                  <option :value="3">5000-8000 元</option>
                  <option :value="4">8000 元以上</option>
                </select>
              </label>
              <label>
                联系人
                <input v-model="institutionForm.contactPerson" />
              </label>
              <label>
                联系电话
                <input v-model="institutionForm.contactPhone" />
              </label>
            </div>

            <div class="upload-block">
              <h3>机构展示图片</h3>
              <p class="hint">封面图片和机构相册支持直接选择上传，不需要手动填写图片地址。</p>

              <div class="upload-grid">
                <div class="upload-card">
                  <label class="upload-label">
                    <span>封面图片</span>
                    <input type="file" accept="image/*" @change="handleCoverImageUpload" />
                  </label>

                  <div v-if="institutionForm.coverImageUrl" class="image-preview single">
                    <img :src="normalizeFileUrl(institutionForm.coverImageUrl)" alt="机构封面图" />
                    <button type="button" @click="clearCoverImage">删除封面</button>
                  </div>
                </div>

                <div class="upload-card">
                  <label class="upload-label">
                    <span>机构相册</span>
                    <input type="file" accept="image/*" multiple @change="handleGalleryUpload" />
                  </label>

                  <div v-if="galleryUrls.length" class="image-preview gallery">
                    <div v-for="(url, index) in galleryUrls" :key="url" class="gallery-item">
                      <img :src="normalizeFileUrl(url)" alt="机构相册图" />
                      <button type="button" @click="removeGalleryImage(index)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <label class="check-line">
              <input v-model="institutionForm.hasPanorama" type="checkbox" />
              <span>机构已提供全景/实景展示</span>
            </label>

            <label>
              机构介绍
              <textarea v-model="institutionForm.intro"></textarea>
            </label>

            <h3>服务项目</h3>
            <article v-for="(item, index) in institutionForm.services" :key="index" class="item-row">
              <select v-model="item.serviceType">
                <option value="selfCare">基本自理</option>
                <option value="semiCare">半失能照护</option>
                <option value="nursing">失能护理</option>
                <option value="dementia">认知症照护</option>
                <option value="rehab">康复护理</option>
                <option value="medical">医养结合</option>
              </select>
              <input v-model="item.serviceDetail" placeholder="服务说明" />
              <label class="mini-check"><input v-model="item.isAvailable" type="checkbox" /> 可提供</label>
              <button class="danger" @click="removeService(index)">删除</button>
            </article>
            <button class="add" @click="addService">+ 添加服务</button>

            <h3>设施设备</h3>
            <article v-for="(item, index) in institutionForm.facilities" :key="index" class="item-row two">
              <input v-model="item.facilityName" placeholder="设施名称" />
              <input v-model="item.facilityDesc" placeholder="设施说明" />
              <button class="danger" @click="removeFacility(index)">删除</button>
            </article>
            <button class="add" @click="addFacility">+ 添加设施</button>

            <button class="primary save-institution" @click="saveInstitution">保存机构详情</button>
          </div>
        </section>

        <section v-if="activeTab === 'applications'" class="card">
          <h2>入驻申请记录</h2>
          <article v-for="item in applications" :key="item.id" class="application-card">
            <div class="application-title">
              <h3>{{ item.institutionName }}</h3>
              <span>{{ item.statusText }}</span>
            </div>
            <p>{{ item.province }} {{ item.city }} {{ item.district }} · {{ item.address }}</p>
            <p>联系人：{{ item.contactPerson }} ｜ 电话：{{ item.contactPhone }}</p>
            <p v-if="item.rejectReason">驳回原因：{{ item.rejectReason }}</p>
          </article>
          <button class="primary" @click="goApplication">提交新申请</button>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Map from 'ol/Map.js'
import View from 'ol/View.js'
import TileLayer from 'ol/layer/Tile.js'
import VectorLayer from 'ol/layer/Vector.js'
import XYZ from 'ol/source/XYZ.js'
import VectorSource from 'ol/source/Vector.js'
import Feature from 'ol/Feature.js'
import Point from 'ol/geom/Point.js'
import { fromLonLat, toLonLat } from 'ol/proj.js'
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style.js'
import { clearAuth, saveAuth } from '../utils/auth'

const router = useRouter()
const activeTab = ref('overview')
const applications = ref([])
const institutionOptions = ref([])
const selectedInstitutionId = ref(null)
const addressKeyword = ref('')
const galleryUrls = ref([])
const pickMode = ref(false)
const districtOptions = ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区', '溧水区', '高淳区']

const accountForm = reactive({ username: '', nickname: '', phone: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const institution = reactive({ id: null, name: '' })
const institutionForm = reactive(defaultInstitutionForm())

let locationMap = null
let pointLayer = null
const tiandituTk = '01c5845db0eb91889d42399c5a5b4f16'

onMounted(async () => {
  await loadAccount()
  await loadApplications()
  await loadInstitutionOptions()
  await reloadInstitution()
})

onBeforeUnmount(() => {
  if (locationMap) {
    locationMap.setTarget(null)
    locationMap = null
  }
})

watch(activeTab, async value => {
  if (value === 'institution') {
    await nextTick()
    if (!locationMap) {
      initLocationMap()
    } else {
      setTimeout(() => locationMap.updateSize(), 50)
    }
    renderPoint()
  }
})

function defaultInstitutionForm() {
  return {
    name: '',
    province: '江苏省',
    city: '南京市',
    district: '',
    address: '',
    addressName: '',
    lon: null,
    lat: null,
    pointSource: '',
    institutionCategory: 3,
    gradeLevel: '未设置',
    totalBeds: null,
    availableBeds: null,
    monthlyFeeBase: null,
    priceTier: 2,
    contactPerson: '',
    contactPhone: '',
    coverImageUrl: '',
    images: '[]',
    hasPanorama: false,
    intro: '',
    services: [{ serviceType: 'selfCare', serviceDetail: '', isAvailable: true }],
    facilities: [{ facilityName: '', facilityDesc: '' }]
  }
}

function createTiandituLayer(layerName, zIndex) {
  return new TileLayer({
    zIndex,
    source: new XYZ({
      url: `https://t0.tianditu.gov.cn/${layerName}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layerName}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${tiandituTk}`,
      crossOrigin: 'anonymous'
    })
  })
}

function initLocationMap() {
  const source = new VectorSource()
  pointLayer = new VectorLayer({
    source,
    zIndex: 20,
    style: new Style({
      image: new CircleStyle({
        radius: 8,
        fill: new Fill({ color: '#e8845b' }),
        stroke: new Stroke({ color: '#fff', width: 3 })
      })
    })
  })

  locationMap = new Map({
    target: 'console-location-map',
    layers: [createTiandituLayer('vec', 1), createTiandituLayer('cva', 2), pointLayer],
    view: new View({ center: fromLonLat([118.7969, 32.0603]), zoom: 11 })
  })

  locationMap.on('singleclick', event => {
    if (!pickMode.value) return
    const [lon, lat] = toLonLat(event.coordinate)
    setPoint({ lon, lat, address: institutionForm.address || '地图点选位置', addressName: institutionForm.addressName || '机构点选位置', pointSource: 'map_pick' })
    pickMode.value = false
  })
}

function renderPoint() {
  if (!pointLayer || !institutionForm.lon || !institutionForm.lat) return
  const source = pointLayer.getSource()
  source.clear()
  source.addFeature(new Feature({ geometry: new Point(fromLonLat([institutionForm.lon, institutionForm.lat])) }))
}

function setPoint(point) {
  institutionForm.lon = Number(point.lon)
  institutionForm.lat = Number(point.lat)
  institutionForm.address = point.address || institutionForm.address
  institutionForm.addressName = point.addressName || point.address || institutionForm.addressName
  institutionForm.pointSource = point.pointSource || 'unknown'
  renderPoint()
  if (locationMap) {
    locationMap.getView().animate({ center: fromLonLat([institutionForm.lon, institutionForm.lat]), zoom: 16, duration: 350 })
  }
}

async function loadAccount() {
  const response = await axios.get('/api/auth/me')
  const user = response.data || {}
  accountForm.username = user.username || ''
  accountForm.nickname = user.nickname || ''
  accountForm.phone = user.phone || ''
  accountForm.email = user.email || ''
}

async function saveAccount() {
  const response = await axios.put('/api/auth/profile', {
    nickname: accountForm.nickname,
    phone: accountForm.phone,
    email: accountForm.email
  })
  if (response.data?.token && response.data?.user) {
    saveAuth(response.data.token, response.data.user)
  }
  alert('机构账号资料已保存。')
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    alert('请填写原密码和新密码。')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次新密码不一致。')
    return
  }
  await axios.put('/api/auth/password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  alert('密码修改成功。')
}

async function loadApplications() {
  const response = await axios.get('/api/institution-console/applications/mine')
  applications.value = response.data || []
}

async function loadInstitutionOptions() {
  const response = await axios.get('/api/institution-console/profile/options')
  institutionOptions.value = response.data || []

  if (!institutionOptions.value.length) {
    selectedInstitutionId.value = null
    return
  }

  const stillExists = institutionOptions.value.some(item => Number(item.id) === Number(selectedInstitutionId.value))
  if (!selectedInstitutionId.value || !stillExists) {
    selectedInstitutionId.value = institutionOptions.value[0].id
  }
}

async function reloadInstitution() {
  if (!institutionOptions.value.length) {
    await loadInstitutionOptions()
  }

  if (!selectedInstitutionId.value) {
    institution.id = null
    institution.name = ''
    return
  }

  const response = await axios.get(`/api/institution-console/profile/${selectedInstitutionId.value}`)
  const data = response.data

  if (!data) {
    institution.id = null
    institution.name = ''
    return
  }

  institution.id = data.id
  institution.name = data.name
  applyInstitution(data)
}

function applyInstitution(data) {
  institutionForm.name = data.name || ''
  institutionForm.province = data.province || '江苏省'
  institutionForm.city = data.city || '南京市'
  institutionForm.district = data.district || ''
  institutionForm.address = data.address || ''
  institutionForm.addressName = data.address || ''
  institutionForm.lon = data.lon || data.lonWgs84 || null
  institutionForm.lat = data.lat || data.latWgs84 || null
  institutionForm.institutionCategory = data.institutionCategory || 3
  institutionForm.gradeLevel = data.gradeLevel || '未设置'
  institutionForm.totalBeds = data.totalBeds
  institutionForm.availableBeds = data.availableBeds
  institutionForm.monthlyFeeBase = data.monthlyFeeBase
  institutionForm.priceTier = data.priceTier || 2
  institutionForm.contactPerson = data.contactPerson || ''
  institutionForm.contactPhone = data.contactPhone || ''
  institutionForm.coverImageUrl = data.coverImageUrl || ''
  institutionForm.images = data.images || '[]'
  institutionForm.hasPanorama = Boolean(data.hasPanorama)
  institutionForm.intro = data.intro || ''
  institutionForm.services = (data.services || []).map(item => ({
    serviceType: item.serviceType,
    serviceDetail: item.serviceDetail,
    isAvailable: item.isAvailable !== false
  }))
  if (institutionForm.services.length === 0) addService()
  institutionForm.facilities = (data.facilities || []).map(item => ({
    facilityName: item.facilityName,
    facilityDesc: item.facilityDesc
  }))
  if (institutionForm.facilities.length === 0) addFacility()
  galleryUrls.value = parseImagesToArray(institutionForm.images)
  syncGalleryToForm()
  renderPoint()
}

function parseImagesToArray(value) {
  try {
    const arr = JSON.parse(value || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function syncGalleryToForm() {
  institutionForm.images = JSON.stringify(galleryUrls.value)
}

async function uploadInstitutionImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await axios.post('/api/institution-console/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  if (!response.data?.success) {
    throw new Error(response.data?.message || '图片上传失败')
  }

  return response.data.url
}

async function handleCoverImageUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    institutionForm.coverImageUrl = await uploadInstitutionImage(file)
  } catch (error) {
    console.error('封面图片上传失败：', error)
    alert(error.message || '封面图片上传失败')
  } finally {
    event.target.value = ''
  }
}

async function handleGalleryUpload(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return

  try {
    for (const file of files) {
      const url = await uploadInstitutionImage(file)
      galleryUrls.value.push(url)
    }
    syncGalleryToForm()
  } catch (error) {
    console.error('机构相册上传失败：', error)
    alert(error.message || '机构相册上传失败')
  } finally {
    event.target.value = ''
  }
}

function clearCoverImage() {
  institutionForm.coverImageUrl = ''
}

function removeGalleryImage(index) {
  galleryUrls.value.splice(index, 1)
  syncGalleryToForm()
}

function normalizeFileUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

async function matchByTianditu() {
  const keyword = addressKeyword.value.trim() || institutionForm.address.trim() || institutionForm.name.trim()
  if (!keyword) {
    alert('请输入地址关键词。')
    return
  }
  const response = await axios.get('/api/geo/geocode', { params: { keyword } })
  const data = response.data
  setPoint({ lon: data.lon, lat: data.lat, address: data.formattedAddress, addressName: data.keyword || keyword, pointSource: 'tianditu' })
}

function togglePickMode() { pickMode.value = !pickMode.value }
function handleProvinceChange() { institutionForm.city = '南京市'; institutionForm.district = '' }
function handleCityChange() { institutionForm.district = '' }
function addService() { institutionForm.services.push({ serviceType: 'selfCare', serviceDetail: '', isAvailable: true }) }
function removeService(index) { institutionForm.services.splice(index, 1) }
function addFacility() { institutionForm.facilities.push({ facilityName: '', facilityDesc: '' }) }
function removeFacility(index) { institutionForm.facilities.splice(index, 1) }

async function saveInstitution() {
  syncGalleryToForm()

  await axios.put(`/api/institution-console/profile/${selectedInstitutionId.value}`, {
    ...institutionForm,
    services: institutionForm.services.filter(item => item.serviceType || item.serviceDetail),
    facilities: institutionForm.facilities.filter(item => item.facilityName || item.facilityDesc)
  })
  alert('机构详情已保存。')
  await reloadInstitution()
}

function goApplication() { router.push('/institution-application') }
function goHome() { router.push('/map') }
function logout() { clearAuth(); router.push('/login') }
</script>

<style scoped>
.console-page { min-height: 100vh; background: linear-gradient(180deg, #f4faf8 0%, #eef3f5 100%); color: #20312d; }
.console-header { min-height: 86px; padding: 18px 34px; background: #fff; border-bottom: 1px solid #dce8e4; display: flex; justify-content: space-between; align-items: center; }
.console-header h1 { margin: 0; color: #2e7d6b; }
.console-header p { margin: 5px 0 0; color: #6f827d; }
.header-actions { display: flex; gap: 10px; }
button { border: none; cursor: pointer; font-weight: 900; }
.header-actions button, .primary, .add, .empty-box button { height: 40px; border-radius: 999px; padding: 0 18px; background: #2e7d6b; color: #fff; }
.console-main { max-width: 1280px; margin: 0 auto; padding: 28px; display: grid; grid-template-columns: 230px 1fr; gap: 20px; }
.side-nav { background: #fff; border: 1px solid #dce8e4; border-radius: 24px; padding: 14px; height: fit-content; display: grid; gap: 10px; box-shadow: 0 12px 28px rgba(31,80,70,.08); }
.side-nav button { height: 44px; border-radius: 16px; background: #f4faf8; color: #52606d; }
.side-nav button.active { background: #2e7d6b; color: #fff; }
.card { background: #fff; border-radius: 24px; border: 1px solid #dce8e4; padding: 24px; box-shadow: 0 12px 28px rgba(31,80,70,.08); margin-bottom: 18px; }
.card h2 { margin: 0 0 14px; color: #2e7d6b; }
.hero-card p, .section-title-row p { color: #6f827d; line-height: 1.8; }
.quick-actions { display: flex; gap: 12px; margin: 18px 0; }
.quick-actions button, .section-title-row button { height: 38px; border-radius: 999px; padding: 0 16px; background: #eef7f4; color: #2e7d6b; }
.status-box, .empty-box, .point-box { background: #f6fbf9; border: 1px solid #e4f0ec; border-radius: 18px; padding: 14px; color: #52606d; }
.empty-box { display: grid; gap: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.form-grid.three { grid-template-columns: repeat(3, 1fr); }
label { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; font-weight: 900; color: #52606d; }
input, textarea, select { border: 1px solid #d9e0e8; border-radius: 14px; padding: 0 12px; outline: none; background: #fff; box-sizing: border-box; }
input, select { height: 42px; }
textarea { min-height: 96px; padding-top: 12px; line-height: 1.7; }
.section-title-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 12px; }
.institution-toolbar { display: flex; gap: 10px; align-items: center; }
.institution-toolbar select { min-width: 260px; height: 38px; border-radius: 999px; }
.address-tools { display: grid; grid-template-columns: 1fr 120px 100px; gap: 10px; margin: 12px 0; }
.address-tools button { border-radius: 14px; background: #2e7d6b; color: #fff; }
.address-tools button.active { background: #e8845b; }
.location-map { height: 330px; border-radius: 20px; border: 1px solid #dce8e4; overflow: hidden; margin: 12px 0 16px; }
.check-line, .mini-check { flex-direction: row; align-items: center; gap: 8px; }
.check-line input, .mini-check input { width: auto; height: auto; }
.item-row { display: grid; grid-template-columns: 170px 1fr 90px 70px; gap: 10px; align-items: center; margin-bottom: 10px; }
.item-row.two { grid-template-columns: 1fr 1fr 70px; }
.danger { height: 38px; border-radius: 12px; background: rgba(217,83,79,.12); color: #d9534f; }
.add { margin-bottom: 16px; }
.save-institution { margin-top: 12px; }
.application-card { border-radius: 18px; background: #f8fbfa; border: 1px solid #e3eeea; padding: 16px; margin-bottom: 12px; }
.application-title { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.application-title h3 { margin: 0; }
.application-title span { border-radius: 999px; background: #e3f1ed; color: #2e7d6b; padding: 5px 10px; font-weight: 900; }

.upload-block {
  margin: 16px 0 20px;
  padding: 18px;
  border-radius: 22px;
  background: #f7fbfa;
  border: 1px solid #e4f0ec;
}
.upload-block h3 { margin: 0 0 8px; color: #1f6f61; }
.hint { color: #6f827d; line-height: 1.7; }
.upload-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin: 12px 0 18px; }
.upload-card { padding: 16px; border-radius: 18px; background: #fff; border: 1px solid #dce9e5; }
.upload-label { display: grid; gap: 10px; cursor: pointer; }
.upload-label span { color: #314d47; font-weight: 900; }
.upload-label input { width: 100%; height: auto; border-radius: 14px; border: 1px solid #dce9e5; padding: 10px; background: #fff; }
.image-preview.single { margin-top: 14px; }
.image-preview.single img { width: 100%; max-height: 240px; object-fit: cover; border-radius: 16px; display: block; }
.image-preview.single button { margin-top: 10px; height: 34px; border-radius: 999px; padding: 0 14px; background: rgba(217, 83, 79, 0.12); color: #d9534f; }
.image-preview.gallery { margin-top: 14px; display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.gallery-item { position: relative; border-radius: 14px; overflow: hidden; background: #eef6f3; }
.gallery-item img { width: 100%; height: 100px; object-fit: cover; display: block; }
.gallery-item button { position: absolute; right: 6px; top: 6px; border-radius: 999px; padding: 4px 8px; background: rgba(217, 83, 79, 0.92); color: #fff; font-size: 12px; }

@media (max-width: 980px) { .console-main, .form-grid, .form-grid.three, .address-tools, .item-row, .item-row.two, .upload-grid { grid-template-columns: 1fr; } }
</style>
