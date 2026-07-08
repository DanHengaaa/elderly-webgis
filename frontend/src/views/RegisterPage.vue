<template>
  <div class="register-page">
    <section class="register-card">
      <div class="brand">
        <h1>注册账号</h1>
        <p>创建客户或机构账号，接入平台服务</p>
      </div>

      <label>
        用户名
        <input v-model="form.username" placeholder="至少 3 个字符" />
      </label>

      <label>
        昵称
        <input v-model="form.nickname" placeholder="例如：家属小林" />
      </label>

      <label>
        手机号
        <input v-model="form.phone" placeholder="可选" />
      </label>

      <label>
        密码
        <input v-model="form.password" type="password" placeholder="至少 6 位" />
      </label>

      <label>
        账号类型
        <select v-model="form.roleCode">
          <option value="CUSTOMER">客户 / 家属 / 老人</option>
          <option value="INSTITUTION">养老机构</option>
        </select>
      </label>

      <button class="register-btn" @click="register">
        注册并登录
      </button>

      <p v-if="errorMessage" class="error-text">
        {{ errorMessage }}
      </p>

      <div class="footer-row">
        <span>已有账号？</span>
        <button @click="goLogin">去登录</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { saveAuth } from '../utils/auth'

const router = useRouter()
const errorMessage = ref('')

const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  roleCode: 'CUSTOMER'
})

async function register() {
  errorMessage.value = ''

  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名。'
    return
  }

  if (!form.password.trim()) {
    errorMessage.value = '请输入密码。'
    return
  }

  try {
    const response = await axios.post('/api/auth/register', {
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      phone: form.phone.trim(),
      password: form.password.trim(),
      roleCode: form.roleCode
    })

    saveAuth(response.data.token, response.data.user)

    if (response.data.user.roleCode === 'INSTITUTION') {
      router.push('/institution-console')
    } else {
      router.push('/map')
    }
  } catch (error) {
    console.error('注册失败：', error)
    errorMessage.value = error.response?.data?.message || '注册失败，请稍后重试。'
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(232, 132, 91, 0.18), transparent 32%),
    linear-gradient(135deg, #e3f1ed 0%, #f7f8f6 55%, #eef3f5 100%);
  color: #1f2933;
}

.register-card {
  width: 430px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d9e0e8;
  box-shadow: 0 24px 60px rgba(31, 41, 51, 0.14);
  padding: 32px;
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.brand h1 {
  margin: 0;
  color: #2e7d6b;
  font-size: 30px;
  font-weight: 900;
}

.brand p {
  margin: 9px 0 0;
  color: #52606d;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
  color: #52606d;
  font-weight: 900;
}

input,
select {
  height: 44px;
  border: 1px solid #d9e0e8;
  border-radius: 14px;
  padding: 0 12px;
  outline: none;
  font-size: 15px;
}

input:focus,
select:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

.register-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 16px;
  background: #2e7d6b;
  color: #fff;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
}

.error-text {
  margin: 14px 0 0;
  color: #d9534f;
  font-weight: 800;
}

.footer-row {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 10px;
  color: #52606d;
}

.footer-row button {
  border: none;
  background: transparent;
  color: #2e7d6b;
  font-weight: 900;
  cursor: pointer;
}
</style>