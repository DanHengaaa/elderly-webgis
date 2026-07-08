import { createApp } from 'vue'
import 'ol/ol.css'
import './style.css'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import { getToken, clearAuth } from './utils/auth'

axios.defaults.baseURL = ''

axios.interceptors.request.use(config => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      clearAuth()
      router.push('/login')
    }

    return Promise.reject(error)
  }
)

createApp(App).use(router).mount('#app')