<template>
  <div class="chat-view">
    <!-- 左侧会话列表 -->
    <div class="session-panel" :class="{ collapsed: sessionCollapsed }">
      <div class="session-header">
        <h3>对话历史</h3>
        <el-button size="small" type="primary" @click="startNewChat" :icon="Plus">新建对话</el-button>
      </div>
      <div class="session-search">
        <el-input
          v-model="sessionSearch"
          placeholder="搜索会话..."
          size="small"
          clearable
          :prefix-icon="Search"
        />
      </div>
      <div class="session-list" v-loading="sessionsLoading">
        <div
          v-for="session in filteredSessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session)"
        >
          <div class="session-title">
            <span v-if="editingSessionId !== session.id" @dblclick.stop="startRename(session)">{{ session.title || '新对话' }}</span>
            <el-input
              v-else
              v-model="renameTitle"
              size="small"
              ref="renameInput"
              @blur="confirmRename(session)"
              @keyup.enter="confirmRename(session)"
              @click.stop
            />
          </div>
          <div class="session-meta">
            <span>{{ session.modelName }}</span>
            <span>{{ session.messageCount || 0 }}条</span>
          </div>
          <el-button
            class="session-delete"
            size="small"
            text
            type="danger"
            :icon="Delete"
            @click.stop="deleteSession(session)"
          />
        </div>
        <el-empty v-if="!sessionsLoading && filteredSessions.length === 0" description="暂无对话" :image-size="60" />
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main">
      <!-- 未选择会话时的提示 -->
      <div v-if="!currentSessionId && !newChatModel && currentMessages.length === 0 && !loading" class="chat-empty">
        <el-empty description="选择模型开始对话">
          <template #extra>
            <el-select v-model="newChatModel" placeholder="选择AI模型" style="width: 260px">
              <el-option
                v-for="m in availableModels"
                :key="m.id"
                :label="m.name"
                :value="m.id"
                :disabled="!m.enabled"
              >
                <span>{{ m.name }}</span>
                <el-tag v-if="!m.enabled" size="small" type="info">禁用</el-tag>
              </el-option>
            </el-select>
            <el-button type="primary" :disabled="!newChatModel" @click="startChatWithModel" style="margin-left: 12px">
              开始对话
            </el-button>
          </template>
        </el-empty>
      </div>

      <!-- 聊天界面 -->
      <template v-else>
        <div class="chat-header">
          <div class="chat-header-info">
            <strong>{{ currentTitle || '新对话' }}</strong>
            <el-tag v-if="currentModelName" size="small">{{ currentModelName }}</el-tag>
            <el-tag v-if="sseStatus === 'disconnected'" size="small" type="danger">连接断开</el-tag>
            <el-tag v-else-if="sseStatus === 'reconnecting'" size="small" type="warning">重连中...</el-tag>
          </div>
          <el-button size="small" text @click="sessionCollapsed = !sessionCollapsed">
            <el-icon><Fold v-if="!sessionCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>
        <div class="chat-messages" ref="messagesRef" @click="onMessagesClick">
          <div v-for="(msg, idx) in currentMessages" :key="idx" :class="['msg', msg.role]">
            <div class="msg-avatar">
              <el-avatar :size="36" :icon="msg.role === 'user' ? User : Monitor" :style="msg.role === 'user' ? 'background: #409eff' : 'background: #67c23a'" />
            </div>
            <div class="msg-content-wrapper">
              <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
              <div class="msg-actions">
                <el-tooltip content="复制消息" :show-after="300">
                  <el-button size="small" text @click="copyText(msg.content)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除消息" :show-after="300">
                  <el-button size="small" text type="danger" @click="deleteMessage(idx)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
          <!-- 流式输出占位 -->
          <div v-if="loading" class="msg assistant">
            <div class="msg-avatar">
              <el-avatar :size="36" :icon="Monitor" style="background: #67c23a" />
            </div>
            <div class="msg-content streaming" v-html="renderMarkdown(streamingContent || '')"></div>
            <div v-if="!streamingContent" class="msg-content typing-indicator">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            :placeholder="loading ? '正在回复中...' : '输入消息，Ctrl+Enter 发送'"
            :disabled="loading"
            @keyup.enter.ctrl="sendMessage"
            resize="none"
          />
          <div class="input-actions">
            <span class="input-hint">Ctrl+Enter 发送</span>
            <el-button type="primary" :loading="loading" @click="sendMessage" :disabled="!inputMessage.trim() || loading">
              发送
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Delete, Fold, Expand, User, Monitor } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import DOMPurify from 'dompurify'
import request from '../../utils/request'

