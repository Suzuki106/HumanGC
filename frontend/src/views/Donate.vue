<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { donate } from '../api'

const appStore = useAppStore()

const selectedAmount = ref(0)
const isDonating = ref(false)
const thankYouMessage = ref('')
const donationHistory = ref([])
const totalDonated = ref(0)
const daysExtended = ref(0)

const presetAmounts = [
  { amount: 5, days: 1, label: '¥5', desc: '延长 1 天' },
  { amount: 20, days: 5, label: '¥20', desc: '延长 5 天' },
  { amount: 50, days: 15, label: '¥50', desc: '延长 15 天' },
  { amount: 100, days: 30, label: '¥100', desc: '延长 30 天' }
]

const customAmount = ref('')

async function doDonate(amount) {
  if (!amount || amount <= 0) return
  isDonating.value = true
  thankYouMessage.value = ''

  try {
    const res = await donate(amount, appStore.anonymousId)
    thankYouMessage.value = res.data.message || generateThankYou(amount)
    totalDonated.value += amount
    daysExtended.value += presetAmounts.find(p => p.amount === amount)?.days || Math.floor(amount / 5)
    donationHistory.value.unshift({
      amount,
      anonymousId: appStore.anonymousId,
      date: new Date().toISOString().split('T')[0],
      message: thankYouMessage.value
    })
  } catch {
    // Simulate success for demo
    thankYouMessage.value = generateThankYou(amount)
    totalDonated.value += amount
    daysExtended.value += presetAmounts.find(p => p.amount === amount)?.days || Math.floor(amount / 5)
    donationHistory.value.unshift({
      amount,
      anonymousId: appStore.anonymousId,
      date: new Date().toISOString().split('T')[0],
      message: thankYouMessage.value
    })
  }
  isDonating.value = false
}

function generateThankYou(amount) {
  const messages = [
    `感谢您的 ¥${amount} 打赏！服务器又多了 ${Math.floor(amount / 5)} 天的生命，您就是反学术事业的英雄！`,
    `老板大气！¥${amount} 已经到账（虚构的），服务器表示很感动并决定多活几天。`,
    `您的 ¥${amount} 打赏让我们热泪盈眶。虽然这钱不会真的用来续费，但您的善意是最宝贵的！`
  ]
  return messages[Math.floor(Math.random() * messages.length)]
}

onMounted(() => {
  appStore.fetchServerStatus()
  // Mock donation history
  donationHistory.value = [
    { amount: 20, anonymousId: 'a1b2c3', date: '2024-12-14', message: '服务器续命成功！' },
    { amount: 5, anonymousId: 'd4e5f6', date: '2024-12-12', message: '感谢打赏！' },
    { amount: 100, anonymousId: 'g7h8i9', date: '2024-12-10', message: '老板大气！' }
  ]
  totalDonated.value = donationHistory.value.reduce((sum, d) => sum + d.amount, 0)
})
</script>

<template>
  <div class="donate-page">
    <div class="container">
      <h1 class="page-title">打赏续命</h1>
      <p class="page-desc">
        服务器运营不易，每一分钱都至关重要。<br/>
        您的慷慨解囊，将直接延长本站服务器寿命，让反学术事业薪火相传。
      </p>

      <!-- Shutdown Progress -->
      <div class="card shutdown-card">
        <h3 class="card-title">&#9888;&#65039; 服务器倒闭倒计时</h3>
        <div class="shutdown-info">
          <div class="shutdown-big-progress">
            <div class="shutdown-big-fill" :style="{ width: (appStore.serverStatus.progressPercent || 45) + '%' }"></div>
          </div>
          <div class="shutdown-details">
            <div class="detail-item">
              <span class="detail-value">{{ appStore.serverStatus.daysRemaining || 23 }}</span>
              <span class="detail-label">剩余天数</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">{{ appStore.serverStatus.hoursRemaining || 12 }}</span>
              <span class="detail-label">剩余小时</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">{{ appStore.serverStatus.progressPercent || 45 }}%</span>
              <span class="detail-label">倒闭进度</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">¥{{ (appStore.serverStatus.totalCost || 300) - (appStore.serverStatus.donatedAmount || 0) }}</span>
              <span class="detail-label">资金缺口</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Donation Received Stats -->
      <div class="card stats-card" v-if="totalDonated > 0">
        <div class="stats-row">
          <div class="stats-item">
            <span class="stats-value">¥{{ totalDonated }}</span>
            <span class="stats-label">累计收到打赏</span>
          </div>
          <div class="stats-item">
            <span class="stats-value">{{ daysExtended }}天</span>
            <span class="stats-label">累计续命天数</span>
          </div>
          <div class="stats-item">
            <span class="stats-value">{{ donationHistory.length }}</span>
            <span class="stats-label">打赏次数</span>
          </div>
        </div>
      </div>

      <!-- Donation Area -->
      <div class="card donate-card">
        <h3 class="card-title">&#9829; 资助续命</h3>
        <p class="donate-appeal">
          服务器运营不易，每月的云服务器费用全靠站长卖肾支撑。<br/>
          如果这个网站曾经帮到过你，请考虑施舍一点，让服务器多活几天。
        </p>

        <!-- QR Code Placeholder -->
        <div class="qr-placeholder">
          <div class="qr-box">
            <div class="qr-icon">&#128179;</div>
            <p>付款码占位 - 开发阶段</p>
            <p class="qr-hint">上线后将替换为真实收款二维码</p>
          </div>
        </div>

        <!-- Preset Amounts -->
        <div class="preset-grid">
          <button
            v-for="preset in presetAmounts"
            :key="preset.amount"
            class="preset-btn"
            :class="{ selected: selectedAmount === preset.amount }"
            @click="selectedAmount = preset.amount; customAmount = ''"
          >
            <span class="preset-amount">{{ preset.label }}</span>
            <span class="preset-desc">{{ preset.desc }}</span>
          </button>
        </div>

        <div class="custom-donate">
          <input
            v-model="customAmount"
            type="number"
            class="amount-input"
            placeholder="自定义金额"
            min="1"
            @input="selectedAmount = 0"
          />
          <button
            class="btn-donate"
            :disabled="isDonating || (!selectedAmount && !customAmount)"
            @click="doDonate(selectedAmount || Number(customAmount))"
          >
            <template v-if="isDonating">
              <span class="spinner"></span> 处理中...
            </template>
            <template v-else>
              &#9829; 立即打赏
            </template>
          </button>
        </div>

        <!-- Thank You -->
        <div class="thank-you" v-if="thankYouMessage">
          <p>&#10024; {{ thankYouMessage }}</p>
        </div>
      </div>

      <!-- Donation History -->
      <div class="card" v-if="donationHistory.length">
        <h3 class="card-title">打赏记录</h3>
        <div class="history-list">
          <div
            v-for="(item, idx) in donationHistory"
            :key="idx"
            class="history-item"
          >
            <span class="history-amount">¥{{ item.amount }}</span>
            <span class="history-date">{{ item.date }}</span>
            <span class="history-msg">{{ item.message }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.donate-page {
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

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--dark-blue);
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--primary-blue);
}

