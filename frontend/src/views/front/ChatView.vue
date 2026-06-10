<template>
  <div class="chat-view">
    <!-- 左侧会话列表 -->
    <div class="session-panel" :class="{ collapsed: sessionCollapsed }">
      <div class="session-header">
        <h3>对话历史</h3>
        <el-button size="small" type="primary" @click="startNewChat" :icon="Plus" round>新建</el-button>
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
      <div class="session-list" v-loading="sessionsLoading" element-loading-background="rgba(10,10,15,0.8)">
        <div
          v-for="session in filteredSessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session)"
        >
          <div class="session-icon">
            <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M10 2a7 7 0 0 1 7 7c0 2.2-1 4.2-2.6 5.5L14 18l-3-2.5a7 7 0 0 1-1 .1A7 7 0 0 1 10 2z"/>
            </svg>
          </div>
          <div class="session-info">
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
              <span class="dot">·</span>
              <span>{{ session.messageCount || 0 }}条</span>
            </div>
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
        <div class="empty-content">
          <div class="empty-brand">
            <div class="empty-logo-ring">
              <svg viewBox="0 0 40 40" width="48" height="48" fill="none">
                <rect width="40" height="40" rx="10" fill="url(#chat-logo)"/>
                <path d="M12 28V16l8-6 8 6v12H12z" stroke="#0a0a0f" stroke-width="2" fill="none"/>
                <path d="M16 22h8v6h-8z" fill="#0a0a0f" opacity="0.8"/>
                <defs>
                  <linearGradient id="chat-logo" x1="0" y1="0" x2="40" y2="40">
                    <stop offset="0%" stop-color="#4a6fa5"/>
                    <stop offset="100%" stop-color="#6b8fc9"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>
          <h3 class="empty-title">开始新的对话</h3>
          <p class="empty-desc">选择一个 AI 模型，输入你的问题，开启智能对话之旅</p>
          <div class="empty-actions">
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
            <el-button type="primary" :disabled="!newChatModel" @click="startChatWithModel" round>
              开始对话
            </el-button>
          </div>
          <div class="suggestion-chips" v-if="availableModels.length > 0">
            <span class="suggestion-label">快速开始：</span>
            <div class="suggestion-list">
              <el-button
                v-for="q in suggestions"
                :key="q"
                size="small"
                plain
                round
                @click="sendSuggestion(q)"
              >
                {{ q }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天界面 -->
      <template v-else>
        <div class="chat-header">
          <div class="chat-header-info">
            <div class="header-status-dot" :class="sseStatus === 'connected' ? 'status-connected' : sseStatus === 'reconnecting' ? 'status-reconnecting' : 'status-idle'"></div>
            <strong>{{ currentTitle || '新对话' }}</strong>
            <el-tag v-if="currentModelName" size="small" effect="dark">{{ currentModelName }}</el-tag>
            <el-tag v-if="sseStatus === 'disconnected'" size="small" type="danger" effect="dark">连接断开</el-tag>
            <el-tag v-else-if="sseStatus === 'reconnecting'" size="small" type="warning" effect="dark">重连中...</el-tag>
          </div>
          <el-button size="small" text @click="sessionCollapsed = !sessionCollapsed">
            <el-icon><Fold v-if="!sessionCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>
        <div class="chat-messages" ref="messagesRef" @click="onMessagesClick">
          <template v-for="(msg, idx) in currentMessages" :key="idx">
            <!-- 日期分隔线 -->
            <div v-if="showDateSeparator(idx)" class="date-separator">
              <span class="date-sep-line"></span>
              <span class="date-sep-text">{{ formatDateLabel(msg.createTime) }}</span>
              <span class="date-sep-line"></span>
            </div>
            <div :class="['msg', msg.role]">
              <div class="msg-avatar">
                <el-avatar :size="36" v-if="msg.role === 'user'" style="background: linear-gradient(135deg, #4a6fa5, #6b8fc9); color: #0a0a0f; font-weight: 700;">
                  {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
                </el-avatar>
                <el-avatar :size="36" v-else style="background: linear-gradient(135deg, #4a80d4, #6a9be0);">
                  <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="#fff" stroke-width="1.5">
                    <rect x="2" y="2" width="16" height="16" rx="4"/>
                    <path d="M7 10l2 2 4-4"/>
                  </svg>
                </el-avatar>
              </div>
              <div class="msg-content-wrapper">
                <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
                <div class="msg-footer">
                  <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                </div>
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
          </template>
          <!-- 流式输出占位 -->
          <div v-if="loading" class="msg assistant">
            <div class="msg-avatar">
              <el-avatar :size="36" style="background: linear-gradient(135deg, #4a80d4, #6a9be0);">
                <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="#fff" stroke-width="1.5">
                  <rect x="2" y="2" width="16" height="16" rx="4"/>
                  <path d="M7 10l2 2 4-4"/>
                </svg>
              </el-avatar>
            </div>
            <div class="msg-content-wrapper">
              <div class="msg-content streaming" v-html="renderMarkdown(streamingContent || '')"></div>
              <div v-if="!streamingContent" class="msg-content typing-indicator">
                <div class="typing-bar">
                  <div class="typing-bar-glow"></div>
                </div>
                <span class="typing-text">思考中</span>
              </div>
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
            <el-button type="primary" :loading="loading" @click="sendMessage" :disabled="!inputMessage.trim() || loading" round>
              发送
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Delete, Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import DOMPurify from 'dompurify'
import request from '../../utils/request'
import { useChatSessions } from './composables/useChatSessions'
import { useSseChat } from './composables/useSseChat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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

// Composables
const { fetchSessions: csFetchSessions, fetchMessages: csFetchMessages, switchSession: csSwitchSession, deleteSession: csDeleteSession, startRename: csStartRename, confirmRename: csConfirmRename } = useChatSessions()
const { sendMessage: ssSendMessage, cancelStreaming: ssCancelStreaming } = useSseChat()

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
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制')
  }
}

