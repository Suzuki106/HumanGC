<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { detect, getReview, getUserPapers } from '../api'
import FileUploader from '../components/FileUploader.vue'
import RateGauge from '../components/RateGauge.vue'
import FeatureChart from '../components/FeatureChart.vue'

const appStore = useAppStore()

const paperId = ref(null)
const detectResult = ref(null)
const isDetecting = ref(false)
const uploadHistory = ref([])

const humanRate = ref(0)
const features = ref([])
const summaryText = ref('')
const reviewText = ref('')
const isReviewLoading = ref(false)

async function onFileUploaded(id) {
  paperId.value = id
  isDetecting.value = false
  detectResult.value = null
}

function generateMockSummary(rate, feats) {
  const prefixes = [
    '经Humangc鉴定，', '经过9维深度扫描，', '屎山鉴定师报告：',
    'HumanGC检测报告出炉：', '含人率雷达扫描结果：', '系统分析完毕，结论如下：',
    '检测完成！Humangc鉴定书：', '经过全方位审查，', '屎山指数分析报告：'
  ]
  const verdicts = rate >= 80 ? ['鉴定完毕，这论文是人写的没跑了！', '含人率爆表！作者你暴露了！', '铁证如山——这就是人类手笔！', '警报！检测到大量人类痕迹！', '兄弟你是人吧？别装了。', '人味冲天！这论文简直是用人类逻辑羞辱AI。'] :
    rate >= 60 ? ['七成把握——这玩意儿出自人手。', '人味偏重，AI看了直摇头。', '大概率是人类写的，句子通顺得令AI不适。', '人类痕迹占上风，建议回炉重造。', '含人量超标预警！你是不是偷偷亲自写的？', '文笔不错——这就是问题，太像人写的了。'] :
    rate >= 40 ? ['人模AI样的，处于量子叠加态。', '薛定谔的论文——说不清是人写的还是AI凑的。', '五五开，堪称人机缝合的巅峰之作。', '人味AI味对半开，不人不鬼的尴尬状态。', '暧昧地带——既不够人也不够AI。', '半人半AI，学术界最尴尬的存在。'] :
    rate >= 20 ? ['AI写的吧？但还残留着人类的倔强。', '大概率AI生成，不过有几处人类修改的痕迹。', 'AI感很强，但角落里藏着人类的小心思。', '机器生成为主，人类摸鱼为辅。', 'AI代写无疑，只是作者忍不住改了几个字。', '八成AI两成人，缝合得还不错。'] :
    ['纯度感人！这是一篇几乎没有人类痕迹的神作！', '绝了！AI界的标杆，人类看了沉默。', '满分答辩！建议收录进AI论文博物馆。', '完美！这就是AI该有的样子。', '恭喜！您已成功去人化！', '太强了！纯AI写作的教科书级别示范。']

  const topFeats = [...feats].sort((a, b) => b.triggerCount - a.triggerCount).slice(0, 3)
  const intro = [' 其中，', ' 罪魁祸首是：', ' 主要扣分项：', ' 关键证据：', ' 破案线索：', ' 人味来源：', ' 重点标注：', ' 数据说话：']

  let s = prefixes[Math.floor(Math.random() * prefixes.length)]
  s += verdicts[Math.floor(Math.random() * verdicts.length)]
  if (topFeats.length && topFeats[0].triggerCount > 0) {
    s += intro[Math.floor(Math.random() * intro.length)]
    s += topFeats.filter(f => f.triggerCount > 0).map(f => f.featureName + '×' + f.triggerCount).join('、')
    s += '。'
  }
  return s
}

async function startDetect() {
  if (!paperId.value) return
  isDetecting.value = true
  detectResult.value = null
  reviewText.value = ''

  try {
    const res = await detect(paperId.value)
    detectResult.value = res.data
    humanRate.value = res.data.humanRate || res.data.rate || 0
    features.value = res.data.features || generateMockFeatures(humanRate.value)
    summaryText.value = res.data.summary || generateMockSummary(humanRate.value, features.value)

    // Trigger AI review
    isReviewLoading.value = true
    try {
      const reviewRes = await getReview(paperId.value)
      reviewText.value = reviewRes.data.reviewText || ''
    } catch {
      reviewText.value = ''
    }
    isReviewLoading.value = false
  } catch {
    // Mock data for demo
    humanRate.value = Math.floor(Math.random() * 60) + 5
    features.value = generateMockFeatures(humanRate.value)
    detectResult.value = { humanRate: humanRate.value, features: features.value }
    summaryText.value = generateMockSummary(humanRate.value, features.value)
  }
  isDetecting.value = false
}

function generateMockFeatures(rate) {
  const baseFeatures = [
    { featureName: '逻辑连贯性', triggerCount: 0, score: 0 },
    { featureName: '用词多样性', triggerCount: 0, score: 0 },
    { featureName: '句式复杂度', triggerCount: 0, score: 0 },
    { featureName: '段落结构', triggerCount: 0, score: 0 },
    { featureName: '引文自然度', triggerCount: 0, score: 0 },
    { featureName: '口语化程度', triggerCount: 0, score: 0 },
    { featureName: '情感色彩', triggerCount: 0, score: 0 },
    { featureName: '错别字率', triggerCount: 0, score: 0 },
    { featureName: '套话密度', triggerCount: 0, score: 0 }
  ]
  return baseFeatures.map(f => {
    f.triggerCount = Math.floor(Math.random() * 15 * (rate / 50)) + 2
    f.score = Math.min(100, Math.floor(Math.random() * rate * 1.2 + 10))
    return f
  })
}

