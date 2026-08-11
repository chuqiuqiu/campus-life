import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken } from '../utils/auth'

Vue.use(VueRouter)

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/items', name: 'Items', component: () => import('../views/Items.vue') },
  { path: '/items/:id', name: 'ItemDetail', component: () => import('../views/ItemDetail.vue') },
  { path: '/activities', name: 'Activities', component: () => import('../views/Activities.vue') },
  { path: '/activities/:id', name: 'ActivityDetail', component: () => import('../views/ActivityDetail.vue') },
  { path: '/infos', name: 'Infos', component: () => import('../views/Infos.vue') },
  { path: '/infos/:id', name: 'InfoDetail', component: () => import('../views/InfoDetail.vue') },
  { path: '/losts', name: 'Losts', component: () => import('../views/Losts.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

// 全局路由守卫：需要登录的页面校验 token
router.beforeEach((to, from, next) => {
  const publicPages = ['/login', '/', '/items', '/activities', '/infos', '/losts']
  const matchedPublic = publicPages.some(p => to.path === p || to.path.startsWith(p + '/'))
  if (matchedPublic) {
    next()
  } else if (getToken()) {
    next()
  } else {
    next('/login')
  }
})

export default router
