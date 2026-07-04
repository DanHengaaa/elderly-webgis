import { createRouter, createWebHistory } from 'vue-router'
import MapHome from '../views/MapHome.vue'
import InstitutionDetail from '../views/InstitutionDetail.vue'

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
    }
  ]
})

export default router