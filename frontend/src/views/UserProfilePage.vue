<template>
  <div class="profile-page">
    <header class="profile-header">
      <div class="brand" @click="goHome">
        <span class="brand-icon">养</span>
        <div>
          <strong>养老服务智慧决策 WebGIS 平台</strong>
          <p>个人中心</p>
        </div>
      </div>
      <button class="ghost-btn" @click="goHome">返回首页</button>
    </header>

    <main class="profile-main">
      <section class="profile-hero">
        <div class="avatar">{{ avatarText }}</div>
        <div class="hero-info">
          <h1>{{ profile.nickname || profile.username || '用户' }}</h1>
          <p>{{ roleText }}</p>
        </div>
      </section>

      <section class="profile-grid">
        <div class="info-card">
          <div class="card-title">
            <h2>账号信息</h2>
            <p>展示当前登录账号的基础信息</p>
          </div>
          <div class="info-list">
            <div class="info-item"><span>用户名</span><strong>{{ profile.username || '未填写' }}</strong></div>
            <div class="info-item"><span>昵称</span><strong>{{ profile.nickname || '未填写' }}</strong></div>
            <div class="info-item"><span>手机号</span><strong>{{ profile.phone || '未填写' }}</strong></div>
            <div class="info-item"><span>邮箱</span><strong>{{ profile.email || '未填写' }}</strong></div>
            <div class="info-item"><span>用户类型</span><strong>{{ roleText }}</strong></div>
            <div v-if="shouldShowInstitution" class="info-item"><span>绑定机构</span><strong>{{ institutionDisplayName }}</strong></div>
          </div>
        </div>

        <div class="form-card">
          <div class="card-title"><h2>修改个人资料</h2><p>可修改昵称、手机号和邮箱</p></div>
          <form class="profile-form" @submit.prevent="submitProfile">
            <label><span>用户名</span><input v-model="profileForm.username" type="text" disabled /></label>
            <label><span>昵称</span><input v-model="profileForm.nickname" type="text" placeholder="请输入昵称" /></label>
            <label><span>手机号</span><input v-model="profileForm.phone" type="text" placeholder="请输入手机号" /></label>
            <label><span>邮箱</span><input v-model="profileForm.email" type="email" placeholder="请输入邮箱" /></label>
            <button class="primary-btn full-btn" type="submit" :disabled="profileSaving">{{ profileSaving ? '正在保存...' : '保存资料' }}</button>
          </form>
        </div>

        <div class="form-card password-card">
          <div class="card-title"><h2>修改密码</h2><p>请填写原密码并设置新密码</p></div>
          <form class="profile-form" @submit.prevent="submitPassword">
            <label><span>原密码</span><input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" /></label>
            <label><span>新密码</span><input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" /></label>
            <label><span>确认新密码</span><input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" /></label>
            <button class="primary-btn full-btn" type="submit" :disabled="passwordSaving">{{ passwordSaving ? '正在修改...' : '修改密码' }}</button>
          </form>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getCurrentUser, saveAuth } from '../utils/auth'

