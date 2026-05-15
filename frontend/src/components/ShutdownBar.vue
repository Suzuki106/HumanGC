<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const router = useRouter()
const appStore = useAppStore()

const progressPercent = ref(0)
const daysRemaining = ref(0)
const hoursRemaining = ref(0)

let refreshTimer = null

function updateDisplay() {
  const status = appStore.serverStatus
  progressPercent.value = status.progressPercent || 0
  daysRemaining.value = status.daysRemaining || 0
  hoursRemaining.value = status.hoursRemaining || 0
}

onMounted(() => {
  updateDisplay()
  appStore.fetchServerStatus().then(updateDisplay)
  refreshTimer = setInterval(() => {
    appStore.fetchServerStatus().then(updateDisplay)
  }, 60000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <div class="shutdown-bar" >
    <div class="shutdown-inner">
      <span class="shutdown-icon">&#9888;&#65039;</span>
      <span class="shutdown-text">
        日费 <strong>¥3</strong>，余额 <strong>¥{{ (appStore.serverStatus.donatedAmount || 0) + 100 }}</strong>
        &nbsp;&middot;&nbsp; 可撑 <strong>{{ daysRemaining }}</strong>天<strong>{{ hoursRemaining }}</strong>小时
      </span>
      <div class="shutdown-progress">
        <div class="shutdown-progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <router-link to="/donate" class="shutdown-link">
        不想让本站倒闭？点击资助续命 &rarr;
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.shutdown-bar {
  background: var(--warning-yellow);
  border-bottom: 2px solid #e6a800;
}

.shutdown-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #7a5e00;
}

.shutdown-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.shutdown-text {
  flex-shrink: 0;
}

.shutdown-text strong {
  color: #5c3d00;
}

.shutdown-progress {
  flex: 1;
  min-width: 120px;
  max-width: 300px;
  height: 8px;
  background: #f0d060;
  border-radius: 4px;
  overflow: hidden;
}

.shutdown-progress-fill {
  height: 100%;
  background: #d4832a;
  border-radius: 4px;
  transition: width 1s ease;
}

.shutdown-link {
  color: #8b0000;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.shutdown-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .shutdown-inner {
    padding: 6px 12px;
    gap: 6px;
    font-size: 12px;
  }

  .shutdown-progress {
    min-width: 80px;
    height: 6px;
  }
}
</style>
