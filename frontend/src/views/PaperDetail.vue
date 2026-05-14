<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaper } from '../api'
import RateGauge from '../components/RateGauge.vue'
import FeatureChart from '../components/FeatureChart.vue'

const route = useRoute()
const router = useRouter()

const paperId = route.params.id
const paper = ref(null)
const isLoading = ref(true)
const activeTab = ref('original')

const tabs = [
  { key: 'original', label: '原文' },
  { key: 'shitsified', label: '屎山版' }
]

onMounted(async () => {
  try {
    const res = await getPaper(paperId)
    // Backend returns {paper, features, user} - flatten to match component expectations
    const data = res.data
    if (data.paper) {
      paper.value = { ...data.paper, features: data.features, user: data.user }
    } else {
      paper.value = data
    }
  } catch {
    // Mock data
    paper.value = {
      id: paperId,
      filename: '基于深度学习的自然语言处理模型优化研究_final.pdf',
      title: '基于深度学习的自然语言处理模型优化研究',
      uploadDate: '2024-12-15 14:30',
      style: '本科生DDL版',
      humanRate: 72.5,
      features: [
        { featureName: '逻辑连贯性', triggerCount: 24, score: 85 },
        { featureName: '用词多样性', triggerCount: 18, score: 72 },
        { featureName: '句式复杂度', triggerCount: 15, score: 65 },
        { featureName: '段落结构', triggerCount: 20, score: 78 },
        { featureName: '引文自然度', triggerCount: 12, score: 55 },
        { featureName: '口语化程度', triggerCount: 8, score: 40 },
        { featureName: '情感色彩', triggerCount: 5, score: 30 },
        { featureName: '错别字率', triggerCount: 3, score: 20 },
        { featureName: '套话密度', triggerCount: 22, score: 80 }
      ],
      originalText: '摘要：本文提出了一种基于深度学习的自然语言处理模型，该模型在多个基准数据集上取得了优异的性能表现。实验结果表明，我们的方法在准确率和召回率方面均优于现有方法。本研究为自然语言处理领域提供了新的研究思路。\n\n第一章 绪论\n\n自然语言处理（NLP）是人工智能领域的重要研究方向。近年来，随着深度学习技术的发展，NLP任务取得了显著进展。然而，在许多实际应用场景中，现有方法仍然存在一些局限性。\n\n本文的主要贡献包括以下几个方面：首先，我们提出了一个新颖的混合注意力机制；其次，我们在多个数据集上验证了该方法的有效性；最后，我们对模型进行了全面的消融实验分析。',
      shitsifiedText: '摘要：咱们搞了个深度学习的东西，就是拿来处理自然语言的。反正数据集上跑了一下，效果好得很，比别的方法都牛逼多了。然后就觉得，嗯，这波操作给NLP领域算是提供了一个新方向吧。\n\n第一章 开头\n\n自然语言处理（就是NLP）是人工智能里挺火的一个方向。最近几年，深度学习出来了，NLP就变厉害了。但是吧，在实际用的时候，还是有些问题没解决好的。\n\n我们做的贡献主要是有几个：首先，搞了个新的混合注意力机制（听起来很厉害对吧）；其次，在好几个数据集上都试了，效果都还行；最后，做了消融实验（就是删这删那的）。',
      review: {
        score: 78,
        summary: '这篇论文含人率偏高，多处暴露出人类写作的典型特征。建议使用一键变史功能进行深度改造。'
      }
    }
  }
  isLoading.value = false
})
</script>

