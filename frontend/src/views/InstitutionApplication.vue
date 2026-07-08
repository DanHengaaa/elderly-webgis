<template>
  <div class="apply-page">
    <header class="apply-topbar">
      <button @click="goConsole">← 返回工作台</button>
      <div>
        <h1>机构入驻申请</h1>
        <p>请按表单顺序完整填写机构展示信息，页面最下方提交申请。</p>
      </div>
      <span class="topbar-spacer"></span>
    </header>

    <main class="apply-main">
      <section class="form-card">
        <h2>一、基础展示信息</h2>

        <label>
          机构名称
          <input v-model="form.institutionName" placeholder="请输入养老机构名称" />
        </label>

        <div class="grid-row three">
          <label>
            省份
            <select v-model="form.province" @change="handleProvinceChange">
              <option value="">请选择省份</option>
              <option v-for="item in provinceOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label>
            城市
            <select v-model="form.city" @change="handleCityChange">
              <option value="">请选择城市</option>
              <option v-for="item in cityOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label>
            区县
            <select v-model="form.district">
              <option value="">请选择区县</option>
              <option v-for="item in districtOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>

        <div class="grid-row three">
          <label>
            机构性质
            <select v-model.number="form.institutionCategory">
              <option :value="null">请选择机构性质</option>
              <option :value="1">公办公营</option>
              <option :value="2">公办民营</option>
              <option :value="3">民营</option>
            </select>
          </label>

          <label>
            机构等级
            <select v-model="form.gradeLevel">
              <option value="">请选择机构等级</option>
              <option value="一级">一级</option>
              <option value="二级">二级</option>
              <option value="三级">三级</option>
              <option value="四级">四级</option>
              <option value="五级">五级</option>
              <option value="未设置">未设置</option>
            </select>
          </label>

          <label>
            价格档位
            <select v-model.number="form.priceTier">
              <option :value="null">请选择价格档位</option>
              <option :value="1">3000 元以下</option>
              <option :value="2">3000-5000 元</option>
              <option :value="3">5000-8000 元</option>
              <option :value="4">8000 元以上</option>
            </select>
          </label>
        </div>

        <div class="grid-row three">
          <label>
            总床位数
            <input v-model.number="form.totalBeds" type="number" min="0" placeholder="例如：300" />
          </label>

          <label>
            空余床位数
            <input v-model.number="form.availableBeds" type="number" min="0" placeholder="例如：30" />
          </label>

          <label>
            月费起步价
            <input v-model.number="form.monthlyFeeBase" type="number" min="0" placeholder="例如：4500" />
          </label>
        </div>

        <div class="grid-row">
          <label>
            联系人
            <input v-model="form.contactPerson" placeholder="请输入联系人" />
          </label>

          <label>
            联系电话
            <input v-model="form.contactPhone" placeholder="请输入联系电话" />
          </label>
        </div>

        <div class="upload-block">
          <h3>机构展示图片</h3>
          <p class="hint">封面图片和机构相册由机构方直接选择上传，不需要手动填写图片地址。</p>

          <div class="upload-grid">
            <div class="upload-card">
              <label class="upload-label">
                <span>封面图片</span>
                <input type="file" accept="image/*" @change="handleCoverImageUpload" />
              </label>

              <div v-if="form.coverImageUrl" class="image-preview single">
                <img :src="normalizeFileUrl(form.coverImageUrl)" alt="机构封面图" />
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
          <input v-model="form.hasPanorama" type="checkbox" />
          <span>机构已提供全景/实景展示</span>
        </label>

        <label>
          机构介绍
          <textarea v-model="form.intro" placeholder="请介绍机构服务定位、环境特色、护理能力等，这部分会显示在机构详情页"></textarea>
        </label>
      </section>

      <section class="form-card">
        <h2>二、地址与空间点位</h2>
        <p class="hint">不要手动填写经纬度。请使用天地图匹配地址，或在地图上点选点位后自行命名地址。</p>

        <div class="address-tools">
          <input v-model="addressKeyword" placeholder="输入机构地址关键词，例如：鼓楼区某某养老院" @keyup.enter="matchByTianditu" />
          <button @click="matchByTianditu">调用天地图匹配</button>
          <button :class="{ active: pickMode }" @click="togglePickMode">
            {{ pickMode ? '正在点选地图' : '地图上点选' }}
          </button>
        </div>

        <div class="grid-row">
          <label>
            地址名称
            <input v-model="form.addressName" placeholder="例如：润明老年公寓主入口" />
          </label>

          <label>
            详细地址
            <input v-model="form.address" placeholder="天地图匹配或地图点选后自动填写，也可调整名称描述" />
          </label>
        </div>

        <div class="point-box">
          <strong>当前点位：</strong>
          <span v-if="form.lon && form.lat">{{ form.address || form.addressName }} ｜ {{ form.lon.toFixed(6) }}, {{ form.lat.toFixed(6) }}</span>
          <span v-else>暂未设置点位</span>
        </div>

        <div id="apply-location-map" class="location-map"></div>
      </section>

      <section class="form-card">
        <h2>三、服务项目</h2>
        <p class="hint">这里对应机构详情页的服务项目列表。</p>

        <article v-for="(item, index) in form.services" :key="index" class="item-row">
          <select v-model="item.serviceType">
            <option value="selfCare">基本自理</option>
            <option value="semiCare">半失能照护</option>
            <option value="nursing">失能护理</option>
            <option value="dementia">认知症照护</option>
            <option value="rehab">康复护理</option>
            <option value="medical">医养结合</option>
          </select>
          <input v-model="item.serviceDetail" placeholder="服务说明，例如：24小时护理、康复训练等" />
          <label class="mini-check"><input v-model="item.isAvailable" type="checkbox" /> 可提供</label>
          <button class="danger" @click="removeService(index)">删除</button>
        </article>

        <button class="add-btn" @click="addService">+ 添加服务项目</button>
      </section>

      <section class="form-card">
        <h2>四、设施设备</h2>
        <p class="hint">这里对应机构详情页的设施设备列表。</p>

        <article v-for="(item, index) in form.facilities" :key="index" class="item-row two">
          <input v-model="item.facilityName" placeholder="设施名称，例如：无障碍电梯" />
          <input v-model="item.facilityDesc" placeholder="设施说明" />
          <button class="danger" @click="removeFacility(index)">删除</button>
        </article>

        <button class="add-btn" @click="addFacility">+ 添加设施设备</button>
      </section>

      <section class="form-card">
        <h2>五、资质材料</h2>
        <p class="hint">营业执照、备案证明和其他材料支持上传图片、PDF、Word、Excel、PPT、TXT 等主流文件类型。</p>

        <div class="material-grid">
          <div class="upload-card">
            <label class="upload-label">
              <span>营业执照</span>
              <input
                type="file"
                accept=".jpg,.jpeg,.png,.webp,.gif,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.rtf"
                @change="handleLicenseUpload"
              />
            </label>

            <div v-if="form.licenseUrl" class="file-item">
              <span>{{ getFileName(form.licenseUrl) }}</span>
              <div>
                <a :href="normalizeFileUrl(form.licenseUrl)" target="_blank">查看</a>
                <button type="button" @click="form.licenseUrl = ''">删除</button>
              </div>
            </div>
          </div>

          <div class="upload-card">
            <label class="upload-label">
              <span>备案证明</span>
              <input
                type="file"
                accept=".jpg,.jpeg,.png,.webp,.gif,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.rtf"
                @change="handleRecordCertificateUpload"
              />
            </label>

            <div v-if="form.recordCertificateUrl" class="file-item">
              <span>{{ getFileName(form.recordCertificateUrl) }}</span>
              <div>
                <a :href="normalizeFileUrl(form.recordCertificateUrl)" target="_blank">查看</a>
                <button type="button" @click="form.recordCertificateUrl = ''">删除</button>
              </div>
            </div>
          </div>

          <div class="upload-card full">
            <label class="upload-label">
              <span>其他材料</span>
              <input
                type="file"
                multiple
                accept=".jpg,.jpeg,.png,.webp,.gif,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.rtf"
                @change="handleOtherMaterialsUpload"
              />
            </label>

            <div v-if="otherMaterialUrls.length" class="file-list">
              <div v-for="(url, index) in otherMaterialUrls" :key="url" class="file-item">
                <span>{{ getFileName(url) }}</span>
                <div>
                  <a :href="normalizeFileUrl(url)" target="_blank">查看</a>
                  <button type="button" @click="removeOtherMaterial(index)">删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <label>
          补充说明
          <textarea v-model="form.description" placeholder="可补充机构资质、运营情况、服务特色等"></textarea>
        </label>
      </section>

      <section class="form-card submit-card">
        <div>
          <h2>六、提交入驻申请</h2>
          <p class="hint bottom-hint">确认基础信息、地址点位、服务设施、展示图片和资质材料无误后，点击下方按钮提交管理员审核。</p>
        </div>

        <div v-if="errorMessage" class="error-box">{{ errorMessage }}</div>
        <div v-if="successMessage" class="success-box">{{ successMessage }}</div>

        <button class="bottom-submit-btn" @click="submitApplication">
          提交申请
        </button>
      </section>

      <section class="form-card">
        <h2>我的申请记录</h2>

        <article v-for="item in applications" :key="item.id" class="application-card">
          <div class="application-title">
            <h3>{{ item.institutionName }}</h3>
            <span>{{ item.statusText }}</span>
          </div>
          <p>{{ item.province }} {{ item.city }} {{ item.district }} · {{ item.address }}</p>
          <p>联系人：{{ item.contactPerson }} ｜ 电话：{{ item.contactPhone }}</p>
          <p v-if="item.rejectReason">驳回原因：{{ item.rejectReason }}</p>
          <p v-if="item.institutionId">已发布机构 ID：{{ item.institutionId }}</p>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
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

