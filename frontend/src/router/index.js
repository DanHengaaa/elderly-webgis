import { createRouter, createWebHistory } from 'vue-router'

import MapHome from '../views/MapHome.vue'
import InstitutionDetail from '../views/InstitutionDetail.vue'
import SmartAssessment from '../views/SmartAssessment.vue'
import RecommendationPage from '../views/RecommendationPage.vue'
import AiAssistantPage from '../views/AiAssistantPage.vue'
import CommunityHome from '../views/CommunityHome.vue'
import CommunityPostDetail from '../views/CommunityPostDetail.vue'
import CommunityPublish from '../views/CommunityPublish.vue'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'
import UserProfilePage from '../views/UserProfilePage.vue'
import AdminDashboard from '../views/AdminDashboard.vue'
import InstitutionConsole from '../views/InstitutionConsole.vue'
import { getCurrentUser, isLoggedIn } from '../utils/auth'
import AdminCommunityReview from '../views/AdminCommunityReview.vue'
import AdminInstitutionReview from '../views/AdminInstitutionReview.vue'
import InstitutionApplication from '../views/InstitutionApplication.vue'


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
    },
    {
      path: '/recommend',
      name: 'RecommendationPage',
      component: RecommendationPage
    },
  {
      path: '/recommendations',
      redirect: '/recommend'
    },
    {
      path: '/ai-assistant',
      name: 'AiAssistantPage',
      component: AiAssistantPage
    },
    {
  path: '/community',
  name: 'CommunityHome',
  component: CommunityHome
},
{
  path: '/community/posts/:id',
  name: 'CommunityPostDetail',
  component: CommunityPostDetail,
  props: true
},
{
  path: '/community/publish',
  name: 'CommunityPublish',
  component: CommunityPublish
},
{
  path: '/login',
  name: 'LoginPage',
  component: LoginPage
},
{
  path: '/register',
  name: 'RegisterPage',
  component: RegisterPage
},
{
  path: '/profile',
  name: 'UserProfilePage',
  component: UserProfilePage,
  meta: {
    requiresAuth: true
  }
},
{
  path: '/admin',
  name: 'AdminDashboard',
  component: AdminDashboard,
  meta: {
    requiresAuth: true,
    roles: ['ADMIN']
  }
},
{
  path: '/institution-console',
  name: 'InstitutionConsole',
  component: InstitutionConsole,
  meta: {
    requiresAuth: true,
    roles: ['INSTITUTION']
  }
},{
  path: '/admin/community-review',
  name: 'AdminCommunityReview',
  component: AdminCommunityReview,
  meta: {
    requiresAuth: true,
    roles: ['ADMIN']
  }
},
{
  path: '/admin/institution-review',
  name: 'AdminInstitutionReview',
  component: AdminInstitutionReview,
  meta: {
    requiresAuth: true,
    roles: ['ADMIN']
  }
},
{
  path: '/institution-application',
  name: 'InstitutionApplication',
  component: InstitutionApplication,
  meta: {
    requiresAuth: true,
    roles: ['INSTITUTION']
  }
}

  ]
  
})
router.beforeEach((to, from, next) => {
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  if (!isLoggedIn()) {
    next({
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    })
    return
  }

  const user = getCurrentUser()

  if (to.meta.roles && !to.meta.roles.includes(user?.roleCode)) {
    if (user?.roleCode === 'ADMIN') {
      next('/admin')
      return
    }

    if (user?.roleCode === 'INSTITUTION') {
      next('/institution-console')
      return
    }

    next('/map')
    return
  }

  next()
})
export default router