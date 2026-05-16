<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  dimensions: {
    type: Object,
    required: true
  }
})

const chartRef = ref(null)
let chart = null

const dimLabels = {
  burstiness: '句子突发性',
  bigram: '词组多样性',
  lexical: '词汇多样性',
  readability: '平均句长',
  punctuation: '标点多样性',
  sentenceVar: '句长波动',
  hapax: '罕用词比例'
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const data = props.dimensions || {}
  const indicators = Object.entries(dimLabels).map(([key, label]) => ({
    name: label,
    max: 100
  }))

  const values = Object.keys(dimLabels).map(k => data[k] ?? 0)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}: ${p.value}分`
    },
    radar: {
      center: ['50%', '52%'],
      radius: '70%',
      axisName: {
        color: '#4a5568',
        fontSize: 11,
        fontWeight: 600
      },
      indicator: indicators,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      splitLine: { lineStyle: { color: '#edf2f7' } },
      splitArea: {
        areaStyle: {
          color: ['#f7fafc', '#edf2f7', '#f7fafc', '#edf2f7', '#f7fafc']
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '含人率维度' }],
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#c62828', width: 2 },
      areaStyle: { color: 'rgba(198, 40, 40, 0.15)' },
      itemStyle: { color: '#c62828' }
    }]
  })
}

onMounted(renderChart)
watch(() => props.dimensions, renderChart, { deep: true })
</script>

<template>
  <div class="radar-container">
    <div ref="chartRef" class="radar-chart"></div>
  </div>
</template>

<style scoped>
.radar-container {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.radar-chart {
  width: 420px;
  height: 380px;
}

@media (max-width: 768px) {
  .radar-chart {
    width: 340px;
    height: 320px;
  }
}
</style>