const router = useRouter()
const errorMessage = ref('')
const successMessage = ref('')
const applications = ref([])
const addressKeyword = ref('')
const galleryUrls = ref([])
const otherMaterialUrls = ref([])
const pickMode = ref(false)

const provinceOptions = ['江苏省']
const cityMap = {
  江苏省: ['南京市']
}
const districtMap = {
  南京市: ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区', '溧水区', '高淳区']
}

const cityOptions = ref([])
const districtOptions = ref([])

const form = reactive({
  institutionName: '',
  contactPerson: '',
  contactPhone: '',
  province: '江苏省',
  city: '南京市',
  district: '',
  address: '',
  addressName: '',
  lon: null,
  lat: null,
  pointSource: '',
  institutionCategory: null,
  gradeLevel: '',
  totalBeds: null,
  availableBeds: null,
  monthlyFeeBase: null,
  priceTier: null,
  coverImageUrl: '',
  images: '[]',
  hasPanorama: false,
  intro: '',
  description: '',
  services: [
    { serviceType: 'selfCare', serviceDetail: '', isAvailable: true }
  ],
  facilities: [
    { facilityName: '', facilityDesc: '' }
  ],
  licenseUrl: '',
  recordCertificateUrl: '',
  otherMaterialUrl: ''
})

let locationMap = null
let pointLayer = null
const tiandituTk = '01c5845db0eb91889d42399c5a5b4f16'

