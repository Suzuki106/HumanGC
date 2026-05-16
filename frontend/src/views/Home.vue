<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { getLeaderboard, getStats } from '../api'
import ShutdownBar from '../components/ShutdownBar.vue'

const router = useRouter()
const appStore = useAppStore()
const topPapers = ref([])
const stats = ref({
  totalPapers: 0,
  shitsifiedPapers: 0
})

const features = [
  {
    icon: '&#128269;',
    title: '含人率检测',
    desc: '基于 DeepSeek 大模型的先进检测算法，精准识别论文中的人类写作痕迹，包含 9 大特征维度综合分析。',
    color: '#1a56b8'
  },
  {
    icon: '&#128169;',
    title: '一键变史',
    desc: '将人类论文一键转化为 AI 风格的"屎山论文"，提供 4 种经典模板，帮助您避开论文评审的含人率审查。',
    color: '#ff6b35'
  },
  {
    icon: '&#127942;',
    title: '优秀范文排行榜',
    desc: '收录全球各高校最具代表性的低含人率范文，支持按个人、地域、高校等多维度排行对比。',
    color: '#1a3a6b'
  },
  {
    icon: '&#129300;',
    title: 'AI 阅卷点评',
    desc: '采用 DeepSeek API 对论文进行智能评审，提供幽默辛辣的学术点评，让您提前了解论文的"含人风险"。',
    color: '#2e7d32'
  },
  {
    icon: '&#128202;',
    title: '含人率分析报告',
    desc: '生成详细的含人率检测报告，包括特征雷达图、触发词统计、段落含人比例分布等专业数据。',
    color: '#6a1b9a'
  },
  {
    icon: '&#9829;&#65039;',
    title: '打赏续命',
    desc: '服务器运营不易，欢迎施舍打赏。您的每一分钱都将用于延长服务器寿命，让反学术事业持续下去。',
    color: '#c62828'
  }
]

async function loadLeaderboard() {
  try {
    const res = await getLeaderboard('paper', 1, 5)
    topPapers.value = (res.data.entries || []).slice(0, 5)
  } catch {
    topPapers.value = []
  }
}

async function loadStats() {
  try {
    const res = await getStats()
    stats.value = res.data
  } catch {
    // keep defaults
  }
}

onMounted(() => {
  loadLeaderboard()
  loadStats()
})
</script>

