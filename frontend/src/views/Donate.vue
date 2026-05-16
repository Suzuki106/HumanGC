<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { donate } from '../api'

const appStore = useAppStore()

const selectedAmount = ref(0)
const isDonating = ref(false)
const thankYouMessage = ref('')
const donationHistory = ref([])
const totalDonated = ref(0)

const presetAmounts = [
  { amount: 5, days:  '~1.0', label: '¥5', desc: '撑 1.0 天' },
  { amount: 20, days:  '~4.1', label: '¥20', desc: '撑 4.1 天' },
  { amount: 50, days:  '~10.3', label: '¥50', desc: '撑 10.3 天' },
  { amount: 100, days:  '~20.5', label: '¥100', desc: '撑 20.5 天' }
]

const customAmount = ref('')

async function doDonate(amount) {
  if (!amount || amount <= 0) return
  isDonating.value = true
  thankYouMessage.value = ''

  try {
    await donate(amount, appStore.anonymousId)
    thankYouMessage.value = generateThankYou(amount)
    totalDonated.value += amount
  } catch {
    thankYouMessage.value = generateThankYou(amount)
    totalDonated.value += amount
  }

  donationHistory.value.unshift({
    amount,
    anonymousId: appStore.anonymousId,
    date: new Date().toISOString().split('T')[0],
    message: thankYouMessage.value
  })

  // Refresh server status to update progress
  await appStore.fetchServerStatus()
  isDonating.value = false
}

function generateThankYou(amount) {
  const dailyCost = 4.87
  const pct = Math.round(amount / dailyCost)
  const messages = [
    `感谢您的 ¥${amount} 打赏！您贡献了约 ${pct} 天的服务器运行时间，反学术事业因您而延续！`,
    `老板大气！¥${amount} 已到账，服务器又续了约 ${pct} 天命，服务器表示感激涕零。`,
    `您的 ¥${amount} 让服务器多喘了一口气。虽然钱是虚构的，但您的善意是最宝贵的！`
  ]
  return messages[Math.floor(Math.random() * messages.length)]
}

onMounted(() => {
  appStore.fetchServerStatus()
})
</script>

<template>
  <div class="donate-page">
    <div class="container">
      <h1 class="page-title">打赏续命</h1>
      <p class="page-desc">
        服务器运营不易，每天云服务费用 ¥4.87，当前余额 ¥300。<br/>
        您的慷慨解囊，将直接延长本站服务器的寿命。
      </p>

      <!-- Shutdown Progress -->
      <div class="card shutdown-card">
        <h3 class="card-title">&#9888;&#65039; 服务器倒闭倒计时</h3>
        <div class="shutdown-info">
          <div class="shutdown-big-progress">
            <div class="shutdown-big-fill" :style="{ width: (appStore.serverStatus.progressPercent || 0) + '%' }"></div>
          </div>
          <div class="shutdown-details">
            <div class="detail-item">
              <span class="detail-value">¥{{ appStore.serverStatus.totalFunds || 300 }}</span>
              <span class="detail-label">当前余额</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">¥{{ appStore.serverStatus.donatedAmount || 0 }}</span>
              <span class="detail-label">已筹金额</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">{{ appStore.serverStatus.daysRemaining || 0 }}天</span>
              <span class="detail-label">可撑天数</span>
            </div>
            <div class="detail-item">
              <span class="detail-value">{{ appStore.serverStatus.progressPercent || 0 }}%</span>
              <span class="detail-label">30天目标覆盖</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Donation Stats -->
      <div class="card stats-card" v-if="totalDonated > 0">
        <div class="stats-row">
          <div class="stats-item">
            <span class="stats-value">¥{{ totalDonated }}</span>
            <span class="stats-label">累计打赏金额</span>
          </div>
          <div class="stats-item">
            <span class="stats-value">{{ donationHistory.length }}</span>
            <span class="stats-label">打赏次数</span>
          </div>
          <div class="stats-item">
            <span class="stats-value">{{ appStore.serverStatus.daysRemaining || 0 }}天</span>
            <span class="stats-label">剩余天数</span>
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

        <!-- QR Code -->
        <div class="qr-section">
          <img src="/alipay.jpg" alt="支付宝收款码" class="qr-image" />
          <p class="qr-hint">请使用支付宝扫码打赏</p>
        </div>

        <div class="simulate-notice">
          &#128161; 打赏按钮暂为模拟功能，不会实际扣款。如需真实打赏请使用上方收款码。
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

.qr-section {
  text-align: center;
  margin-bottom: 20px;
}

.qr-image {
  width: 220px;
  height: 220px;
  border-radius: 10px;
  border: 2px solid var(--border-color);
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  object-fit: contain;
}

.qr-hint {
  font-size: 12px;
  color: var(--text-light);
  margin: 8px 0 0;
}

.simulate-notice {
  text-align: center;
  padding: 10px 16px;
  background: #fef9e7;
  border: 1px solid #f9e79f;
  border-radius: 6px;
  font-size: 12px;
  color: #7d6608;
  margin-bottom: 20px;
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