onMounted(async () => {
  cityOptions.value = cityMap[form.province] || []
  districtOptions.value = districtMap[form.city] || []
  await loadMine()
  await nextTick()
  initLocationMap()
})

onBeforeUnmount(() => {
  if (locationMap) {
    locationMap.setTarget(null)
    locationMap = null
  }
})

function syncGalleryToForm() {
  form.images = JSON.stringify(galleryUrls.value)
}

function syncMaterialsToForm() {
  form.otherMaterialUrl = JSON.stringify(otherMaterialUrls.value)
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
        stroke: new Stroke({ color: '#ffffff', width: 3 })
      })
    })
  })

  locationMap = new Map({
    target: 'apply-location-map',
    layers: [
      createTiandituLayer('vec', 1),
      createTiandituLayer('cva', 2),
      pointLayer
    ],
    view: new View({
      center: fromLonLat([118.7969, 32.0603]),
      zoom: 11
    })
  })

  locationMap.on('singleclick', event => {
    if (!pickMode.value) {
      return
    }

    const [lon, lat] = toLonLat(event.coordinate)
    setPoint({
      lon,
      lat,
      address: form.address || '地图点选位置',
      addressName: form.addressName || '机构点选位置',
      pointSource: 'map_pick'
    })
    pickMode.value = false
  })
}

