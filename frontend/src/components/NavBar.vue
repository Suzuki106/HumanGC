<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '../stores/app'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const navLinks = [
  { path: '/', label: '首页' },
  { path: '/detect', label: '含人率检测' },
  { path: '/shitsify', label: '一键变史' },
  { path: '/leaderboard', label: '排行榜' }
]

const isActive = (path) => {
  return route.path === path
}
</script>

<template>
  <nav class="navbar">
    <div class="navbar-inner">
      <div class="navbar-brand" @click="router.push('/')">
        <span class="brand-icon">HG</span>
        <span class="brand-text">HumanGC</span>
      </div>

      <div class="navbar-links">
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="nav-link"
          :class="{ active: isActive(link.path) }"
        >
          {{ link.label }}
        </router-link>
      </div>

      <div class="navbar-actions">
        <span class="anonymous-badge" v-if="appStore.anonymousId">
          <span class="badge-dot"></span>
          匿名用户
        </span>
        <router-link to="/detect" class="btn-nav-free">免费检测</router-link>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: var(--primary-blue);
  box-shadow: 0 2px 8px rgba(26, 58, 107, 0.3);
  height: 60px;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  height: 100%;
  gap: 0;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-right: 32px;
  flex-shrink: 0;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: #fff;
  color: var(--primary-blue);
  font-weight: 900;
  font-size: 14px;
  border-radius: 6px;
  letter-spacing: -0.5px;
}

.brand-text {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 15px;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.nav-link.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 600;
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0;
}

.anonymous-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.badge-dot {
  width: 7px;
  height: 7px;
  background: #5cff7e;
  border-radius: 50%;
}

.btn-nav-free {
  color: #fff;
  background: var(--accent-orange);
  padding: 7px 18px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s;
  white-space: nowrap;
}

.btn-nav-free:hover {
  background: #e55a2b;
}

@media (max-width: 768px) {
  .navbar {
    height: auto;
    min-height: 60px;
  }

  .navbar-inner {
    flex-wrap: wrap;
    padding: 8px 12px;
    gap: 8px;
  }

  .navbar-links {
    order: 3;
    width: 100%;
    overflow-x: auto;
    gap: 2px;
  }

  .nav-link {
    font-size: 13px;
    padding: 6px 10px;
  }

  .anonymous-badge {
    display: none;
  }
}
</style>