/* Shutdown Card */
.shutdown-card {
  margin-bottom: 20px;
  border-left: 4px solid var(--accent-orange);
}

.shutdown-big-progress {
  width: 100%;
  height: 16px;
  background: #f0d060;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.shutdown-big-fill {
  height: 100%;
  background: linear-gradient(90deg, #d4832a, #c62828);
  border-radius: 8px;
  transition: width 1s ease;
}

.shutdown-details {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--dark-blue);
}

.detail-label {
  font-size: 12px;
  color: var(--text-light);
}

/* Stats Card */
.stats-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #1a56b8, #1a3a6b);
  color: #fff;
}

.stats-row {
  display: flex;
  gap: 32px;
  justify-content: center;
  flex-wrap: wrap;
}

.stats-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stats-value {
  font-size: 28px;
  font-weight: 800;
}

.stats-label {
  font-size: 13px;
  opacity: 0.8;
}

/* Donate Card */
.donate-card {
  margin-bottom: 20px;
}

.donate-appeal {
  font-size: 14px;
  color: var(--text-dark);
  line-height: 1.8;
  text-align: center;
  margin: 0 0 24px;
}

.qr-placeholder {
  text-align: center;
  margin-bottom: 24px;
}

.qr-box {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 200px;
  background: #f0f4fa;
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  padding: 20px;
}

.qr-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.qr-box p {
  font-size: 13px;
  color: var(--text-dark);
  margin: 0;
  font-weight: 600;
}

.qr-hint {
  font-size: 11px !important;
  color: var(--text-light) !important;
  font-weight: 400 !important;
  margin-top: 4px !important;
}

/* Preset Grid */
.preset-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.preset-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 10px;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.preset-btn:hover {
  border-color: var(--primary-blue);
  background: rgba(26, 86, 184, 0.03);
}

.preset-btn.selected {
  border-color: var(--primary-blue);
  background: rgba(26, 86, 184, 0.08);
}

.preset-amount {
  font-size: 18px;
  font-weight: 800;
  color: var(--dark-blue);
}

.preset-desc {
  font-size: 11px;
  color: var(--text-light);
}

/* Custom Donate */
.custom-donate {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.amount-input {
  flex: 1;
  min-width: 120px;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 16px;
  outline: none;
}

.amount-input:focus {
  border-color: var(--primary-blue);
}

.btn-donate {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  padding: 10px 36px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.btn-donate:hover:not(:disabled) {
  background: #e55a2b;
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.4);
}

.btn-donate:disabled {
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

/* Thank You */
.thank-you {
  text-align: center;
  padding: 16px;
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border-radius: 8px;
  margin-bottom: 0;
}

.thank-you p {
  color: #2e7d32;
  font-size: 15px;
  font-weight: 600;
  margin: 0;
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
  gap: 12px;
  padding: 10px 14px;
  background: #f8f9fc;
  border-radius: 6px;
  font-size: 13px;
  flex-wrap: wrap;
}

.history-amount {
  font-weight: 800;
  color: var(--accent-orange);
  min-width: 50px;
}

.history-date {
  color: var(--text-light);
  min-width: 90px;
}

.history-msg {
  color: var(--text-dark);
  flex: 1;
}

@media (max-width: 768px) {
  .preset-grid { grid-template-columns: repeat(2, 1fr); }
  .page-title { font-size: 22px; }
  .shutdown-details { gap: 16px; }
}
</style>