function renderPoint() {
  if (!pointLayer || !form.lon || !form.lat) {
    return
  }

  const source = pointLayer.getSource()
  source.clear()
  source.addFeature(new Feature({ geometry: new Point(fromLonLat([form.lon, form.lat])) }))
}

function setPoint(point) {
  form.lon = Number(point.lon)
  form.lat = Number(point.lat)
  form.address = point.address || form.address
  form.addressName = point.addressName || point.address || form.addressName
  form.pointSource = point.pointSource || 'unknown'
  renderPoint()

  if (locationMap) {
    locationMap.getView().animate({
      center: fromLonLat([form.lon, form.lat]),
      zoom: 16,
      duration: 350
    })
  }
}

function handleProvinceChange() {
  cityOptions.value = cityMap[form.province] || []
  form.city = cityOptions.value[0] || ''
  handleCityChange()
}

function handleCityChange() {
  districtOptions.value = districtMap[form.city] || []
  form.district = ''
}

async function loadMine() {
  const response = await axios.get('/api/institution-console/applications/mine')
  applications.value = response.data || []
}

async function matchByTianditu() {
  errorMessage.value = ''

  const keyword = addressKeyword.value.trim() || form.address.trim() || form.institutionName.trim()

  if (!keyword) {
    errorMessage.value = '请输入地址关键词或机构名称。'
    return
  }

  try {
    const response = await axios.get('/api/geo/geocode', {
      params: { keyword }
    })

    const data = response.data
    setPoint({
      lon: data.lon,
      lat: data.lat,
      address: data.formattedAddress,
      addressName: data.keyword || keyword,
      pointSource: 'tianditu'
    })
  } catch (error) {
    console.error('天地图匹配失败：', error)
    errorMessage.value = '天地图匹配失败，请换一个更具体的地址关键词，或改用地图点选。'
  }
}

function togglePickMode() {
  pickMode.value = !pickMode.value
}

function addService() {
  form.services.push({ serviceType: 'selfCare', serviceDetail: '', isAvailable: true })
}

function removeService(index) {
  form.services.splice(index, 1)
}

function addFacility() {
  form.facilities.push({ facilityName: '', facilityDesc: '' })
}

function removeFacility(index) {
  form.facilities.splice(index, 1)
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

async function uploadInstitutionFile(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await axios.post('/api/institution-console/upload/file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  if (!response.data?.success) {
    throw new Error(response.data?.message || '文件上传失败')
  }

  return response.data.url
}

async function handleCoverImageUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    form.coverImageUrl = await uploadInstitutionImage(file)
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

async function handleLicenseUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    form.licenseUrl = await uploadInstitutionFile(file)
  } catch (error) {
    console.error('营业执照上传失败：', error)
    alert(error.message || '营业执照上传失败')
  } finally {
    event.target.value = ''
  }
}

async function handleRecordCertificateUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    form.recordCertificateUrl = await uploadInstitutionFile(file)
  } catch (error) {
    console.error('备案证明上传失败：', error)
    alert(error.message || '备案证明上传失败')
  } finally {
    event.target.value = ''
  }
}