async function loadHistory() {
  if (!appStore.anonymousId) return
  try {
    const res = await getUserPapers(appStore.anonymousId)
    uploadHistory.value = res.data.papers || res.data || []
  } catch {
    uploadHistory.value = []
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<template>
  <div class="detect-page">
    <div class="container">
      <h1 class="page-title">含人率检测</h1>
      <p class="page-desc">
        上传您的论文文件，系统将使用 DeepSeek 大模型分析文本中的"人类痕迹"，<br/>
        从 9 个维度综合评估论文的含人率。
      </p>

      <!-- Upload Section -->
      <div class="card section-card">
        <h3 class="card-title">上传论文</h3>
        <FileUploader @file-uploaded="onFileUploaded" />
      </div>

      <!-- Detect Button -->
      <div class="card section-card" v-if="paperId && !detectResult">
        <div class="detect-action">
          <p class="detect-ready-text">文件已就绪，点击下方按钮开始检测</p>
          <button
            class="btn-detect"
            :disabled="isDetecting"
            @click="startDetect"
          >
            <template v-if="isDetecting">
              <span class="spinner"></span> 检测中...
            </template>
            <template v-else>
              开始检测
            </template>
          </button>
        </div>
      </div>

      <!-- Results -->
      <div class="results-section" v-if="detectResult">
        <!-- Rate Gauge -->
        <div class="card section-card rate-section">
          <h3 class="card-title">含人率检测结果</h3>
          <div class="rate-display">
            <RateGauge :rate="humanRate" />
          </div>
          <p class="summary-text">{{ summaryText }}</p>
        </div>

        <!-- Feature Breakdown -->
        <div class="card section-card">
          <h3 class="card-title">特征分析</h3>
          <FeatureChart :features="features" />
        </div>

        <!-- AI Review -->
        <div class="card section-card review-inline-card" v-if="reviewText || isReviewLoading">
          <div class="review-inline-header">
            <h3 class="card-title" style="border-bottom: none; margin-bottom: 0;">
              🤬 HumanGC屎山鉴定师 · 锐评
            </h3>
            <span class="review-badge">DeepSeek AI</span>
          </div>
          <div class="review-inline-body" v-if="isReviewLoading">
            <p class="review-loading-text">AI正在暴躁点评中...</p>
          </div>
          <div class="review-inline-body" v-else>
            <p v-for="(p, idx) in reviewText.split('\n').filter(Boolean)" :key="idx" class="review-p">{{ p }}</p>
          </div>
        </div>

        <!-- Actions -->
        <div class="card section-card result-actions">
          <p class="action-hint">
            <template v-if="humanRate > 60">
              &#9888;&#65039; 您的论文含人率较高，建议使用"一键变史"功能降低含人率
            </template>
            <template v-else-if="humanRate > 30">
              &#128161; 含人率中等，可进一步优化
            </template>
            <template v-else>
              &#10004;&#65039; 含人率较低，论文质量优秀
            </template>
          </p>
          <router-link
            v-if="humanRate > 60"
            to="/shitsify"
            class="btn-orange"
          >
            一键变史，降低含人率 &rarr;
          </router-link>
        </div>
      </div>

      <!-- Upload History -->
      <div class="card section-card" v-if="uploadHistory.length">
        <h3 class="card-title">上传历史</h3>
        <div class="history-list">
          <div
            v-for="(item, idx) in uploadHistory"
            :key="idx"
            class="history-item"
          >
            <span class="history-name">{{ item.filename || item.title || '未知文件' }}</span>
            <span class="history-date">{{ item.uploadDate || item.createdAt || '-' }}</span>
            <span class="history-rate" v-if="item.humanRate !== undefined">
              含人率：{{ item.humanRate }}%
            </span>
            <router-link
              v-if="item.id"
              :to="'/paper/' + item.id"
              class="history-link"
            >
              查看详情 &rarr;
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detect-page {
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

/* Detect Action */
.detect-action {
  text-align: center;
  padding: 16px 0;
}

.detect-ready-text {
  font-size: 14px;
  color: var(--text-dark);
  margin: 0 0 16px;
}

.btn-detect {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  padding: 12px 48px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-detect:hover:not(:disabled) {
  background: #e55a2b;
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.4);
}

.btn-detect:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Rate Section */
.rate-section {
  text-align: center;
}

.rate-display {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.summary-text {
  font-size: 16px;
  font-weight: 600;
  margin: 16px 0 0;
}

/* Result Actions */
.result-actions {
  text-align: center;
}

.action-hint {
  font-size: 14px;
  margin: 0 0 16px;
  color: var(--text-dark);
}

.btn-orange {
  display: inline-block;
  background: var(--accent-orange);
  color: #fff;
  padding: 10px 32px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-orange:hover {
  background: #e55a2b;
}

/* History */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: #f8f9fc;
  border-radius: 6px;
  font-size: 13px;
  flex-wrap: wrap;
}

.history-name {
  font-weight: 600;
  color: var(--dark-blue);
  flex: 1;
  min-width: 150px;
}

.history-date {
  color: var(--text-light);
}

.history-rate {
  color: var(--primary-blue);
  font-weight: 700;
}

.history-link {
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: 600;
}

.history-link:hover {
  text-decoration: underline;
}

/* AI Review inline */
.review-inline-card {
  border-left: 4px solid #c62828;
  background: #fffbfb;
}

.review-inline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.review-badge {
  background: linear-gradient(135deg, #c62828, #e53935);
  color: #fff;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.review-inline-body {
  padding: 0;
}

.review-loading-text {
  color: #999;
  font-style: italic;
  font-size: 14px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.review-p {
  font-size: 14px;
  line-height: 2;
  color: #333;
  margin: 0 0 8px;
  text-indent: 2em;
}

.review-p:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .page-title { font-size: 22px; }
}
</style>
