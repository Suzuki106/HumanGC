<script setup>
import { ref } from 'vue'
import { shitsify } from '../api'
import FileUploader from '../components/FileUploader.vue'

const paperId = ref(null)
const selectedStyle = ref('undergrad')
const isGenerating = ref(false)
const result = ref(null)
const errorMsg = ref('')

const styles = [
  {
    id: 'undergrad',
    name: '本科生DDL版',
    desc: '口语化表达 + 半截句 + 偶尔错别字，完美模拟深夜赶Deadline的大四学生',
    icon: '&#128218;'
  },
  {
    id: 'advisor_headache',
    name: '导师看了头疼版',
    desc: '官话堆砌 + 超级长难句 + 逻辑跳跃，保证你的导师读完只想让你延毕',
    icon: '&#129300;'
  },
  {
    id: 'cnki_stitch',
    name: '知网缝合怪版',
    desc: '术语堆砌 + 引用混乱 + 翻译腔，达到查重率0%但学术价值也为0的境界',
    icon: '&#129504;'
  },
  {
    id: 'real_human',
    name: '真实人类版',
    desc: '保留部分人工痕迹 + 风格跳变，模仿那种"一半是ChatGPT，一半是手动修改"的真实感',
    icon: '&#128104;&#8205;&#127891;'
  }
]

async function onFileUploaded(id) {
  paperId.value = id
  result.value = null
  errorMsg.value = ''
}

async function doShitsify() {
  if (!paperId.value) return
  isGenerating.value = true
  errorMsg.value = ''
  result.value = null

  try {
    const res = await shitsify(paperId.value, selectedStyle.value)
    result.value = res.data
  } catch {
    // Mock data for demo
    result.value = {
      originalText: '摘要：本文提出了一种基于深度学习的自然语言处理模型，该模型在多个基准数据集上取得了优异的性能表现。实验结果表明，我们的方法在准确率和召回率方面均优于现有方法。本研究为自然语言处理领域提供了新的研究思路。',
      shitsifiedText: '摘要：咱们搞了个深度学习的东西，就是拿来处理自然语言的。反正数据集上跑了一下，效果好得很，比别的方法都牛逼多了。然后就觉得，嗯，这波操作给NLP领域算是提供了一个新方向吧。对了，代码在GitHub上，喜欢的话记得star（虽然可能用不了）。',
      highlights: [
        { start: 12, end: 18, feature: '口语化表达' },
        { start: 45, end: 52, feature: '不专业用词' },
        { start: 70, end: 75, feature: '逻辑断裂' }
      ]
    }
  }
  isGenerating.value = false
}
</script>

<template>
  <div class="shitsify-page">
    <div class="container">
      <h1 class="page-title">一键变史</h1>
      <p class="page-desc">
        将您的"含人率高"论文一键转化为低含人率的"屎山论文"。<br/>
        四种经典模板，覆盖不同学术场景，让您的论文真正做到"非人化"。
      </p>

      <!-- Upload -->
      <div class="card section-card">
        <h3 class="card-title">第一步：上传论文</h3>
        <FileUploader @file-uploaded="onFileUploaded" />
      </div>

      <!-- Style Selection -->
      <div class="card section-card" v-if="paperId">
        <h3 class="card-title">第二步：选择风格模板</h3>
        <div class="style-grid">
          <div
            v-for="style in styles"
            :key="style.id"
            class="style-card"
            :class="{ selected: selectedStyle === style.id }"
            @click="selectedStyle = style.id"
          >
            <div class="style-icon" v-html="style.icon"></div>
            <div class="style-info">
              <h4 class="style-name">{{ style.name }}</h4>
              <p class="style-desc">{{ style.desc }}</p>
            </div>
            <div class="style-check" v-if="selectedStyle === style.id">&#10004;</div>
          </div>
        </div>

        <div class="generate-action">
          <button
            class="btn-generate"
            :disabled="isGenerating"
            @click="doShitsify"
          >
            <template v-if="isGenerating">
              <span class="spinner"></span> 正在生成屎山论文...
            </template>
            <template v-else>
              &#128169; 一键变史
            </template>
          </button>
        </div>
      </div>

      <!-- Results: Side by Side -->
      <div class="results-section" v-if="result">
        <h3 class="card-title" style="margin-bottom: 16px;">生成结果</h3>
        <div class="comparison">
          <div class="comp-panel">
            <div class="comp-header">
              <h4>原文</h4>
              <button class="btn-dl" @click="() => {}">下载</button>
            </div>
            <div class="comp-body">
              <p>{{ result.originalText }}</p>
            </div>
          </div>
          <div class="comp-panel comp-right">
            <div class="comp-header">
              <h4>屎山版</h4>
              <button class="btn-dl" @click="() => {}">下载</button>
            </div>
            <div class="comp-body">
              <p>{{ result.shitsifiedText }}</p>
            </div>
          </div>
        </div>

        <div class="card highlight-section" v-if="result.highlights && result.highlights.length">
          <h4 class="highlight-title">注入特征标注</h4>
          <div class="highlight-list">
            <div
              v-for="(hl, idx) in result.highlights"
              :key="idx"
              class="highlight-item"
            >
              <span class="hl-pos">位置 {{ hl.start }}-{{ hl.end }}</span>
              <span class="hl-tag">{{ hl.feature }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shitsify-page {
  padding-top: 60px;
  min-height: 100vh;
  background: #f5f7fb;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--dark-blue);
  text-align: center;
  margin: 32px 0 8px;
}

.page-desc {
  text-align: center;
  color: var(--text-light);
  font-size: 14px;
  line-height: 1.8;
  margin: 0 0 32px;
}

.section-card {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--primary-blue);
}

/* Style Grid */
.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.style-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.style-card:hover {
  border-color: var(--primary-blue);
  background: rgba(26, 86, 184, 0.03);
}

.style-card.selected {
  border-color: var(--primary-blue);
  background: rgba(26, 86, 184, 0.06);
}

.style-icon {
  font-size: 28px;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4fa;
  border-radius: 8px;
}

.style-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0 0 4px;
}

.style-desc {
  font-size: 12px;
  color: var(--text-light);
  line-height: 1.6;
  margin: 0;
}

.style-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: var(--primary-blue);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

/* Generate */
.generate-action {
  text-align: center;
  padding: 8px 0;
}

.btn-generate {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  padding: 14px 48px;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(255, 107, 53, 0.35);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-generate:hover:not(:disabled) {
  background: #e55a2b;
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 107, 53, 0.5);
}

.btn-generate:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Comparison */
.comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.comp-panel {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f0f4fa;
  border-bottom: 1px solid var(--border-color);
}

.comp-header h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0;
}

.comp-right .comp-header {
  background: #fff3e0;
}

.btn-dl {
  background: var(--primary-blue);
  color: #fff;
  border: none;
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.comp-body {
  padding: 16px;
}

.comp-body p {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-dark);
  margin: 0;
  white-space: pre-wrap;
}

/* Highlights */
.highlight-section {
  margin-bottom: 20px;
}

.highlight-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0 0 12px;
}

.highlight-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.highlight-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff3e0;
  border-radius: 6px;
  font-size: 12px;
}

.hl-pos {
  color: var(--text-light);
}

.hl-tag {
  background: var(--accent-orange);
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .style-grid { grid-template-columns: 1fr; }
  .comparison { grid-template-columns: 1fr; }
  .page-title { font-size: 22px; }
}
</style>
