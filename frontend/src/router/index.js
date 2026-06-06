import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkin',
    name: 'CheckIn',
    component: () => import('@/views/CheckInView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/RecordsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    // Catch-all redirect to home
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard: check auth status before each route
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // On first load, check if user is already logged in (via session cookie)
  if (!authStore.authChecked) {
    await authStore.checkAuth()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    // Route requires login but user is not authenticated
    next({ name: 'Login' })
  } else if (to.meta.requiresGuest && authStore.isLoggedIn) {
    // Route is guest-only (e.g. login page) but user is already logged in
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
