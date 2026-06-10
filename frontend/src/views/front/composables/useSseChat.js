import { ref, onUnmounted } from 'vue'

export function useSseChat() {
  const loading = ref(false)
  const streamingContent = ref('')
  const sseStatus = ref('idle')
  let abortController = null

  const sendMessage = async (modelId, message, sessionId, callbacks = {}) => {
    if (loading.value) return
    loading.value = true
    streamingContent.value = ''
    sseStatus.value = 'connecting'

    abortController = new AbortController()

    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': 'Bearer ' + token } : {}),
        },
        body: JSON.stringify({ modelId, message, sessionId }),
        signal: abortController.signal,
      })

      sseStatus.value = 'connected'
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''
      let resultSessionId = sessionId

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.slice(7).trim()
          } else if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (currentEvent === 'sessionId' && data) {
              resultSessionId = data
              callbacks.onSessionId?.(data)
            } else if (currentEvent === 'token') {
              streamingContent.value += data
              callbacks.onToken?.(data)
            } else if (currentEvent === 'done') {
              callbacks.onDone?.(streamingContent.value, resultSessionId)
            } else if (currentEvent === 'error') {
              callbacks.onError?.(data)
            }
          }
        }
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        sseStatus.value = 'disconnected'
        callbacks.onError?.(err.message)
      }
    } finally {
      loading.value = false
      sseStatus.value = 'idle'
    }
  }

  const cancelStreaming = () => {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    loading.value = false
    streamingContent.value = ''
    sseStatus.value = 'idle'
  }

  onUnmounted(() => cancelStreaming())

  return { loading, streamingContent, sseStatus, sendMessage, cancelStreaming }
}
