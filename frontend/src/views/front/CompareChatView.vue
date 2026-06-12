<template>
  <div class="compare-view">
    <!-- 模型选择器 -->
    <div class="model-selector">
      <div class="selector-label">{{ $t('compareChat.selectModels') }}</div>
      <div class="model-chips" v-if="availableModels.length > 0">
        <el-tag
          v-for="m in availableModels"
          :key="m.id"
          :type="selectedModelIds.includes(m.id) ? 'primary' : 'info'"
          :effect="selectedModelIds.includes(m.id) ? 'dark' : 'plain'"
          class="model-chip"
          :class="{ selected: selectedModelIds.includes(m.id) }"
          @click="toggleModel(m.id)"
        >{{ m.name }}</el-tag>
      </div>
      <el-empty v-if="availableModels.length === 0" :description="$t('chat.noModel')" :image-size="40" />
    </div>

    <!-- 顶部操作栏 -->
    <div class="compare-toolbar">
      <el-button size="small" @click="startNewCompare" :icon="Plus" round>{{ $t('compareChat.newCompare') }}</el-button>
      <el-select
        v-model="currentSessionId"
        :placeholder="$t('compareChat.history')"
        size="small"
        clearable
        style="width: 240px"
        @change="onSessionChange"
      >
        <el-option
          v-for="s in compareSessions"
          :key="s.id"
          :label="s.title"
          :value="s.id"
        />
      </el-select>
      <el-button
        v-if="currentSessionId"
        size="small"
        type="danger"
        text
        :icon="Delete"
        @click="deleteSession"
      >{{ $t('common.delete') }}</el-button>
    </div>

    <!-- 对比结果网格 -->
    <div class="compare-main">
      <!-- 空状态 -->
      <el-empty
        v-if="!loading && results.length === 0 && !currentSessionId"
        :description="$t('compareChat.selectHint')"
        :image-size="80"
      />

      <!-- 历史结果回显 -->
      <div v-if="historyMessages.length > 0 && !loading && results.length === 0" class="compare-results">
        <div class="user-bubble">{{ historyUserMessage }}</div>
        <div class="results-grid" :style="{ gridTemplateColumns: 'repeat(' + historyResults.length + ', 1fr)' }">
          <div v-for="r in historyResults" :key="r.modelId" class="result-column">
            <div class="result-header">
              <div class="result-model-name">{{ r.modelName }}</div>
              <span class="result-tokens" v-if="r.totalTokens">{{ r.promptTokens || 0 }}+{{ r.completionTokens || 0 }} tokens</span>
            </div>
            <div class="result-body" v-html="renderMarkdown(r.content)"></div>
            <div class="result-footer">
              <el-button size="small" text @click="copyText(r.content)">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前结果 -->
      <div v-if="results.length > 0" class="compare-results">
        <div class="user-bubble">{{ userMessage }}</div>
        <div class="results-grid" :style="{ gridTemplateColumns: 'repeat(' + results.length + ', 1fr)' }">
          <div
            v-for="r in results"
            :key="r.modelId"
            class="result-column"
            :class="{ 'has-error': r.error }"
          >
            <div class="result-header">
              <div class="result-model-name">{{ r.modelName }}</div>
              <span v-if="!r.error" class="result-latency">{{ $t('compareChat.latency') }}: {{ r.latencyMs }}ms</span>
            </div>
            <div class="result-body">
              <div v-if="r.error" class="result-error">
                <el-icon class="error-icon"><WarningFilled /></el-icon>
                <span>{{ $t('compareChat.error') }}: {{ r.error }}</span>
              </div>
              <div v-else class="result-content" v-html="renderMarkdown(r.content)"></div>
            </div>
            <div v-if="!r.error" class="result-footer">
              <el-button size="small" text @click="copyText(r.content)">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading" class="compare-results">
        <div class="results-grid" :style="{ gridTemplateColumns: 'repeat(' + selectedModelIds.length + ', 1fr)' }">
          <div v-for="n in selectedModelIds.length" :key="n" class="result-column skeleton-col">
            <div class="skeleton skeleton-header-bar"></div>
            <div class="skeleton skeleton-line"></div>
            <div class="skeleton skeleton-line"></div>
            <div class="skeleton skeleton-line short"></div>
            <div class="skeleton skeleton-line"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="compare-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        :placeholder="$t('compareChat.inputPlaceholder')"
        :disabled="loading"
        @keyup.enter.ctrl="sendCompare"
        resize="none"
      />
      <div class="input-actions">
        <span class="input-hint">{{ $t('chat.sendWithEnter') }}</span>
        <el-button type="primary" :loading="loading" :disabled="!canSend" @click="sendCompare" round>
          {{ $t('compareChat.send') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, WarningFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import DOMPurify from 'dompurify'
import request from '../../utils/request'

const { t } = useI18n()

// 状态
const availableModels = ref([])
const selectedModelIds = ref([])
const inputMessage = ref('')
const loading = ref(false)
const results = ref([])
const userMessage = ref('')
const compareSessions = ref([])
const currentSessionId = ref(null)
const historyResults = ref([])
const historyUserMessage = ref('')
const historyMessages = ref([])

// 配置 marked
marked.use(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try { return hljs.highlight(code, { language: lang }).value }
        catch (e) {}
      }
      try { return hljs.highlightAuto(code).value }
      catch (e) {}
      return code
    }
  })
)
marked.setOptions({ breaks: true, gfm: true })

