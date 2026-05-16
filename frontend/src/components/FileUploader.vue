<script setup>
import { ref, computed } from 'vue'
import { upload } from '../api'
import { useAppStore } from '../stores/app'

const props = defineProps({
  showUserMeta: { type: Boolean, default: false }
})
const emit = defineEmits(['file-uploaded'])
const appStore = useAppStore()

const uploadMode = ref('file') // 'file' | 'text'

const isDragging = ref(false)
const selectedFile = ref(null)
const uploadProgress = ref(0)
const isUploading = ref(false)
const errorMessage = ref('')
const region = ref('')
const school = ref('')

// Text paste mode
const pastedText = ref('')
const textFilename = ref('')

const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain']
const allowedExtensions = ['.pdf', '.docx', '.txt']

const fileSizeFormatted = computed(() => {
  if (!selectedFile.value) return ''
  const size = selectedFile.value.size
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
})

const textCharCount = computed(() => pastedText.value.length)

function validateFile(file) {
  if (!file) return false
  const ext = '.' + file.name.split('.').pop().toLowerCase()
  if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(ext)) {
    errorMessage.value = '仅支持 PDF、DOCX、TXT 格式文件'
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    errorMessage.value = '文件大小不能超过 50MB'
    return false
  }
  return true
}

function onDragOver(e) {
  e.preventDefault()
  isDragging.value = true
}

function onDragLeave() {
  isDragging.value = false
}

function onDrop(e) {
  e.preventDefault()
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (validateFile(file)) {
    selectedFile.value = file
    errorMessage.value = ''
  }
}

function onFileChange(e) {
  const file = e.target.files[0]
  if (validateFile(file)) {
    selectedFile.value = file
    errorMessage.value = ''
  }
}

function prepareTextFile() {
  if (!pastedText.value.trim()) {
    errorMessage.value = '请粘贴论文内容'
    return
  }
  if (pastedText.value.length > 500000) {
    errorMessage.value = '文字内容过长，请控制在50万字以内'
    return
  }
  const filename = textFilename.value.trim() || 'pasted-text.txt'
  const blob = new Blob([pastedText.value], { type: 'text/plain;charset=UTF-8' })
  const file = new File([blob], filename.endsWith('.txt') ? filename : filename + '.txt', { type: 'text/plain' })
  selectedFile.value = file
  errorMessage.value = ''
}

async function startUpload() {
  if (!selectedFile.value) return
  isUploading.value = true
  uploadProgress.value = 0
  errorMessage.value = ''

  let progressInterval
  try {
    progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += Math.random() * 15
      }
    }, 200)

    const res = await upload(selectedFile.value, region.value, school.value, appStore.anonymousId)
    clearInterval(progressInterval)
    uploadProgress.value = 100
    const paperId = res.data?.paperId || res.data?.id
    if (!paperId) {
      errorMessage.value = '上传失败：服务器返回异常'
      isUploading.value = false
      return
    }
    emit('file-uploaded', paperId)
  } catch (err) {
    if (progressInterval) clearInterval(progressInterval)
    errorMessage.value = '上传失败：' + (err.response?.data?.message || err.message || 'Network Error')
    uploadProgress.value = 0
  } finally {
    isUploading.value = false
  }
}

function clearFile() {
  selectedFile.value = null
  uploadProgress.value = 0
  errorMessage.value = ''
  pastedText.value = ''
  textFilename.value = ''
}

function switchMode(mode) {
  uploadMode.value = mode
  clearFile()
}
</script>