// 删除单条消息
const deleteMessage = async (idx) => {
  const msg = currentMessages.value[idx]
  if (!msg || !msg.id) {
    currentMessages.value.splice(idx, 1)
    return
  }
  try {
    await request.delete('/chat/messages/' + msg.id)
    currentMessages.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error('删除失败')
  }
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

// Markdown 渲染 (使用 marked)
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

// ===== 视觉增强辅助函数 =====

const suggestions = [
  '解释一下量子计算的基本原理',
  '用 Python 写一个快速排序算法',
  '如何优化 SQL 查询性能？',
  '什么是微服务架构？'
]

const sendSuggestion = (text) => {
  inputMessage.value = text
  sendMessage()
}

const showDateSeparator = (idx) => {
  if (idx <= 0) return true
  const prev = currentMessages.value[idx - 1]
  const curr = currentMessages.value[idx]
  if (!prev.createTime || !curr.createTime) return false
  const pd = new Date(prev.createTime).toDateString()
  const cd = new Date(curr.createTime).toDateString()
  return pd !== cd
}

const formatDateLabel = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const sd = d.toDateString()
  const td = now.toDateString()
  const yd = new Date(now)
  yd.setDate(yd.getDate() - 1)
  if (sd === td) return '今天'
  if (sd === yd.toDateString()) return '昨天'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

onMounted(async () => {
  await fetchModels()
  await fetchSessions()

  const modelId = route.query.modelId
  if (modelId) {
    const model = availableModels.value.find(m => m.id === Number(modelId))
    if (model && model.enabled) {
      newChatModel.value = model.id
      currentModelName.value = model.name
    }
  }
})

onUnmounted(() => {
  cancelStreaming()
})
</script>

<style scoped>
/* ===== 基础布局 ===== */
.chat-view {
  display: flex;
  height: calc(100vh - 140px);
  gap: 16px;
}

/* ===== Session Panel ===== */
.session-panel {
  width: 280px;
  min-width: 280px;
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid rgba(74, 111, 165, 0.1);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
  overflow: hidden;
}
.session-panel.collapsed {
  width: 0;
  min-width: 0;
  padding: 0;
  margin: 0;
  opacity: 0;
  border: none;
}
.session-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(74, 111, 165, 0.08);
}
.session-header h3 {
  margin: 0;
  font-size: 15px;
  color: var(--app-text-primary);
  font-weight: 500;
  letter-spacing: 0.5px;
}
.session-search {
  padding: 12px 16px;
}
.session-search :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.03) !important;
  border-color: rgba(255,255,255,0.06) !important;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.session-list::-webkit-scrollbar {
  width: 4px;
}
.session-list::-webkit-scrollbar-thumb {
  background: rgba(74, 111, 165, 0.15);
  border-radius: 2px;
}
.session-item {
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  margin-bottom: 4px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border: 1px solid transparent;
}
.session-item:hover {
  background: rgba(74, 111, 165, 0.06);
  border-color: rgba(74, 111, 165, 0.1);
  transform: translateX(2px);
}
.session-item.active {
  background: rgba(74, 111, 165, 0.08);
  border-color: rgba(74, 111, 165, 0.2);
}
.session-item:hover .session-delete {
  opacity: 1;
}
.session-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(74, 111, 165, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-accent);
  margin-top: 2px;
  transition: all 0.3s ease;
}
.session-item.active .session-icon {
  background: var(--app-accent);
  color: #0a0a0f;
}
.session-info {
  flex: 1;
  min-width: 0;
}
.session-title {
  font-weight: 500;
  font-size: 13px;
  color: var(--app-text-primary);
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-title .el-input {
  height: 24px;
}
.session-title .el-input__inner {
  height: 24px;
  line-height: 24px;
  padding: 0 4px;
  font-size: 13px;
}
.session-meta {
  font-size: 11px;
  color: var(--app-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
}
.session-meta .dot {
  color: rgba(255,255,255,0.15);
}
.session-delete {
  position: absolute;
  right: 6px;
  top: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}

/* ===== Chat Main ===== */
.chat-main {
  flex: 1;
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid rgba(74, 111, 165, 0.1);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(12px);
  overflow: hidden;
  position: relative;
}

/* ===== 背景微纹理 ===== */
.chat-main::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(74, 111, 165, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(107, 143, 201, 0.02) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}
.chat-messages,
.chat-header,
.chat-input-area {
  position: relative;
  z-index: 1;
}

/* ===== Empty State ===== */
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}
.empty-content {
  text-align: center;
  max-width: 460px;
  animation: fadeInUp 0.6s ease;
}
.empty-brand {
  margin-bottom: 24px;
}
.empty-logo-ring {
  display: inline-flex;
  padding: 16px;
  border-radius: 50%;
  background: rgba(74, 111, 165, 0.06);
  animation: logoPulse 3s ease-in-out infinite;
}
@keyframes logoPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(74, 111, 165, 0.1); }
  50% { box-shadow: 0 0 0 16px rgba(74, 111, 165, 0); }
}
.empty-title {
  font-size: 22px;
  font-weight: 400;
  color: var(--app-text-primary);
  margin: 0 0 8px;
  letter-spacing: 1px;
}
.empty-desc {
  font-size: 13px;
  color: var(--app-text-muted);
  margin: 0 0 28px;
  font-weight: 300;
}
.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 32px;
}

