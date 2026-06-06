import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'checkin', name: 'CheckIn', component: () => import('../views/CheckIn.vue') },
      { path: 'records', name: 'Records', component: () => import('../views/Records.vue') },
      { path: 'chat', name: 'Chat', component: () => import('../views/Chat.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.path === '/login') return next()
  try {
    const { data } = await axios.get('/api/auth/check')
    if (data.code === 200 && data.data?.loggedIn) return next()
    return next('/login')
  } catch {
    return next('/login')
  }
})

export default router