async function handleOtherMaterialsUpload(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return

  try {
    for (const file of files) {
      const url = await uploadInstitutionFile(file)
      otherMaterialUrls.value.push(url)
    }
    syncMaterialsToForm()
  } catch (error) {
    console.error('其他材料上传失败：', error)
    alert(error.message || '其他材料上传失败')
  } finally {
    event.target.value = ''
  }
}

function clearCoverImage() {
  form.coverImageUrl = ''
}

function removeGalleryImage(index) {
  galleryUrls.value.splice(index, 1)
  syncGalleryToForm()
}

function removeOtherMaterial(index) {
  otherMaterialUrls.value.splice(index, 1)
  syncMaterialsToForm()
}

function normalizeFileUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

function getFileName(url) {
  if (!url) return '文件'
  const parts = String(url).split('/')
  return parts[parts.length - 1] || '文件'
}

async function submitApplication() {
  errorMessage.value = ''
  successMessage.value = ''
  syncGalleryToForm()
  syncMaterialsToForm()

  if (!form.institutionName.trim()) {
    errorMessage.value = '请输入机构名称。'
    return
  }

  if (!form.province || !form.city || !form.district) {
    errorMessage.value = '请选择省、市、区县。'
    return
  }

  if (!form.lon || !form.lat) {
    errorMessage.value = '请通过天地图匹配或地图点选设置机构点位。'
    return
  }

  if (!form.contactPhone.trim()) {
    errorMessage.value = '请输入联系电话。'
    return
  }

  try {
    const payload = {
      ...form,
      services: form.services.filter(item => item.serviceType || item.serviceDetail),
      facilities: form.facilities.filter(item => item.facilityName || item.facilityDesc)
    }

    const response = await axios.post('/api/institution-console/applications', payload)
    successMessage.value = response.data.message || '提交成功，等待管理员审核。'
    await loadMine()
  } catch (error) {
    console.error('提交入驻申请失败：', error)
    errorMessage.value = error?.response?.data?.message || error?.response?.data?.error || '提交失败，请确认当前登录账号为机构角色。'
  }
}

function goConsole() {
  router.push('/institution-console')
}
</script>

<style scoped>
.apply-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4faf8 0%, #eef3f5 100%);
  color: #20312d;
}

.apply-topbar {
  min-height: 90px;
  padding: 16px 32px;
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
  display: grid;
  grid-template-columns: 150px 1fr 150px;
  align-items: center;
  gap: 16px;
}

.apply-topbar h1 {
  margin: 0;
  text-align: center;
}

.apply-topbar p {
  margin: 6px 0 0;
  text-align: center;
  opacity: 0.9;
  font-size: 13px;
}

button {
  border: none;
  cursor: pointer;
  font-weight: 900;
}

.apply-topbar button {
  height: 40px;
  border-radius: 999px;
  color: #2e7d6b;
  background: #fff;
}

.apply-main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px;
}

.form-card {
  background: #fff;
  border-radius: 24px;
  border: 1px solid #d9e8e4;
  padding: 24px;
  margin-bottom: 18px;
  box-shadow: 0 12px 28px rgba(31, 80, 70, 0.08);
}

.form-card h2 {
  margin: 0 0 18px;
  color: #2e7d6b;
}

.upload-block {
  margin: 16px 0 20px;
  padding: 18px;
  border-radius: 22px;
  background: #f7fbfa;
  border: 1px solid #e4f0ec;
}

.upload-block h3,
.upload-card h3 {
  margin: 0 0 8px;
  color: #1f6f61;
}

.hint {
  margin-top: -8px;
  color: #6f827d;
  line-height: 1.7;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
  font-weight: 900;
  color: #52606d;
}

input,
textarea,
select {
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
  background: #fff;
  box-sizing: border-box;
}

input,
select {
  height: 42px;
}

textarea {
  min-height: 100px;
  padding-top: 12px;
  line-height: 1.7;
}

.grid-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.grid-row.three {
  grid-template-columns: repeat(3, 1fr);
}

.check-line,
.mini-check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.check-line input,
.mini-check input {
  width: auto;
  height: auto;
}