/* Suggestion Chips */
.suggestion-chips {
  text-align: center;
}
.suggestion-label {
  font-size: 12px;
  color: var(--app-text-muted);
  display: block;
  margin-bottom: 10px;
}
.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.suggestion-list .el-button {
  transition: all 0.2s ease;
  border-color: rgba(74, 111, 165, 0.15);
  color: var(--app-text-secondary);
}
.suggestion-list .el-button:hover {
  border-color: var(--app-accent);
  color: var(--app-accent);
  background: rgba(74, 111, 165, 0.08);
  transform: translateY(-1px);
}

/* ===== Chat Header ===== */
.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid rgba(74, 111, 165, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  background: rgba(255,255,255,0.01);
}
.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.chat-header-info strong {
  font-size: 15px;
  color: var(--app-text-primary);
  font-weight: 500;
}
.header-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-connected { background: #67c23a; box-shadow: 0 0 8px rgba(103,194,58,0.4); }
.status-reconnecting { background: #e6a23c; box-shadow: 0 0 8px rgba(230,162,60,0.4); animation: pulse 1.5s ease-in-out infinite; }
.status-idle { background: rgba(255,255,255,0.2); }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ===== Messages Area ===== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(74, 111, 165, 0.12);
  border-radius: 3px;
}
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(74, 111, 165, 0.25);
}