<template>
  <div class="file-uploader">
    <!-- Mode Tabs -->
    <div class="mode-tabs">
      <button
        class="mode-tab"
        :class="{ active: uploadMode === 'file' }"
        @click="switchMode('file')"
      >
        📄 上传文件
      </button>
      <button
        class="mode-tab"
        :class="{ active: uploadMode === 'text' }"
        @click="switchMode('text')"
      >
        📋 粘贴文字
      </button>
    </div>

    <!-- File Upload Mode -->
    <template v-if="uploadMode === 'file'">
      <div
        class="drop-zone"
        :class="{ dragging: isDragging, 'has-file': selectedFile }"
        @dragover="onDragOver"
        @dragleave="onDragLeave"
        @drop="onDrop"
        @click="!selectedFile && $refs.fileInput.click()"
      >
        <input
          ref="fileInput"
          type="file"
          accept=".pdf,.docx,.txt"
          class="file-input-hidden"
          @change="onFileChange"
        />

        <template v-if="!selectedFile">
          <div class="upload-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect x="6" y="24" width="36" height="18" rx="2" stroke="#1a56b8" stroke-width="2" fill="rgba(26,86,184,0.05)"/>
              <path d="M16 24l8-12 8 12" stroke="#1a56b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="24" y1="12" x2="24" y2="34" stroke="#1a56b8" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <p class="upload-text">将论文文件拖拽到此处，或<strong>点击选择文件</strong></p>
          <p class="upload-hint">支持 PDF、DOCX、TXT 格式，最大 50MB</p>
        </template>

        <template v-else>
          <div class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">{{ fileSizeFormatted }}</span>
          </div>

          <div class="progress-bar" v-if="isUploading">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p class="upload-progress-text" v-if="isUploading">
            上传中... {{ Math.round(uploadProgress) }}%
          </p>

          <div class="user-meta" v-if="showUserMeta && !isUploading && uploadProgress !== 100">
            <div class="meta-field">
              <label class="meta-label">
                地区
                <span class="meta-tip" title="地区为用户级别，修改后将同步更新您所有已上传论文的地区">
                  &#9432;
                  <span class="meta-tooltip">地区为用户级别，修改后将同步更新您所有已上传论文的地区</span>
                </span>
              </label>
              <input
                v-model="region"
                type="text"
                class="meta-input"
                placeholder="未知地区"
                maxlength="64"
              />
            </div>
            <div class="meta-field">
              <label class="meta-label">
                学校
                <span class="meta-tip" title="学校为用户级别，修改后将同步更新您所有已上传论文的学校">
                  &#9432;
                  <span class="meta-tooltip">学校为用户级别，修改后将同步更新您所有已上传论文的学校</span>
                </span>
              </label>
              <input
                v-model="school"
                type="text"
                class="meta-input"
                placeholder="未知学校"
                maxlength="128"
              />
            </div>
          </div>

          <div class="file-actions" v-if="!isUploading">
            <button class="btn-upload" @click.stop="startUpload">开始上传</button>
            <button class="btn-clear" @click.stop="clearFile">重新选择</button>
          </div>

          <p class="upload-success" v-if="uploadProgress === 100 && !isUploading">
            &#10004; 上传成功
          </p>
        </template>
      </div>
    </template>

    <!-- Text Paste Mode -->
    <template v-if="uploadMode === 'text'">
      <div class="text-upload-area" :class="{ 'has-file': selectedFile }">
        <template v-if="!selectedFile">
          <div class="text-input-group">
            <label class="text-label">论文标题/文件名（选填）</label>
            <input
              v-model="textFilename"
              type="text"
              class="text-filename-input"
              placeholder="例如：毕业论文.txt"
              maxlength="128"
            />
          </div>
          <div class="text-input-group">
            <label class="text-label">粘贴论文内容</label>
            <textarea
              v-model="pastedText"
              class="text-area"
              placeholder="在此粘贴您的论文文字内容..."
              rows="12"
            ></textarea>
            <span class="text-count">已输入 {{ textCharCount }} 字</span>
          </div>
          <div class="user-meta" v-if="showUserMeta">
            <div class="meta-field">
              <label class="meta-label">
                地区
                <span class="meta-tip" title="地区为用户级别，修改后将同步更新您所有已上传论文的地区">
                  &#9432;
                  <span class="meta-tooltip">地区为用户级别，修改后将同步更新您所有已上传论文的地区</span>
                </span>
              </label>
              <input
                v-model="region"
                type="text"
                class="meta-input"
                placeholder="未知地区"
                maxlength="64"
              />
            </div>
            <div class="meta-field">
              <label class="meta-label">
                学校
                <span class="meta-tip" title="学校为用户级别，修改后将同步更新您所有已上传论文的学校">
                  &#9432;
                  <span class="meta-tooltip">学校为用户级别，修改后将同步更新您所有已上传论文的学校</span>
                </span>
              </label>
              <input
                v-model="school"
                type="text"
                class="meta-input"
                placeholder="未知学校"
                maxlength="128"
              />
            </div>
          </div>
          <button
            class="btn-upload"
            :disabled="!pastedText.trim()"
            @click="prepareTextFile"
          >
            确认内容，准备上传
          </button>
        </template>

        <template v-else>
          <div class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">{{ fileSizeFormatted }}</span>
          </div>

          <div class="progress-bar" v-if="isUploading">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p class="upload-progress-text" v-if="isUploading">
            上传中... {{ Math.round(uploadProgress) }}%
          </p>

          <div class="user-meta" v-if="showUserMeta && !isUploading && uploadProgress !== 100">
            <div class="meta-field">
              <label class="meta-label">
                地区
                <span class="meta-tip" title="地区为用户级别，修改后将同步更新您所有已上传论文的地区">
                  &#9432;
                  <span class="meta-tooltip">地区为用户级别，修改后将同步更新您所有已上传论文的地区</span>
                </span>
              </label>
              <input
                v-model="region"
                type="text"
                class="meta-input"
                placeholder="未知地区"
                maxlength="64"
              />
            </div>
            <div class="meta-field">
              <label class="meta-label">
                学校
                <span class="meta-tip" title="学校为用户级别，修改后将同步更新您所有已上传论文的学校">
                  &#9432;
                  <span class="meta-tooltip">学校为用户级别，修改后将同步更新您所有已上传论文的学校</span>
                </span>
              </label>
              <input
                v-model="school"
                type="text"
                class="meta-input"
                placeholder="未知学校"
                maxlength="128"
              />
            </div>
          </div>

          <div class="file-actions" v-if="!isUploading">
            <button class="btn-upload" @click.stop="startUpload">开始上传</button>
            <button class="btn-clear" @click.stop="clearFile">重新输入</button>
          </div>

          <p class="upload-success" v-if="uploadProgress === 100 && !isUploading">
            &#10004; 上传成功
          </p>
        </template>
      </div>
    </template>

    <p class="error-msg" v-if="errorMessage">{{ errorMessage }}</p>
  </div>