<template>
  <div class="home-page">
    <!-- Hero Banner -->
    <section class="hero-banner">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">HumanGC — 全球领先的论文含人率检测系统</h1>
        <p class="hero-subtitle">反内卷 &middot; 反学术 &middot; 反人类写作</p>
        <p class="hero-desc">
          基于 DeepSeek 大语言模型，精准检测论文中的"人类痕迹"，<br/>
          帮助您写出让导师满意、让评审无语的纯正 AI 论文。
        </p>
        <div class="hero-actions">
          <router-link to="/detect" class="btn-hero-orange">立即检测含人率</router-link>
          <router-link to="/shitsify" class="btn-hero-outline">一键变史</router-link>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ stats.totalPapers.toLocaleString() }}</span>
            <span class="stat-label">已检测论文</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">985‰</span>
            <span class="stat-label">检测准确率</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ stats.shitsifiedPapers.toLocaleString() }}</span>
            <span class="stat-label">屎山论文数</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Shutdown Warning -->
    <ShutdownBar />

    <!-- Feature Grid -->
    <section class="section features-section">
      <div class="container">
        <h2 class="section-title">核心功能</h2>
        <p class="section-subtitle">一站式论文含人率解决方案，覆盖检测、降人、排行、点评全流程</p>
        <div class="feature-grid">
          <div
            v-for="(feat, idx) in features"
            :key="idx"
            class="feature-card"
          >
            <div class="feature-icon" :style="{ background: feat.color + '15', color: feat.color }">
              <span v-html="feat.icon"></span>
            </div>
            <h3 class="feature-title">{{ feat.title }}</h3>
            <p class="feature-desc">{{ feat.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Leaderboard Preview -->
    <section class="section leaderboard-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">优秀范文排行榜</h2>
          <router-link to="/leaderboard" class="section-link">查看完整排行 &rarr;</router-link>
        </div>
        <table class="academic-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>论文名称</th>
              <th>含人率</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="paper in topPapers"
              :key="paper.rank"
              style="cursor: pointer;"
              @click="paper.paperId && router.push('/paper/' + paper.paperId)"
              :title="paper.paperId ? '查看论文详情' : ''"
            >
              <td>
                <span class="rank-badge" :class="'rank-' + paper.rank">
                  {{ paper.rank }}
                </span>
              </td>
              <td class="td-title">{{ paper.name }}</td>
              <td>
                <span class="rate-badge" :class="paper.avgHumanRate < 5 ? 'rate-low' : 'rate-mid'">
                  {{ paper.avgHumanRate }}%
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-col">
            <h4>关于 HumanGC</h4>
            <p>HumanGC 是全球首个专注于"论文含人率"检测与转化的平台，致力于推动学术写作的全面非人化进程。本平台仅供娱乐，请勿用于实际学术不端行为。</p>
          </div>
          <div class="footer-col">
            <h4>功能入口</h4>
            <ul>
              <li><router-link to="/detect">含人率检测</router-link></li>
              <li><router-link to="/shitsify">一键变史</router-link></li>
              <li><router-link to="/leaderboard">排行榜</router-link></li>
              <li><router-link to="/donate">打赏续命</router-link></li>
            </ul>
          </div>
          <div class="footer-col">
            <h4>联系我们</h4>
            <ul>
              <li>邮箱：HumanGC@proton.me</li>
              <li>GitHub：<a href="https://github.com/Suzuki106/HumanGC" target="_blank" style="color: rgba(255,255,255,0.6);">Suzuki106/HumanGC</a></li>
              <li>反馈建议：HumanGC@proton.me</li>
            </ul>
          </div>
          <div class="footer-col">
            <h4>友情链接</h4>
            <ul>
              <li><a href="https://cx.cnki.net" target="_blank">知网查重</a></li>
              <li><a href="https://www.paperyy.com" target="_blank">PaperYY</a></li>
              <li><a href="https://www.paperpass.com" target="_blank">PaperPass</a></li>
              <li><a href="https://chat.deepseek.com" target="_blank">DeepSeek</a></li>
            </ul>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2024 HumanGC. 本网站仅供娱乐，不鼓励任何形式的学术不端。&middot; 反内卷联盟荣誉出品</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* Hero Banner */
.hero-banner {
  position: relative;
  background: linear-gradient(135deg, #0d1b3e 0%, #1a3a6b 40%, #1a56b8 100%);
  padding: 80px 20px 60px;
  text-align: center;
  overflow: hidden;
  margin-top: 60px;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(255,255,255,0.03) 0%, transparent 50%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  color: #fff;
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 12px;
  line-height: 1.3;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.hero-subtitle {
  color: rgba(255,255,255,0.85);
  font-size: 18px;
  margin: 0 0 16px;
  letter-spacing: 4px;
}

.hero-desc {
  color: rgba(255,255,255,0.65);
  font-size: 14px;
  line-height: 1.8;
  margin: 0 0 32px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 40px;
}

.btn-hero-orange {
  background: var(--accent-orange);
  color: #fff;
  padding: 14px 36px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.3s;
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.4);
}

.btn-hero-orange:hover {
  background: #e55a2b;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.5);
}

.btn-hero-outline {
  background: transparent;
  color: #fff;
  border: 2px solid rgba(255,255,255,0.5);
  padding: 12px 34px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-hero-outline:hover {
  border-color: #fff;
  background: rgba(255,255,255,0.1);
}

.hero-stats {
  display: flex;
  gap: 40px;
  justify-content: center;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}

/* Sections */
.section {
  padding: 50px 0;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--dark-blue);
  text-align: center;
  margin: 0 0 8px;
}

.section-subtitle {
  text-align: center;
  color: var(--text-light);
  margin: 0 0 36px;
  font-size: 14px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header .section-title {
  text-align: left;
  margin-bottom: 0;
}

.section-link {
  color: var(--primary-blue);
  font-size: 14px;
  text-decoration: none;
  font-weight: 600;
}

.section-link:hover {
  text-decoration: underline;
}

/* Feature Grid */
.features-section {
  background: #f7f9fc;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 8px;
  padding: 28px 24px;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.feature-card:hover {
  box-shadow: var(--card-shadow);
  transform: translateY(-2px);
  border-color: var(--primary-blue);
}

.feature-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 24px;
  margin-bottom: 14px;
}

.feature-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 13px;
  color: var(--text-light);
  line-height: 1.7;
  margin: 0;
}

/* Leaderboard Section */
.leaderboard-section {
  background: #fff;
}

/* Table */
.academic-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.academic-table thead {
  background: var(--primary-blue);
  color: #fff;
}

.academic-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}

.academic-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.academic-table tbody tr:hover {
  background: rgba(26, 86, 184, 0.04);
}

.td-title {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--dark-blue);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  background: #e8edf5;
  color: #666;
}

.rank-badge.rank-1 { background: #ffd700; color: #5c3d00; }
.rank-badge.rank-2 { background: #c0c0c0; color: #3a3a3a; }
.rank-badge.rank-3 { background: #cd7f32; color: #fff; }

.rate-badge {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 13px;
}

.rate-badge.rate-low {
  background: #e8f5e9;
  color: #2e7d32;
}

.rate-badge.rate-mid {
  background: #fff3e0;
  color: #e65100;
}

/* Footer */
.site-footer {
  background: var(--dark-blue);
  color: rgba(255,255,255,0.7);
  padding: 48px 0 24px;
  margin-top: 0;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 32px;
  margin-bottom: 32px;
}

.footer-col h4 {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid rgba(255,255,255,0.15);
}

.footer-col p {
  font-size: 13px;
  line-height: 1.8;
  margin: 0;
}

.footer-col ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-col ul li {
  margin-bottom: 8px;
  font-size: 13px;
}

.footer-col a {
  color: rgba(255,255,255,0.6);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-col a:hover {
  color: #fff;
}

.footer-bottom {
  border-top: 1px solid rgba(255,255,255,0.1);
  padding-top: 20px;
  text-align: center;
  font-size: 12px;
}

@media (max-width: 768px) {
  .hero-title { font-size: 22px; }
  .hero-subtitle { font-size: 14px; letter-spacing: 2px; }
  .hero-stats { gap: 20px; }
  .feature-grid { grid-template-columns: 1fr; }
  .footer-grid { grid-template-columns: 1fr; }
}
</style>
