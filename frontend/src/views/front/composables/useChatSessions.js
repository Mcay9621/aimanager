import { ref, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useChatSessions() {
  const sessions = ref([])
  const sessionsLoading = ref(false)
  const currentSessionId = ref(null)
  const currentMessages = ref([])
  const currentModelName = ref('')
  const currentTitle = ref('')
  const editingSessionId = ref(null)
  const renameTitle = ref('')
  const renameInput = ref(null)

  const fetchSessions = async () => {
    sessionsLoading.value = true
    try {
      sessions.value = await request.get('/chat/sessions') || []
    } catch (e) {
      ElMessage.error('Failed to load sessions')
    } finally {
      sessionsLoading.value = false
    }
  }

  const fetchMessages = async (sessionId) => {
    try {
      currentMessages.value = await request.get('/chat/sessions/' + sessionId + '/messages')
    } catch (e) {
      ElMessage.error('Failed to load messages')
    }
  }

  const switchSession = async (session, cancelStreaming) => {
    cancelStreaming?.()
    currentSessionId.value = session.id
    currentTitle.value = session.title || 'New chat'
    currentModelName.value = session.modelName || ''
    currentMessages.value = []
    await fetchMessages(session.id)
  }

  const deleteSession = async (session) => {
    try {
      await ElMessageBox.confirm('Delete this conversation?', 'Confirm', { type: 'warning' })
      await request.delete('/chat/sessions/' + session.id)
      ElMessage.success('Deleted')
      if (currentSessionId.value === session.id) {
        currentSessionId.value = null
        currentMessages.value = []
        currentTitle.value = ''
        currentModelName.value = ''
      }
      await fetchSessions()
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('Delete failed')
    }
  }

  const startRename = (session) => {
    editingSessionId.value = session.id
    renameTitle.value = session.title || ''
  }

  const confirmRename = async (session) => {
    if (renameTitle.value.trim()) {
      try {
        await request.put('/chat/sessions/' + session.id, { title: renameTitle.value.trim() })
        session.title = renameTitle.value.trim()
        currentTitle.value = session.title
        ElMessage.success('Renamed')
      } catch (e) {
        ElMessage.error('Rename failed')
      }
    }
    editingSessionId.value = null
  }

  return {
    sessions, sessionsLoading,
    currentSessionId, currentMessages, currentModelName, currentTitle,
    editingSessionId, renameTitle, renameInput,
    fetchSessions, fetchMessages, switchSession, deleteSession,
    startRename, confirmRename,
  }
}
