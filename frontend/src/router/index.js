import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/detect',
    name: 'Detect',
    component: () => import('../views/Detect.vue')
  },
  {
    path: '/shitsify',
    name: 'Shitsify',
    component: () => import('../views/Shitsify.vue')
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('../views/Leaderboard.vue')
  },
  {
    path: '/review/:id',
    name: 'Review',
    component: () => import('../views/Review.vue')
  },
  {
    path: '/donate',
    name: 'Donate',
    component: () => import('../views/Donate.vue')
  },
  {
    path: '/paper/:id',
    name: 'PaperDetail',
    component: () => import('../views/PaperDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