const canSend = computed(() =>
  selectedModelIds.value.length >= 2 &&
  inputMessage.value.trim() &&
  !loading.value
)

const toggleModel = (id) => {
  const idx = selectedModelIds.value.indexOf(id)
  if (idx > -1) {
    selectedModelIds.value.splice(idx, 1)
  } else {
    if (selectedModelIds.value.length >= 4) {
      ElMessage.warning(t('compareChat.selectModels'))
      return
    }
    selectedModelIds.value.push(id)
  }
}

const fetchModels = async () => {
  try {
    const all = await request.get('/models')
    // 只展示在线可用的模型（_available 为 true）
    availableModels.value = all.filter(m => m._available === true && m.enabled)
  } catch (e) {
    console.error(e)
  }
}

const fetchCompareSessions = async () => {
  try {
    const all = await request.get('/chat/sessions')
    compareSessions.value = all.filter(s => s.sessionType === 'compare')
  } catch (e) {
    console.error(e)
  }
}

const startNewCompare = () => {
  currentSessionId.value = null
  results.value = []
  historyResults.value = []
  historyMessages.value = []
  historyUserMessage.value = ''
  userMessage.value = ''
  inputMessage.value = ''
}

const onSessionChange = async (val) => {
  if (!val) {
    startNewCompare()
    return
  }
  try {
    const msgs = await request.get(`/chat/sessions/${val}/messages`)
    historyMessages.value = msgs
    // 提取用户消息和模型回答
    const userMsg = msgs.find(m => m.role === 'user')
    historyUserMessage.value = userMsg ? userMsg.content : ''
    const assistantMsgs = msgs.filter(m => m.role === 'assistant' && m.modelName)
    historyResults.value = assistantMsgs.map(m => ({
      modelId: m.id,
      modelName: m.modelName,
      content: m.content,
      promptTokens: m.promptTokens,
      completionTokens: m.completionTokens,
      totalTokens: m.totalTokens
    }))
    results.value = []
    userMessage.value = ''
  } catch (e) {
    ElMessage.error(t('chat.fetchMsgFailed'))
  }
}

const deleteSession = async () => {
  if (!currentSessionId.value) return
  try {
    await ElMessageBox.confirm(t('compareChat.deleteConfirm'), t('chat.confirmTitle'), { type: 'warning' })
    await request.delete(`/chat/sessions/${currentSessionId.value}`)
    ElMessage.success(t('compareChat.deleteSuccess'))
    startNewCompare()
    await fetchCompareSessions()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('chat.deleteFailed'))
  }
}

const sendCompare = async () => {
  if (!canSend.value) return

  const msg = inputMessage.value.trim()
  inputMessage.value = ''
  loading.value = true
  results.value = []
  historyResults.value = []
  historyMessages.value = []
  userMessage.value = msg

  try {
    const res = await request.post('/chat/compare', {
      message: msg,
      modelIds: selectedModelIds.value,
      sessionId: currentSessionId.value || null
    })
    results.value = res.results || []
    currentSessionId.value = res.sessionId
    await fetchCompareSessions()
  } catch (e) {
    ElMessage.error(e.message || t('compareChat.error'))
  } finally {
    loading.value = false
  }
}

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('compareChat.copySuccess'))
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success(t('compareChat.copySuccess'))
  }
}

const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    const html = marked.parse(text)
    const withCopyBtn = html.replace(/<pre>/g, '<pre><button class="copy-code-btn" title="' + t('chat.copyCode') + '" type="button"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>')
    return DOMPurify.sanitize(withCopyBtn)
  } catch (e) {
    return DOMPurify.sanitize(text)
  }
}

onMounted(async () => {
  await fetchModels()
  await fetchCompareSessions()
})
</script>

