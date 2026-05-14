<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLeaderboard } from '../api'

const router = useRouter()

const activeTab = ref('person')
const page = ref(1)
const pageSize = ref(20)
const totalRecords = ref(0)
const records = ref([])
const searchQuery = ref('')
const isLoading = ref(false)

const tabs = [
  { key: 'person', label: '个人排行' },
  { key: 'region', label: '地域排行' },
  { key: 'school', label: '高校排行' }
]

async function loadData() {
  isLoading.value = true
  try {
    const res = await getLeaderboard(activeTab.value, page.value, pageSize.value)
    records.value = res.data.entries || []
    totalRecords.value = res.data.total || records.value.length
  } catch {
    records.value = generateMockData(activeTab.value)
    totalRecords.value = 50
  }
  isLoading.value = false
}

function generateMockData(type) {
  const names = ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十', '郑十一', '王十二']
  const universities = ['清华大学', '北京大学', '浙江大学', '上海交通大学', '华中科技大学', '复旦大学', '南京大学', '中国科学技术大学']
  const regions = ['北京', '上海', '浙江', '湖北', '江苏', '广东', '陕西', '四川']

  const data = []
  for (let i = 0; i < 10; i++) {
    const rank = (page.value - 1) * pageSize.value + i + 1
    const rate = (Math.random() * 15 + 1).toFixed(1)
    const count = Math.floor(Math.random() * 50) + 3

    data.push({
      rank,
      name: type === 'person' ? (names[i] || '匿名用户') : (type === 'region' ? regions[i % regions.length] : universities[i % universities.length]),
      avgHumanRate: parseFloat(rate),
      paperCount: count,
      id: 'item_' + rank
    })
  }
  return data
}

const totalPages = () => Math.max(1, Math.ceil(totalRecords.value / pageSize.value))

function goToPage(p) {
  if (p < 1 || p > totalPages()) return
  page.value = p
}

function onSearch() {
  page.value = 1
  loadData()
}

watch(activeTab, () => {
  page.value = 1
  loadData()
})

watch(page, () => {
  loadData()
})

function onRowClick(item) {
  router.push('/paper/' + item.id)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="leaderboard-page">
    <div class="container">
      <h1 class="page-title">排行榜</h1>
      <p class="page-desc">
        查看全球范围内含人率最低的论文、作者和高校排名。<br/>
        含人率越低，排名越靠前。
      </p>

      <!-- Tabs -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Search -->
      <div class="search-bar">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="搜索名称..."
          @keyup.enter="onSearch"
        />
        <button class="btn-search" @click="onSearch">搜索</button>
      </div>

      <!-- Table -->
      <div class="card">
        <table class="academic-table">
          <thead>
            <tr>
              <th style="width: 80px;">排名</th>
              <th>名称</th>
              <th style="width: 120px;">平均含人率</th>
              <th style="width: 100px;">论文数量</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="isLoading">
              <td colspan="4" style="text-align: center; padding: 40px; color: var(--text-light);">
                加载中...
              </td>
            </tr>
            <tr v-else-if="!records.length">
              <td colspan="4" style="text-align: center; padding: 40px; color: var(--text-light);">
                暂无数据
              </td>
            </tr>
            <tr
              v-for="item in records"
              :key="item.rank"
              @click="onRowClick(item)"
              style="cursor: pointer;"
            >
              <td>
                <span class="rank-badge" :class="'rank-' + item.rank">
                  {{ item.rank }}
                </span>
              </td>
              <td class="td-name">{{ item.name }}</td>
              <td>
                <span class="rate-badge" :class="item.avgHumanRate < 5 ? 'rate-low' : 'rate-mid'">
                  {{ item.avgHumanRate }}%
                </span>
              </td>
              <td>{{ item.paperCount }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination" v-if="totalRecords > pageSize">
        <button
          class="page-btn"
          :disabled="page <= 1"
          @click="goToPage(page - 1)"
        >
          &laquo; 上一页
        </button>
        <span class="page-info">
          第 {{ page }} / {{ totalPages() }} 页（共 {{ totalRecords }} 条）
        </span>
        <button
          class="page-btn"
          :disabled="page >= totalPages()"
          @click="goToPage(page + 1)"
        >
          下一页 &raquo;
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.leaderboard-page {
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
  margin: 0 0 24px;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  background: none;
  border: none;
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-light);
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--primary-blue);
}

.tab-btn.active {
  color: var(--primary-blue);
  border-bottom-color: var(--primary-blue);
}

/* Search */
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  max-width: 300px;
  padding: 8px 14px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: var(--primary-blue);
}

.btn-search {
  background: var(--primary-blue);
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-search:hover {
  background: var(--dark-blue);
}

/* Table */
.card {
  padding: 0;
  overflow: hidden;
}

.academic-table {
  width: 100%;
}

.td-name {
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

.rate-badge.rate-low { background: #e8f5e9; color: #2e7d32; }
.rate-badge.rate-mid { background: #fff3e0; color: #e65100; }

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}

.page-btn {
  background: #fff;
  color: var(--primary-blue);
  border: 1px solid var(--border-color);
  padding: 8px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary-blue);
  background: rgba(26, 86, 184, 0.05);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-light);
}

@media (max-width: 768px) {
  .page-title { font-size: 22px; }
  .tab-btn { padding: 10px 16px; font-size: 13px; }
}
</style>
