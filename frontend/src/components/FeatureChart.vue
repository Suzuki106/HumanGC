<script setup>
import { computed } from 'vue'

const props = defineProps({
  features: {
    type: Array,
    default: () => []
  }
})

const maxCount = computed(() => {
  if (!props.features.length) return 1
  return Math.max(...props.features.map(f => f.triggerCount || 0), 1)
})

function getBarColor(score) {
  if (score < 30) return '#4caf50'
  if (score < 60) return '#ffc107'
  if (score < 80) return '#ff9800'
  return '#f44336'
}

function barWidth(count) {
  return Math.max((count / maxCount.value) * 100, 3) + '%'
}
</script>

<template>
  <div class="feature-chart" v-if="features.length">
    <div class="chart-title">特征检测详情</div>
    <div class="chart-body">
      <div
        class="chart-row"
        v-for="(feat, idx) in features"
        :key="idx"
      >
        <div class="chart-label">
          <span class="label-name">{{ feat.featureName }}</span>
        </div>
        <div class="chart-bar-wrapper">
          <div class="chart-bar-track">
            <div
              class="chart-bar-fill"
              :style="{
                width: barWidth(feat.triggerCount),
                background: getBarColor(feat.score)
              }"
            ></div>
          </div>
        </div>
        <div class="chart-stats">
          <span class="stat-count">{{ feat.triggerCount }}次</span>
          <span class="stat-score" :style="{ color: getBarColor(feat.score) }">
            {{ feat.score }}分
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feature-chart {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--dark-blue);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--primary-blue);
}

.chart-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-label {
  width: 130px;
  flex-shrink: 0;
  text-align: right;
}

.label-name {
  font-size: 13px;
  color: var(--text-dark);
  font-weight: 500;
}

.chart-bar-wrapper {
  flex: 1;
}

.chart-bar-track {
  width: 100%;
  height: 18px;
  background: #f0f4fa;
  border-radius: 4px;
  overflow: hidden;
}

.chart-bar-fill {
  height: 100%;
  border-radius: 4px;
  min-width: 3px;
  transition: width 0.6s ease;
}

.chart-stats {
  width: 90px;
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.stat-count {
  color: var(--text-light);
}

.stat-score {
  font-weight: 700;
}

@media (max-width: 768px) {
  .chart-label {
    width: 80px;
  }

  .chart-stats {
    width: 70px;
    flex-direction: column;
    gap: 2px;
  }

  .label-name {
    font-size: 11px;
  }
}
</style>
