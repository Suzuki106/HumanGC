<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReview, getPaper } from '../api'
import RateGauge from '../components/RateGauge.vue'

const route = useRoute()
const router = useRouter()

const paperId = route.params.id
const review = ref(null)
const paper = ref(null)
const isLoading = ref(true)

const ratingBadges = [
  { threshold: 30, emoji: '&#128514;', label: '太像人了！', color: '#c62828' },
  { threshold: 20, emoji: '&#128518;', label: '人类痕迹明显', color: '#e65100' },
  { threshold: 15, emoji: '&#128522;', label: '还行吧', color: '#f9a825' },
  { threshold: 10, emoji: '&#128528;', label: '挺AI的', color: '#43a047' },
  { threshold: 5, emoji: '&#128529;', label: '几乎不是人', color: '#2e7d32' },
  { threshold: 0, emoji: '&#128580;', label: '纯AI杰作！', color: '#1b5e20' }
]

function getRatingBadge(score) {
  for (const badge of ratingBadges) {
    if (score >= badge.threshold) return badge
  }
  return ratingBadges[ratingBadges.length - 1]
}

onMounted(async () => {
  try {
    const [paperRes, reviewRes] = await Promise.all([
      getPaper(paperId),
      getReview(paperId)
    ])
    // Unwrap nested backend response
    const pdata = paperRes.data
    paper.value = pdata.paper ? { ...pdata.paper, features: pdata.features } : pdata
    review.value = reviewRes.data
  } catch {
    // Mock data
    paper.value = {
      id: paperId,
      title: '基于深度学习的自然语言处理模型优化研究',
      filename: 'nlp_paper_final_v3.pdf',
      humanRate: 72.5,
      uploadDate: '2024-12-15'
    }
    review.value = {
      score: 78,
      reviewText: '这篇论文...怎么说呢？摘要写得像模像样，但第二章开始就暴露了人类的本性——居然用了"咱们"这种词汇，简直令人发指。第三章的公式明显是手动推的，太像人了。不过第四章的图表还挺像AI画的，给你记一功。总体而言，这是一篇"假装是AI写的但其实一半都是人写的"典型论文，建议使用一键变史功能进行深度改造。\n\n优点：图表制作精美，参考文献格式规范。\n缺点：人类痕迹明显，语言过于流畅，逻辑过于清晰（这是坏事）。\n修改建议：多加点错别字，把长句拆成半截句，适当引用不相关的文献。',
      reviewer: 'DeepSeek AI 评审系统'
    }
  }
  isLoading.value = false
})
</script>

<template>
  <div class="review-page">
    <div class="container">
      <router-link to="/" class="back-link">&larr; 返回首页</router-link>

      <div v-if="isLoading" class="loading-state">
        <p>正在加载评审报告...</p>
      </div>

      <template v-else-if="paper && review">
        <h1 class="page-title">AI 阅卷点评</h1>

        <!-- Paper Summary -->
        <div class="card section-card">
          <h3 class="card-title">论文信息</h3>
          <div class="paper-summary">
            <div class="summary-row">
              <span class="summary-label">论文标题：</span>
              <span class="summary-value">{{ paper.originalFilename || paper.title || '未命名论文' }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">文件名称：</span>
              <span class="summary-value">{{ paper.originalFilename }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">上传日期：</span>
              <span class="summary-value">{{ paper.createdAt || paper.uploadDate }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">含人率：</span>
              <span class="summary-value rate-highlight">{{ paper.humanRate }}%</span>
            </div>
          </div>
        </div>

        <!-- AI Review Card -->
        <div class="card section-card review-card">
          <div class="review-header">
            <h3 class="card-title" style="border-bottom: none; margin-bottom: 0;">
              HumanGC AI阅卷官 · 评审意见
            </h3>
            <div
              class="rating-badge"
              :style="{ background: getRatingBadge(paper.humanRate || 50).color }"
            >
              <span v-html="getRatingBadge(paper.humanRate || 50).emoji"></span>
              {{ getRatingBadge(paper.humanRate || 50).label }}
            </div>
          </div>
          <div class="review-body">
            <p v-for="(paragraph, idx) in review.reviewText.split('\n').filter(Boolean)" :key="idx">
              {{ paragraph }}
            </p>
          </div>
        </div>

        <div class="review-actions">
          <router-link :to="'/paper/' + paperId" class="btn-primary">
            查看论文详情 &rarr;
          </router-link>
          <router-link to="/shitsify" class="btn-orange">
            一键变史 &rarr;
          </router-link>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.review-page {
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

/* Paper Summary */
.paper-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-row {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.summary-label {
  color: var(--text-light);
  white-space: nowrap;
  min-width: 80px;
}

.summary-value {
  color: var(--text-dark);
  font-weight: 500;
}

.rate-highlight {
  color: #e65100 !important;
  font-weight: 700 !important;
  font-size: 16px;
}

/* Review Card */
.review-card {
  border-left: 4px solid var(--primary-blue);
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.rating-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.review-body p {
  font-size: 15px;
  line-height: 2;
  color: var(--text-dark);
  margin: 0 0 12px;
  text-indent: 2em;
}

.review-body p:last-child {
  margin-bottom: 0;
}

/* Actions */
.review-actions {
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
  .page-title { font-size: 22px; }
  .review-header { flex-direction: column; align-items: flex-start; }
}
</style>