const route = useRoute()
const router = useRouter()

// 状态
const sessions = ref([])
const sessionsLoading = ref(false)
const sessionSearch = ref('')
const sessionCollapsed = ref(false)
const currentSessionId = ref(null)
const currentMessages = ref([])
const currentModelName = ref('')
const currentTitle = ref('')
const inputMessage = ref('')
const loading = ref(false)
const streamingContent = ref('')
const availableModels = ref([])
const newChatModel = ref(null)
const messagesRef = ref(null)
const sseStatus = ref('idle')
const editingSessionId = ref(null)
const renameTitle = ref('')
const renameInput = ref(null)

let abortController = null

// 配置 marked 使用 highlight.js
marked.use(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(code, { language: lang }).value
        } catch (e) {}
      }
      try {
        return hljs.highlightAuto(code).value
      } catch (e) {}
      return code
    }
  })
)
marked.setOptions({
  breaks: true,
  gfm: true
})

// 过滤会话
const filteredSessions = computed(() => {
  if (!sessionSearch.value) return sessions.value
  const q = sessionSearch.value.toLowerCase()
  return sessions.value.filter(s =>
    (s.title || '').toLowerCase().includes(q) ||
    (s.modelName || '').toLowerCase().includes(q)
  )
})

// 获取可用模型
const fetchModels = async () => {
  try {
    availableModels.value = await request.get('/models')
  } catch (e) {
    console.error(e)
  }
}

// 获取会话列表
const fetchSessions = async () => {
  sessionsLoading.value = true
  try {
    sessions.value = await request.get('/chat/sessions')
  } catch (e) {
    console.error(e)
  } finally {
    sessionsLoading.value = false
  }
}

// 获取会话消息
const fetchMessages = async (sessionId) => {
  try {
    currentMessages.value = await request.get(`/chat/sessions/${sessionId}/messages`)
  } catch (e) {
    ElMessage.error('获取消息失败')
  }
}

// 切换会话
const switchSession = async (session) => {
  if (loading.value) return
  cancelStreaming()
  currentSessionId.value = session.id
  currentTitle.value = session.title || '新对话'
  currentModelName.value = session.modelName || ''
  currentMessages.value = []
  await fetchMessages(session.id)
  await nextTick()
  scrollToBottom()
}

// 删除会话
const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm('确定删除此对话？', '提示', { type: 'warning' })
    await request.delete(`/chat/sessions/${session.id}`)
    ElMessage.success('已删除')
    if (currentSessionId.value === session.id) {
      currentSessionId.value = null
      currentMessages.value = []
      currentTitle.value = ''
      currentModelName.value = ''
    }
    await fetchSessions()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 新建对话
const startNewChat = () => {
  if (loading.value) return
  cancelStreaming()
  currentSessionId.value = null
  currentMessages.value = []
  currentTitle.value = ''
  currentModelName.value = ''
  streamingContent.value = ''
  newChatModel.value = null
}

// 选择模型开始对话
const startChatWithModel = async () => {
  if (!newChatModel.value) return
  const model = availableModels.value.find(m => m.id === newChatModel.value)
  if (model) {
    currentModelName.value = model.name
    try {
      const session = await request.post('/chat/sessions', {
        modelId: String(newChatModel.value),
        title: '新对话'
      })
      currentSessionId.value = session.id
      currentTitle.value = session.title
      await fetchSessions()
    } catch (e) {
      ElMessage.error('创建会话失败')
    }
  }
}

