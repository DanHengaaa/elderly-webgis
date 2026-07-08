<template>
  <div class="apply-page">
    <header class="apply-topbar">
      <button @click="goConsole">← 返回工作台</button>
      <h1>机构入驻申请</h1>
      <button @click="submitApplication">提交申请</button>
    </header>

    <main class="apply-main">
      <section class="form-card">
        <h2>基础信息</h2>

        <label>
          机构名称
          <input v-model="form.institutionName" placeholder="请输入养老机构名称" />
        </label>

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

        <div class="grid-row">
          <label>
            所在区县
            <input v-model="form.district" placeholder="例如：鼓楼区" />
          </label>

          <label>
            详细地址
            <input v-model="form.address" placeholder="请输入机构地址" />
          </label>
        </div>

        <div class="grid-row">
          <label>
            经度，可选
            <input v-model.number="form.lon" type="number" placeholder="118.78" />
          </label>

          <label>
            纬度，可选
            <input v-model.number="form.lat" type="number" placeholder="32.05" />
          </label>
        </div>

        <label>
          机构介绍
          <textarea v-model="form.description" placeholder="请简要介绍机构服务、床位、护理能力等"></textarea>
        </label>
      </section>

      <section class="form-card">
        <h2>资质材料</h2>

        <label>
          营业执照 URL
          <input v-model="form.licenseUrl" placeholder="可先填写图片 URL，后续可接上传" />
        </label>

        <label>
          备案证明 URL
          <input v-model="form.recordCertificateUrl" placeholder="可先填写图片 URL" />
        </label>

        <label>
          其他材料 URL
          <input v-model="form.otherMaterialUrl" placeholder="可选" />
        </label>

        <div v-if="errorMessage" class="error-box">
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-box">
          {{ successMessage }}
        </div>
      </section>

      <section class="form-card">
        <h2>我的申请记录</h2>

        <article v-for="item in applications" :key="item.id" class="application-card">
          <h3>{{ item.institutionName }}</h3>
          <p>{{ item.district }} · {{ item.address }}</p>
          <span>{{ item.statusText }}</span>
          <p v-if="item.rejectReason">驳回原因：{{ item.rejectReason }}</p>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const errorMessage = ref('')
const successMessage = ref('')
const applications = ref([])

const form = reactive({
  institutionName: '',
  contactPerson: '',
  contactPhone: '',
  district: '',
  address: '',
  lon: null,
  lat: null,
  licenseUrl: '',
  recordCertificateUrl: '',
  otherMaterialUrl: '',
  description: ''
})

onMounted(() => {
  loadMine()
})

async function loadMine() {
  const response = await axios.get('/api/institution-console/applications/mine')
  applications.value = response.data || []
}

async function submitApplication() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!form.institutionName.trim()) {
    errorMessage.value = '请输入机构名称。'
    return
  }

  if (!form.contactPhone.trim()) {
    errorMessage.value = '请输入联系电话。'
    return
  }

  try {
    const response = await axios.post('/api/institution-console/applications', form)

    successMessage.value = response.data.message || '提交成功，等待管理员审核。'
    await loadMine()
  } catch (error) {
    console.error('提交入驻申请失败：', error)
    errorMessage.value = '提交失败，请确认当前登录账号为机构角色。'
  }
}

function goConsole() {
  router.push('/institution-console')
}
</script>

<style scoped>
.apply-page {
  min-height: 100vh;
  background: #eef3f5;
}

.apply-topbar {
  height: 82px;
  padding: 0 32px;
  background: linear-gradient(135deg, #2e7d6b, #e8845b);
  color: #fff;
  display: grid;
  grid-template-columns: 150px 1fr 150px;
  align-items: center;
}

.apply-topbar h1 {
  text-align: center;
}

.apply-topbar button {
  height: 40px;
  border: none;
  border-radius: 999px;
  font-weight: 900;
  color: #2e7d6b;
  background: #fff;
  cursor: pointer;
}

.apply-main {
  max-width: 980px;
  margin: 0 auto;
  padding: 28px;
}

.form-card {
  background: #fff;
  border-radius: 24px;
  border: 1px solid #d9e0e8;
  padding: 24px;
  margin-bottom: 18px;
  box-shadow: 0 12px 28px rgba(31, 41, 51, 0.08);
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
  font-weight: 900;
  color: #52606d;
}

input,
textarea {
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
}

input {
  height: 42px;
}

textarea {
  min-height: 110px;
  padding-top: 12px;
}

.grid-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.error-box,
.success-box {
  border-radius: 14px;
  padding: 12px;
  font-weight: 900;
}

.error-box {
  background: rgba(217,83,79,0.1);
  color: #d9534f;
}

.success-box {
  background: #e3f1ed;
  color: #2e7d6b;
}

.application-card {
  border-top: 1px solid #eef2f5;
  padding: 14px 0;
}

.application-card h3 {
  margin: 0;
}

.application-card p {
  color: #52606d;
}

.application-card span {
  display: inline-block;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  padding: 6px 10px;
  font-weight: 900;
}
</style>