<style scoped>
.compare-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  gap: 12px;
}

/* ===== 模型选择器 ===== */
.model-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  flex-shrink: 0;
}
.selector-label {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
  font-weight: 500;
}
.model-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.model-chip {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px;
  font-size: 12px;
  padding: 4px 12px;
}
.model-chip:hover {
  transform: translateY(-1px);
}
.model-chip.selected {
  font-weight: 600;
}

/* ===== 工具栏 ===== */
.compare-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* ===== 主区域 ===== */
.compare-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
.compare-main::-webkit-scrollbar {
  width: 6px;
}
.compare-main::-webkit-scrollbar-thumb {
  background: rgba(74, 111, 165, 0.15);
  border-radius: 3px;
}

/* ===== 用户消息气泡 ===== */
.user-bubble {
  background: linear-gradient(135deg, #4a6fa5, #6b8fc9);
  color: #fff;
  padding: 10px 16px;
  border-radius: 12px;
  border-bottom-right-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 16px;
  max-width: 70%;
  word-break: break-word;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(74, 111, 165, 0.2);
}

/* ===== 结果网格 ===== */
.compare-results {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.results-grid {
  display: grid;
  gap: 12px;
}

/* ===== 结果列 ===== */
.result-column {
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.2s;
}
.result-column:hover {
  border-color: rgba(74, 111, 165, 0.2);
}
.result-column.has-error {
  border-color: rgba(245, 108, 108, 0.3);
}
.result-header {
  padding: 10px 14px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}
.result-model-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}
.result-latency {
  font-size: 11px;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, monospace;
}
.result-tokens {
  font-size: 11px;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, monospace;
}
.result-body {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  max-height: 60vh;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}
.result-body::-webkit-scrollbar {
  width: 4px;
}
.result-body::-webkit-scrollbar-thumb {
  background: rgba(74, 111, 165, 0.1);
  border-radius: 2px;
}
.result-content :deep(p) {
  margin: 0 0 8px;
}
.result-content :deep(p:last-child) {
  margin-bottom: 0;
}
.result-content :deep(pre) {
  background: var(--app-chat-code-bg);
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  position: relative;
  border: 1px solid var(--app-chat-code-border);
}
.result-content :deep(pre code) {
  display: block;
  padding: 14px;
  font-size: 13px;
  line-height: 1.5;
  color: #e8e8e8;
  background: transparent;
}
.result-content :deep(code) {
  background: rgba(74, 111, 165, 0.08);
  color: var(--accent-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.result-content :deep(.copy-code-btn) {
  position: absolute;
  top: 6px;
  right: 6px;
  background: var(--app-chat-msg-bg);
  border: 1px solid var(--app-chat-code-border);
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  display: inline-flex;
  align-items: center;
}
.result-content :deep(pre:hover .copy-code-btn) {
  opacity: 1;
}
.result-content :deep(.copy-code-btn:hover) {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

/* ===== 错误状态 ===== */
.result-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 16px;
  color: #f56c6c;
  text-align: center;
  font-size: 13px;
}
.error-icon {
  font-size: 28px;
}

/* ===== 底部 ===== */
.result-footer {
  padding: 8px 14px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

/* ===== 骨架 ===== */
.skeleton-col {
  padding: 14px;
  gap: 8px;
}
.skeleton {
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}
.skeleton-header-bar {
  height: 20px;
  width: 60%;
  margin-bottom: 12px;
}
.skeleton-line {
  height: 14px;
  width: 100%;
  margin-bottom: 8px;
}
.skeleton-line.short {
  width: 65%;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 输入区 ===== */
.compare-input {
  flex-shrink: 0;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}
.compare-input :deep(.el-textarea__inner) {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 12px !important;
  color: var(--text-primary) !important;
  font-size: 14px;
  transition: all 0.3s;
  padding: 10px 14px !important;
}
.compare-input :deep(.el-textarea__inner:focus) {
  border-color: rgba(74, 111, 165, 0.25) !important;
  background: var(--bg-card-hover) !important;
  box-shadow: 0 0 0 3px rgba(74, 111, 165, 0.06) !important;
}
.compare-input :deep(.el-textarea__inner::placeholder) {
  color: var(--text-muted);
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.input-hint {
  font-size: 12px;
  color: var(--text-muted);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .compare-view {
    height: calc(100vh - 120px);
  }
  .model-selector {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .results-grid {
    grid-template-columns: 1fr !important;
  }
  .user-bubble {
    max-width: 100%;
  }
  .compare-toolbar {
    flex-wrap: wrap;
  }
  .result-body {
    max-height: 40vh;
  }
}
</style>