// 获取当前消息使用的模型ID
const getCurrentModelId = () => {
  if (newChatModel.value) {
    return newChatModel.value
  }
  if (currentSessionId.value) {
    const session = sessions.value.find(s => s.id === currentSessionId.value)
    return session ? session.modelId : null
  }
  return null
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  if (!currentSessionId.value && !newChatModel.value) {
    const enabled = availableModels.value.filter(m => m.enabled)
    if (enabled.length === 0) {
      ElMessage.warning('没有可用的模型')
      return
    }
    newChatModel.value = enabled[0].id
    currentModelName.value = enabled[0].name
    await startChatWithModel()
  }

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  loading.value = true
  streamingContent.value = ''

  currentMessages.value.push({ role: 'user', content: userMessage })
  await nextTick()
  scrollToBottom()

  const modelId = getCurrentModelId()
  if (!modelId) {
    ElMessage.warning('请先选择模型')
    loading.value = false
    return
  }

  await streamChat(modelId, userMessage)
}

const streamChat = async (modelId, userMessage, retryCount = 0) => {
  const MAX_RETRIES = 3

  try {
    sseStatus.value = 'connected'
    abortController = new AbortController()
    const token = localStorage.getItem('token')

    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        modelId,
        message: userMessage,
        sessionId: currentSessionId.value
      }),
      signal: abortController.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullContent = ''
    let currentEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const t = line.trim()
        if (!t) continue

        if (t.startsWith('event:')) {
          currentEvent = t.substring(6).trim()
        } else if (t.startsWith('data:')) {
          const data = t.substring(5).trim()

          if (currentEvent === 'sessionId' && data) {
            const parsed = parseInt(data)
            if (!isNaN(parsed)) {
              currentSessionId.value = parsed
              await fetchSessions()
            }
          } else if (currentEvent === 'token' && data) {
            fullContent += data
            streamingContent.value = fullContent
            await nextTick()
            scrollToBottom()
          } else if (currentEvent === 'error' && data) {
            fullContent += '\n\n[错误: ' + data + ']'
            streamingContent.value = fullContent
          }
          currentEvent = ''
        }
      }
    }

    // 流完成
    if (fullContent) {
      currentMessages.value.push({ role: 'assistant', content: fullContent })
      await fetchSessions()
    }
    streamingContent.value = ''
    sseStatus.value = 'idle'
  } catch (error) {
    if (error.name === 'AbortError') {
      sseStatus.value = 'idle'
      return
    }

    // 尝试重连
    if (retryCount < MAX_RETRIES) {
      sseStatus.value = 'reconnecting'
      ElMessage.warning(`连接断开，${MAX_RETRIES - retryCount}秒后重试...`)
      await new Promise(resolve => setTimeout(resolve, 2000))
      return streamChat(modelId, userMessage, retryCount + 1)
    }

    sseStatus.value = 'disconnected'
    if (streamingContent.value) {
      currentMessages.value.push({ role: 'assistant', content: streamingContent.value + '\n\n[连接中断]' })
    } else {
      ElMessage.error('网络错误，请重试')
    }
    streamingContent.value = ''
  } finally {
    loading.value = false
    abortController = null
    await nextTick()
    scrollToBottom()
  }
}

// 取消流式请求
const cancelStreaming = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

// 复制文本到剪贴板
const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制')
  }
}

// 删除单条消息（仅前端清除，不涉及后端）
const deleteMessage = (idx) => {
  currentMessages.value.splice(idx, 1)
}

// 开始重命名会话
const startRename = (session) => {
  editingSessionId.value = session.id
  renameTitle.value = session.title || '新对话'
  nextTick(() => {
    const input = document.querySelector('.session-item.active .el-input__inner')
    if (input) {
      input.focus()
      input.select()
    }
  })
}

// 确认重命名
const confirmRename = async (session) => {
  const title = renameTitle.value.trim() || '新对话'
  editingSessionId.value = null
  try {
    await request.put(`/chat/sessions/${session.id}`, { title })
    session.title = title
    if (currentSessionId.value === session.id) {
      currentTitle.value = title
    }
  } catch {
    ElMessage.error('重命名失败')
  }
}

// 消息区域的点击委托处理（代码块复制按钮）
const onMessagesClick = (e) => {
  const btn = e.target.closest('.copy-code-btn')
  if (btn) {
    const code = btn.parentElement.querySelector('code')
    if (code) {
      copyText(code.textContent)
    }
  }
}

