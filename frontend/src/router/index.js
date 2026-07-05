import { createRouter, createWebHistory } from 'vue-router'
import MapHome from '../views/MapHome.vue'
import InstitutionDetail from '../views/InstitutionDetail.vue'
import SmartAssessment from '../views/SmartAssessment.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/map'
    },
    {
      path: '/map',
      name: 'MapHome',
      component: MapHome
    },
    {
      path: '/institutions/:id',
      name: 'InstitutionDetail',
      component: InstitutionDetail,
      props: true
    },
    {
      path: '/assessment',
      name: 'SmartAssessment',
      component: SmartAssessment
    }
  ]
})

export default router