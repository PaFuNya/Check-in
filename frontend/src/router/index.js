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
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/HomeView.vue'),
      },
      {
        path: 'checkin',
        name: 'CheckIn',
        component: () => import('@/views/CheckInView.vue'),
      },
      {
        path: 'records',
        name: 'Records',
        component: () => import('@/views/RecordsView.vue'),
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/ChatView.vue'),
      },
    ]
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

  // Check if the route or any parent requires auth
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresGuest = to.matched.some(record => record.meta.requiresGuest)

  if (requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login' })
  } else if (requiresGuest && authStore.isLoggedIn) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