const router = useRouter()
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profile = reactive({ username: '', nickname: '', phone: '', email: '', roleCode: '', institutionId: null, institutionName: '' })
const profileForm = reactive({ username: '', nickname: '', phone: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const roleText = computed(() => {
  switch (profile.roleCode) {
    case 'ADMIN': return '平台管理员'
    case 'INSTITUTION': return '机构用户'
    case 'CUSTOMER': return '普通用户'
    default: return '普通用户'
  }
})
const avatarText = computed(() => String(profile.nickname || profile.username || '养').slice(0, 1))
const shouldShowInstitution = computed(() => profile.roleCode === 'INSTITUTION')
const institutionDisplayName = computed(() => profile.institutionName || (profile.institutionId ? '已绑定机构' : '暂未绑定机构'))

onMounted(() => loadProfile())

async function loadProfile() {
  try {
    const response = await axios.get('/api/auth/me')
    applyProfile(response.data)
  } catch (error) {
    console.error('加载个人信息失败：', error)
    const localUser = getCurrentUser()
    if (localUser) applyProfile(localUser)
    else router.push('/login')
  }
}

function applyProfile(user) {
  profile.username = user?.username || ''
  profile.nickname = user?.nickname || ''
  profile.phone = user?.phone || ''
  profile.email = user?.email || ''
  profile.roleCode = user?.roleCode || 'CUSTOMER'
  profile.institutionId = user?.institutionId || null
  profile.institutionName = user?.institutionName || ''
  profileForm.username = profile.username
  profileForm.nickname = profile.nickname
  profileForm.phone = profile.phone
  profileForm.email = profile.email
}

async function submitProfile() {
  profileSaving.value = true
  try {
    const response = await axios.put('/api/auth/profile', { nickname: profileForm.nickname, phone: profileForm.phone, email: profileForm.email })
    const data = response.data || {}
    if (data.token && data.user) {
      saveAuth(data.token, data.user)
      applyProfile(data.user)
    } else if (data.user) applyProfile(data.user)
    else await loadProfile()
    alert('个人资料保存成功。')
  } catch (error) {
    console.error('保存个人资料失败：', error)
    alert(getErrorMessage(error, '个人资料保存失败，请稍后再试。'))
  } finally {
    profileSaving.value = false
  }
}

async function submitPassword() {
  if (!passwordForm.oldPassword) return alert('请输入原密码。')
  if (!passwordForm.newPassword) return alert('请输入新密码。')
  if (passwordForm.newPassword.length < 6) return alert('新密码长度不能少于 6 位。')
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return alert('两次输入的新密码不一致。')
  passwordSaving.value = true
  try {
    await axios.put('/api/auth/password', { oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    alert('密码修改成功。')
  } catch (error) {
    console.error('修改密码失败：', error)
    alert(getErrorMessage(error, '密码修改失败，请检查原密码是否正确。'))
  } finally {
    passwordSaving.value = false
  }
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.response?.data?.error || fallback
}
function goHome() { router.push('/map') }
</script>

<style scoped>
.profile-page{min-height:100vh;background:radial-gradient(circle at top left,rgba(46,125,107,.18),transparent 32%),linear-gradient(180deg,#f4faf8 0%,#eef5f2 100%);padding:24px 34px 56px;color:#20312d;box-sizing:border-box}.profile-header{max-width:1180px;height:74px;margin:0 auto 24px;padding:0 24px;border-radius:26px;background:rgba(255,255,255,.96);box-shadow:0 16px 40px rgba(30,80,70,.12);border:1px solid rgba(46,125,107,.1);display:flex;align-items:center;justify-content:space-between}.brand{display:flex;align-items:center;gap:14px;cursor:pointer}.brand-icon{width:46px;height:46px;border-radius:16px;background:linear-gradient(135deg,#2e7d6b,#67c6a3);display:grid;place-items:center;color:#fff;font-size:22px;font-weight:900}.brand strong{display:block;color:#1f6f61;font-size:20px}.brand p{margin:4px 0 0;color:#62736f;font-size:13px}.ghost-btn{height:38px;padding:0 18px;border-radius:999px;border:1px solid rgba(46,125,107,.2);background:#fff;color:#2e7d6b;font-weight:900;cursor:pointer}.profile-main{max-width:1180px;margin:0 auto}.profile-hero{min-height:150px;padding:30px;border-radius:30px;background:linear-gradient(135deg,rgba(46,125,107,.96),rgba(103,198,163,.9));box-shadow:0 18px 45px rgba(46,125,107,.22);display:flex;align-items:center;gap:22px;color:#fff}.avatar{width:78px;height:78px;border-radius:28px;background:rgba(255,255,255,.2);border:1px solid rgba(255,255,255,.32);display:grid;place-items:center;font-size:34px;font-weight:900}.hero-info h1{margin:0;font-size:30px;font-weight:900}.hero-info p{margin:8px 0 0;font-size:15px;opacity:.92}.profile-grid{margin-top:22px;display:grid;grid-template-columns:.9fr 1.1fr;gap:22px;align-items:start}.info-card,.form-card{background:rgba(255,255,255,.97);border-radius:28px;padding:26px;border:1px solid rgba(46,125,107,.1);box-shadow:0 18px 45px rgba(30,80,70,.09)}.password-card{grid-column:2}.card-title{margin-bottom:20px}.card-title h2{margin:0;color:#1f6f61;font-size:22px}.card-title p{margin:6px 0 0;color:#788b86;font-size:13px}.info-list{display:grid;gap:14px}.info-item{min-height:58px;padding:13px 16px;border-radius:18px;background:#f6fbf9;border:1px solid #e4f0ec;display:flex;align-items:center;justify-content:space-between;gap:16px}.info-item span{color:#6f827d;font-size:14px;flex-shrink:0}.info-item strong{color:#20312d;font-size:15px;text-align:right;word-break:break-all}.profile-form{display:grid;gap:16px}.profile-form label{display:grid;gap:8px}.profile-form label span{color:#415b55;font-size:14px;font-weight:800}.profile-form input{height:46px;border-radius:16px;border:1px solid #dce8e4;outline:none;padding:0 14px;color:#20312d;background:#fff;font-size:14px;box-sizing:border-box}.profile-form input:focus{border-color:#2e7d6b;box-shadow:0 0 0 3px rgba(46,125,107,.12)}.profile-form input:disabled{color:#81928d;background:#f3f7f5;cursor:not-allowed}.primary-btn{height:44px;border:none;border-radius:16px;background:linear-gradient(135deg,#2e7d6b,#67c6a3);color:#fff;font-weight:900;cursor:pointer;box-shadow:0 12px 26px rgba(46,125,107,.18)}.primary-btn:disabled{opacity:.65;cursor:not-allowed}.full-btn{width:100%;margin-top:4px}@media(max-width:900px){.profile-grid{grid-template-columns:1fr}.password-card{grid-column:auto}}@media(max-width:640px){.profile-page{padding:16px}.profile-header{height:auto;padding:16px;flex-direction:column;align-items:flex-start;gap:14px}.profile-hero{padding:22px;flex-direction:column;align-items:flex-start}.info-item{align-items:flex-start;flex-direction:column}.info-item strong{text-align:left}}
</style>
