<script setup>
import { computed } from 'vue'

const props = defineProps({
  rate: {
    type: Number,
    default: 0
  }
})

const clampedRate = computed(() => Math.max(0, Math.min(100, props.rate)))

const circumference = 2 * Math.PI * 80
const dashOffset = computed(() => {
  return circumference - (clampedRate.value / 100) * circumference
})

const gaugeColor = computed(() => {
  const r = clampedRate.value
  if (r < 20) return '#4caf50'
  if (r < 50) return '#ffc107'
  if (r < 80) return '#ff9800'
  return '#f44336'
})

const rateLabel = computed(() => {
  const r = clampedRate.value
  if (r < 10) return '这论文跟人有关系吗？'
  if (r < 30) return '勉强能看出是人写的'
  if (r < 60) return '你的论文含人量中等'
  if (r < 80) return '你太像人了，需要降人率！'
  return '你就是人类本类！赶紧降人率！'
})
</script>

<template>
  <div class="rate-gauge">
    <div class="gauge-wrapper">
      <svg class="gauge-svg" viewBox="0 0 200 200" width="200" height="200">
        <circle
          cx="100" cy="100" r="80"
          fill="none"
          stroke="#e8edf5"
          stroke-width="14"
        />
        <circle
          cx="100" cy="100" r="80"
          fill="none"
          :stroke="gaugeColor"
          stroke-width="14"
          stroke-linecap="round"
          :stroke-dasharray="circumference"
          :stroke-dashoffset="dashOffset"
          transform="rotate(-90 100 100)"
          class="gauge-arc"
        />
      </svg>
      <div class="gauge-center">
        <span class="rate-value" :style="{ color: gaugeColor }">
          {{ clampedRate }}<small>%</small>
        </span>
        <span class="rate-unit">含人率</span>
      </div>
    </div>
    <p class="rate-label-text" :style="{ color: gaugeColor }">
      {{ rateLabel }}
    </p>
  </div>
</template>

<style scoped>
.rate-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.gauge-wrapper {
  position: relative;
  width: 200px;
  height: 200px;
}

.gauge-svg {
  width: 100%;
  height: 100%;
}

.gauge-arc {
  transition: stroke-dashoffset 1s ease, stroke 0.6s ease;
}

.gauge-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.rate-value {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
}

.rate-value small {
  font-size: 20px;
}

.rate-unit {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
}

.rate-label-text {
  font-size: 15px;
  font-weight: 600;
  text-align: center;
  margin: 0;
  padding: 8px 20px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 6px;
}

@media (max-width: 768px) {
  .gauge-wrapper {
    width: 150px;
    height: 150px;
  }

  .gauge-svg {
    width: 150px;
    height: 150px;
  }

  .rate-value {
    font-size: 30px;
  }
}
</style>