.address-tools {
  display: grid;
  grid-template-columns: 1fr 140px 120px;
  gap: 10px;
  margin-bottom: 14px;
}

.address-tools button,
.add-btn {
  height: 42px;
  border-radius: 14px;
  background: #2e7d6b;
  color: #fff;
}

.address-tools button.active {
  background: #e8845b;
}

.point-box {
  margin: 10px 0 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: #f3faf7;
  color: #52606d;
}

.location-map {
  height: 360px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid #d9e8e4;
}

.item-row {
  display: grid;
  grid-template-columns: 170px 1fr 90px 70px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.item-row.two {
  grid-template-columns: 1fr 1fr 70px;
}

.item-row .danger {
  height: 38px;
  border-radius: 12px;
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.add-btn {
  padding: 0 18px;
}

.error-box,
.success-box {
  border-radius: 14px;
  padding: 12px;
  font-weight: 900;
  margin-top: 12px;
}

.error-box {
  background: rgba(217, 83, 79, 0.1);
  color: #d9534f;
}

.success-box {
  background: #e3f1ed;
  color: #2e7d6b;
}

.application-card {
  border-radius: 18px;
  background: #f8fbfa;
  border: 1px solid #e3eeea;
  padding: 16px;
  margin-bottom: 12px;
}

.application-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.application-title h3 {
  margin: 0;
}

.application-title span {
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 5px 10px;
  font-weight: 900;
}

.application-card p {
  color: #65756f;
  line-height: 1.7;
}

.upload-grid,
.material-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 12px 0 18px;
}

.upload-card {
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #dce9e5;
}

.upload-card.full {
  grid-column: 1 / -1;
}

.upload-label {
  display: grid;
  gap: 10px;
  cursor: pointer;
}

.upload-label span {
  color: #314d47;
  font-weight: 900;
}

.upload-label input {
  width: 100%;
  height: auto;
  border-radius: 14px;
  border: 1px solid #dce9e5;
  padding: 10px;
  background: #fff;
}

.image-preview.single {
  margin-top: 14px;
}

.image-preview.single img {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: 16px;
  display: block;
}

.image-preview.single button {
  margin-top: 10px;
  height: 34px;
  border-radius: 999px;
  padding: 0 14px;
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.image-preview.gallery {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.gallery-item {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #eef6f3;
}

.gallery-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.gallery-item button {
  position: absolute;
  right: 6px;
  top: 6px;
  border-radius: 999px;
  padding: 4px 8px;
  background: rgba(217, 83, 79, 0.92);
  color: #fff;
  font-size: 12px;
}

.file-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.file-item {
  margin-top: 12px;
  min-height: 42px;
  padding: 9px 12px;
  border-radius: 14px;
  background: #f5faf8;
  border: 1px solid #e2eee9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.file-item span {
  color: #314d47;
  font-size: 13px;
  word-break: break-all;
}

.file-item a,
.file-item button {
  border: none;
  background: transparent;
  color: #2e7d6b;
  font-weight: 900;
  cursor: pointer;
  text-decoration: none;
  margin-left: 8px;
}

.file-item button {
  color: #d9534f;
}


.topbar-spacer {
  display: block;
}

.submit-card {
  border: 2px solid rgba(46, 125, 107, 0.18);
  background: linear-gradient(180deg, #ffffff 0%, #f7fbfa 100%);
}

.bottom-hint {
  margin-top: 0;
}

.bottom-submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, #2e7d6b, #67c6a3);
  color: #fff;
  font-size: 17px;
  box-shadow: 0 14px 28px rgba(46, 125, 107, 0.22);
}

.bottom-submit-btn:hover {
  filter: brightness(0.98);
}

@media (max-width: 900px) {
  .apply-topbar,
  .grid-row,
  .grid-row.three,
  .address-tools,
  .item-row,
  .item-row.two,
  .upload-grid,
  .material-grid {
    grid-template-columns: 1fr;
  }
}
</style>