</template>

<style scoped>
.file-uploader {
  width: 100%;
}

/* Mode Tabs */
.mode-tabs {
  display: flex;
  gap: 0;
  margin-bottom: -1px;
  position: relative;
  z-index: 1;
}

.mode-tab {
  flex: 1;
  padding: 10px 0;
  border: 1px solid #ddd;
  border-bottom: none;
  background: #f5f5f5;
  color: #888;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px 8px 0 0;
}

.mode-tab:first-child {
  margin-right: -1px;
}

.mode-tab.active {
  background: #fff;
  color: var(--primary-blue);
  border-color: var(--primary-blue);
  border-bottom: 2px solid #fff;
}

.mode-tab:hover:not(.active) {
  color: #555;
  background: #eee;
}

.drop-zone {
  border: 2px dashed var(--primary-blue);
  border-radius: 0 0 8px 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(26, 86, 184, 0.03);
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.drop-zone:hover {
  background: rgba(26, 86, 184, 0.06);
  border-color: var(--dark-blue);
}

.drop-zone.dragging {
  background: rgba(26, 86, 184, 0.1);
  border-color: var(--dark-blue);
  border-style: solid;
}

.drop-zone.has-file {
  border-style: solid;
  background: rgba(26, 86, 184, 0.05);
  gap: 12px;
  border-radius: 0 0 8px 8px;
}

.file-input-hidden {
  display: none;
}

.upload-icon {
  margin-bottom: 12px;
  opacity: 0.7;
}

.upload-text {
  font-size: 15px;
  color: var(--text-dark);
  margin: 0;
}

.upload-text strong {
  color: var(--primary-blue);
}

.upload-hint {
  font-size: 12px;
  color: var(--text-light);
  margin: 8px 0 0;
}

/* Text Upload Mode */
.text-upload-area {
  border: 2px solid var(--primary-blue);
  border-radius: 0 0 8px 8px;
  padding: 24px;
  background: rgba(26, 86, 184, 0.02);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.text-upload-area.has-file {
  border-style: solid;
  background: rgba(26, 86, 184, 0.05);
}

.text-input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.text-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--dark-blue);
}

.text-filename-input {
  padding: 8px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.text-filename-input:focus {
  border-color: var(--primary-blue);
}

.text-area {
  padding: 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.8;
  resize: vertical;
  outline: none;
  font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
  transition: border-color 0.2s;
}

.text-area:focus {
  border-color: var(--primary-blue);
}

.text-area::placeholder {
  color: #bbb;
}

.text-count {
  font-size: 12px;
  color: var(--text-light);
  text-align: right;
}

.user-meta {
  display: flex;
  gap: 16px;
  width: 100%;
  max-width: 400px;
  justify-content: center;
}

.meta-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.meta-label {
  font-size: 12px;
  color: var(--text-light);
  font-weight: 500;
}

.meta-input {
  padding: 6px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.meta-input:focus {
  border-color: var(--primary-blue);
}

.meta-input::placeholder {
  color: #bbb;
}

.meta-tip {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #888;
  font-size: 11px;
  cursor: help;
  margin-left: 4px;
  font-style: normal;
  vertical-align: middle;
}

.meta-tip:hover {
  background: var(--primary-blue);
  color: #fff;
}

.meta-tooltip {
  display: none;
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: #fff;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 4px;
  white-space: nowrap;
  z-index: 100;
  font-weight: 400;
}

.meta-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #333;
}

.meta-tip:hover .meta-tooltip {
  display: block;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.file-name {
  font-weight: 600;
  color: var(--dark-blue);
  font-size: 15px;
  word-break: break-all;
}

.file-size {
  color: var(--text-light);
  font-size: 13px;
}

.progress-bar {
  width: 100%;
  max-width: 300px;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary-blue);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.upload-progress-text {
  font-size: 13px;
  color: var(--primary-blue);
  margin: 0;
}

.file-actions {
  display: flex;
  gap: 10px;
}

.btn-upload {
  background: var(--primary-blue);
  color: #fff;
  border: none;
  padding: 8px 24px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}

.btn-upload:hover:not(:disabled) {
  background: var(--dark-blue);
}

.btn-upload:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-clear {
  background: #f0f0f0;
  color: #666;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn-clear:hover {
  background: #e0e0e0;
}

.upload-success {
  color: #2e7d32;
  font-weight: 600;
  font-size: 14px;
  margin: 0;
}

.error-msg {
  color: #d32f2f;
  font-size: 13px;
  margin: 8px 0 0;
  text-align: center;
}

@media (max-width: 768px) {
  .user-meta {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