/* ===== Date Separator ===== */
.date-separator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 28px 0 20px;
  animation: fadeIn 0.3s ease;
}
.date-sep-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(74, 111, 165, 0.08), transparent);
}
.date-sep-text {
  font-size: 11px;
  color: rgba(255,255,255,0.2);
  font-weight: 400;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

/* ===== Messages ===== */
.msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: messageSlide 0.35s ease;
  position: relative;
}
@keyframes messageSlide {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.msg.user {
  flex-direction: row-reverse;
}
.msg-avatar {
  flex-shrink: 0;
  margin-top: 4px;
  transition: transform 0.2s ease;
}
.msg-avatar:hover {
  transform: scale(1.05);
}

/* Message content wrapper */
.msg-content-wrapper {
  position: relative;
  max-width: 75%;
}
.msg-content-wrapper:hover .msg-actions {
  opacity: 1;
}
.msg.user .msg-actions {
  left: -8px;
  right: auto;
}

/* Message bubble */
.msg-content {
  padding: 14px 18px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  transition: box-shadow 0.2s ease;
}
.msg-content :deep(p) {
  margin: 0 0 8px;
}
.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}
.msg-content :deep(pre) {
  background: #0a0a0f;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  position: relative;
  border: 1px solid rgba(255,255,255,0.05);
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
  background: rgba(74, 111, 165, 0.08);
  color: var(--app-accent-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

/* User message */
.msg.user .msg-content {
  background: linear-gradient(135deg, #4a6fa5, #6b8fc9);
  color: #fff;
  border-bottom-right-radius: 4px;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(74, 111, 165, 0.2);
}
.msg.user .msg-content :deep(code) {
  background: rgba(0,0,0,0.15);
  color: #e0e8f0;
}
.msg.user .msg-content :deep(pre) {
  background: rgba(0,0,0,0.15);
  border: 1px solid rgba(0,0,0,0.1);
}
.msg.user .msg-content :deep(pre code) {
  color: #e8e8e8;
}

/* AI message */
.msg.assistant .msg-content {
  background: rgba(255,255,255,0.03);
  color: var(--app-text-secondary);
  border: 1px solid rgba(74, 111, 165, 0.08);
  border-bottom-left-radius: 4px;
}
.msg.assistant .msg-content:hover {
  border-color: rgba(74, 111, 165, 0.15);
}
.msg-content.streaming {
  border-style: dashed;
  border-color: rgba(74, 111, 165, 0.15);
}

/* Message footer (timestamp) */
.msg-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding: 0 2px;
}
.msg.user .msg-footer {
  justify-content: flex-end;
}
.msg-time {
  font-size: 11px;
  color: rgba(255,255,255,0.15);
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
}

/* ===== Message Actions ===== */
.msg-actions {
  position: absolute;
  top: -10px;
  right: -8px;
  display: flex;
  gap: 2px;
  background: rgba(18,18,26,0.95);
  border: 1px solid rgba(74, 111, 165, 0.1);
  border-radius: 8px;
  padding: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 3;
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
}

/* ===== Code block copy button ===== */
.chat-messages :deep(pre) {
  position: relative;
}
.chat-messages :deep(.copy-code-btn) {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.5);
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
  background: rgba(255,255,255,0.1);
  color: #fff;
}

/* ===== Typing / Streaming Indicator ===== */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(74, 111, 165, 0.1);
  border-radius: 12px;
  border-bottom-left-radius: 4px;
  min-height: 48px;
}
.typing-bar {
  width: 120px;
  height: 4px;
  background: rgba(74, 111, 165, 0.1);
  border-radius: 2px;
  overflow: hidden;
  position: relative;
}
.typing-bar-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, var(--app-accent), transparent);
  animation: typingSlide 1.2s ease-in-out infinite;
  border-radius: 2px;
}
@keyframes typingSlide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(300%); }
}
.typing-text {
  font-size: 12px;
  color: var(--app-text-muted);
  animation: typingFade 1.5s ease-in-out infinite;
}
@keyframes typingFade {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

/* ===== Input Area ===== */
.chat-input-area {
  padding: 16px 20px;
  border-top: 1px solid rgba(74, 111, 165, 0.08);
  flex-shrink: 0;
  position: relative;
}
.chat-input-area::after {
  content: '';
  position: absolute;
  top: -1px;
  left: 20%;
  right: 20%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(74, 111, 165, 0.12), transparent);
  transition: all 0.3s ease;
}
.chat-input-area:focus-within::after {
  left: 10%;
  right: 10%;
  background: linear-gradient(90deg, transparent, var(--app-accent), transparent);
}
.chat-input-area :deep(.el-textarea__inner) {
  background: rgba(255,255,255,0.02) !important;
  border: 1px solid rgba(255,255,255,0.06) !important;
  border-radius: 12px !important;
  color: var(--app-text-primary) !important;
  font-size: 14px;
  transition: all 0.3s;
  padding: 12px 14px !important;
}
.chat-input-area :deep(.el-textarea__inner:focus) {
  border-color: rgba(74, 111, 165, 0.25) !important;
  background: rgba(255,255,255,0.03) !important;
  box-shadow: 0 0 0 3px rgba(74, 111, 165, 0.06), 0 0 20px rgba(74, 111, 165, 0.03) !important;
}
.chat-input-area :deep(.el-textarea__inner::placeholder) {
  color: var(--app-text-muted);
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.input-hint {
  font-size: 12px;
  color: var(--app-text-muted);
  letter-spacing: 0.3px;
}

/* ===== Responsive ===== */
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
  .msg-content-wrapper {
    max-width: 85%;
  }
  .empty-actions {
    flex-direction: column;
    align-items: center;
  }
  .suggestion-list {
    flex-direction: column;
    align-items: center;
  }
}
</style>