<template>
  <div class="paper-detail-page">
    <div class="container">
      <router-link to="/leaderboard" class="back-link">&larr; 返回排行榜</router-link>

      <div v-if="isLoading" class="loading-state">
        <p>正在加载论文详情...</p>
      </div>

      <template v-else-if="paper">
        <h1 class="page-title">论文详情</h1>

        <!-- Paper Info -->
        <div class="card section-card">
          <h3 class="card-title">基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">论文标题</span>
              <span class="info-value">{{ paper.title }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">文件名</span>
              <span class="info-value">{{ paper.filename }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">上传时间</span>
              <span class="info-value">{{ paper.uploadDate }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">使用风格</span>
              <span class="info-value">{{ paper.style || '未设置' }}</span>
            </div>
          </div>
        </div>

        <!-- Human Rate -->
        <div class="card section-card rate-card">
          <h3 class="card-title">含人率</h3>
          <div class="rate-display">
            <RateGauge :rate="paper.humanRate" />
          </div>
        </div>

        <!-- Feature Breakdown -->
        <div class="card section-card" v-if="paper.features && paper.features.length">
          <h3 class="card-title">特征分析</h3>
          <FeatureChart :features="paper.features" />
        </div>

        <!-- AI Review -->
        <div class="card section-card review-preview" v-if="paper.reviewText">
          <div class="review-header">
            <h3 class="card-title" style="border-bottom: none; margin-bottom: 0;">AI 阅卷点评</h3>
          </div>
          <p class="review-text">{{ paper.reviewText.length > 200 ? paper.reviewText.substring(0, 200) + '...' : paper.reviewText }}</p>
          <router-link :to="'/review/' + paperId" class="view-full-review">
            查看完整评审报告 &rarr;
          </router-link>
        </div>

        <!-- Text Tabs -->
        <div class="card section-card" v-if="paper.originalText">
          <div class="text-tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="text-tab-btn"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>
          <div class="text-content">
            <pre v-if="activeTab === 'original'" class="paper-text">{{ paper.originalText }}</pre>
            <pre v-if="activeTab === 'shitsified'" class="paper-text shitsified">{{ paper.shitsifiedText }}</pre>
          </div>
        </div>

        <!-- Links -->
        <div class="paper-links">
          <router-link to="/leaderboard" class="btn-primary">查看排行榜</router-link>
          <router-link to="/shitsify" class="btn-orange">一键变史</router-link>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.paper-detail-page {
  padding-top: 80px;
  min-height: 100vh;
  background: #f5f7fb;
}

.back-link {
  color: var(--primary-blue);
  text-decoration: none;
  font-size: 14px;
}

.back-link:hover {
  text-decoration: underline;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--dark-blue);
  margin: 16px 0 24px;
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

.loading-state {
  text-align: center;
  padding: 60px;
  color: var(--text-light);
  font-size: 16px;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-light);
}

.info-value {
  font-size: 14px;
  color: var(--text-dark);
  font-weight: 500;
  word-break: break-all;
}

/* Rate */
.rate-card {
  text-align: center;
}

.rate-display {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

/* Review Preview */
.review-preview {
  border-left: 4px solid var(--primary-blue);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-score {
  font-weight: 700;
  color: var(--primary-blue);
  font-size: 14px;
}

.review-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-dark);
  margin: 0 0 12px;
}

.view-full-review {
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}

.view-full-review:hover {
  text-decoration: underline;
}

/* Text Tabs */
.text-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--border-color);
  margin-bottom: 16px;
}

.text-tab-btn {
  background: none;
  border: none;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-light);
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.text-tab-btn:hover {
  color: var(--primary-blue);
}

.text-tab-btn.active {
  color: var(--primary-blue);
  border-bottom-color: var(--primary-blue);
}

.text-content {
  max-height: 500px;
  overflow-y: auto;
}

.paper-text {
  font-size: 14px;
  line-height: 2;
  color: var(--text-dark);
  margin: 0;
  white-space: pre-wrap;
  font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
}

.paper-text.shitsified {
  background: #fffef5;
  padding: 16px;
  border-radius: 6px;
  border: 1px solid #f0d060;
}

/* Links */
.paper-links {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 24px 0 48px;
}

.btn-primary {
  display: inline-block;
  background: var(--primary-blue);
  color: #fff;
  padding: 12px 32px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-primary:hover {
  background: var(--dark-blue);
}

.btn-orange {
  display: inline-block;
  background: var(--accent-orange);
  color: #fff;
  padding: 12px 32px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-orange:hover {
  background: #e55a2b;
}

@media (max-width: 768px) {
  .info-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 22px; }
}
</style>