// Markdown 渲染 (使用 marked)，添加代码块复制按钮
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    const html = marked.parse(text)
    const withCopyBtn = html.replace(/<pre>/g, '<pre><button class="copy-code-btn" title="复制代码" type="button"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>')
    return DOMPurify.sanitize(withCopyBtn)
  } catch (e) {
    return DOMPurify.sanitize(text)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

onMounted(async () => {
  await fetchModels()
  await fetchSessions()

  const modelId = route.query.modelId
  if (modelId) {
    const model = availableModels.value.find(m => m.id === parseInt(modelId))
    if (model && model.enabled) {
      newChatModel.value = model.id
      currentModelName.value = model.name
    }
  }
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: calc(100vh - 140px);
  gap: 16px;
}
.session-panel {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: all 0.3s;
  overflow: hidden;
}
.session-panel.collapsed {
  width: 0;
  min-width: 0;
  padding: 0;
  margin: 0;
  opacity: 0;
}
.session-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}
.session-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1a1a2e;
}
.session-search {
  padding: 12px 16px;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.session-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}
.session-item:hover .session-delete {
  opacity: 1;
}
.session-title {
  font-weight: 500;
  font-size: 14px;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 8px;
}
.session-delete {
  position: absolute;
  right: 8px;
  top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
.chat-main {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  overflow: hidden;
}
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chat-header-info strong {
  font-size: 16px;
  color: #1a1a2e;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafbff;
}
/* 代码块复制按钮 */
.chat-messages :deep(pre) {
  position: relative;
}
.chat-messages :deep(.copy-code-btn) {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  color: #aaa;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  z-index: 2;
}
.chat-messages :deep(pre:hover .copy-code-btn) {
  opacity: 1;
}
.chat-messages :deep(.copy-code-btn:hover) {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
/* 消息操作按钮 */
.msg-content-wrapper {
  position: relative;
  max-width: 75%;
}
.msg-actions {
  position: absolute;
  top: -8px;
  right: -8px;
  display: flex;
  gap: 2px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 3;
}
.msg-content-wrapper:hover .msg-actions {
  opacity: 1;
}
.msg.user .msg-actions {
  left: -8px;
  right: auto;
}
/* 打字指示器 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #e5eaf5;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}
.typing-dot {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  animation: typingBounce 1.4s ease-in-out infinite;
}
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}
/* 重命名输入框 */
.session-title .el-input {
  height: 24px;
}
.session-title .el-input__inner {
  height: 24px;
  line-height: 24px;
  padding: 0 4px;
  font-size: 13px;
}
.msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.msg.user {
  flex-direction: row-reverse;
}
.msg-avatar {
  flex-shrink: 0;
}
.msg-content {
  max-width: 75%;
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.msg-content :deep(p) {
  margin: 0 0 8px;
}
.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}
.msg-content :deep(pre) {
  background: #1a1a2e;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  position: relative;
}
.msg-content :deep(pre code) {
  display: block;
  padding: 14px;
  font-size: 13px;
  line-height: 1.5;
  color: #e8e8e8;
  background: transparent;
}
.msg-content :deep(code) {
  background: #f0f2f5;
  color: #e74c3c;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.msg.user .msg-content {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg.user .msg-content :deep(code) {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.msg.user .msg-content :deep(pre) {
  background: rgba(0,0,0,0.2);
}
.msg.user .msg-content :deep(pre code) {
  color: #e8e8e8;
}
.msg.assistant .msg-content {
  background: #fff;
  color: #333;
  border: 1px solid #e5eaf5;
  border-bottom-left-radius: 4px;
}
.msg-content.streaming {
  border-style: dashed;
}
.chat-input-area {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.input-hint {
  font-size: 12px;
  color: #c0c4cc;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-view {
    flex-direction: column;
    height: calc(100vh - 120px);
    gap: 8px;
  }
  .session-panel {
    width: 100%;
    min-width: 100%;
    max-height: 200px;
  }
  .session-panel.collapsed {
    max-height: 0;
    min-height: 0;
  }
  .msg-content {
    max-width: 85%;
  }
}
</style>
