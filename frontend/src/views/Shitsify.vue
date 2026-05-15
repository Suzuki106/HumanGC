<script setup>
import { ref } from 'vue'
import { shitsify } from '../api'
import FileUploader from '../components/FileUploader.vue'

const paperId = ref(null)
const isGenerating = ref(false)
const result = ref(null)
const errorMsg = ref('')

async function onFileUploaded(id) {
  paperId.value = id
  result.value = null
}

async function doShitsify() {
  if (!paperId.value) return
  isGenerating.value = true
  result.value = null
  errorMsg.value = ''

  try {
    const res = await shitsify(paperId.value, '究极变史')
    result.value = res.data
  } catch (err) {
    errorMsg.value = '变史失败：' + (err.response?.data?.message || err.message || '请稍后重试')
  }
  isGenerating.value = false
}
</script>

<template>
  <div class="shitsify-page">
    <div class="container">
      <h1 class="page-title">💩 一键变史</h1>
      <p class="page-desc">
        不选模式，不挑风格。上传论文，一键毁成依托答辩。<br/>
        融合搬史缝合 + 谷歌翻译20遍 + 断章取义 + 逆天改写，往死里毁。
      </p>

      <!-- Upload -->
      <div class="card section-card">
        <h3 class="card-title">上传论文</h3>
        <FileUploader @file-uploaded="onFileUploaded" />
      </div>

      <!-- Generate Button -->
      <div class="card section-card" v-if="paperId && !result">
        <div class="generate-action">
          <p class="ready-text">文件已就绪。警告：生成结果不可逆，可能引发心理不适。</p>
          <p class="error-msg" v-if="errorMsg">{{ errorMsg }}</p>
          <button
            class="btn-generate"
            :disabled="isGenerating"
            @click="doShitsify"
          >
            <template v-if="isGenerating">
              <span class="spinner"></span> 正在毁灭论文...
            </template>
            <template v-else>
              💩 一键变史
            </template>
          </button>
        </div>
      </div>

      <!-- Results -->
      <div class="results-section" v-if="result">
        <h3 class="section-title">生成结果 · 原文 vs 大粪</h3>
        <div class="comparison">
          <div class="comp-panel">
            <div class="comp-header"><h4>原文</h4></div>
            <div class="comp-body">
              <pre class="paper-text">{{ result.originalText }}</pre>
            </div>
          </div>
          <div class="comp-panel comp-right">
            <div class="comp-header"><h4>💩 变史版</h4></div>
            <div class="comp-body">
              <pre class="paper-text shit-text">{{ result.shitsifiedText }}</pre>
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

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--dark-blue);
  margin-bottom: 16px;
}

/* Generate */
.generate-action {
  text-align: center;
  padding: 16px 0;
}

.error-msg {
  color: #d32f2f;
  font-size: 14px;
  margin: 0 0 12px;
  font-weight: 500;
}

.ready-text {
  font-size: 14px;
  color: #e65100;
  margin: 0 0 16px;
  font-weight: 500;
}

.btn-generate {
  background: linear-gradient(135deg, #c62828, #e65100);
  color: #fff;
  border: none;
  padding: 16px 56px;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 6px 24px rgba(198, 40, 40, 0.4);
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn-generate:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(198, 40, 40, 0.6);
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

@keyframes spin { to { transform: rotate(360deg); } }

/* Comparison */
.comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.comp-panel {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.comp-header {
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

.comp-body {
  padding: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.paper-text {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-dark);
  margin: 0;
  white-space: pre-wrap;
  font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
}

.shit-text {
  background: #fffef5;
  border: 1px solid #f0d060;
}

@media (max-width: 768px) {
  .comparison { grid-template-columns: 1fr; }
  .page-title { font-size: 22px; }
}
</style>
