import { ref, onMounted } from 'vue'
import { getModelTypes } from '@/utils/modelTypes'

export function useModelTypes() {
  const modelTypes = ref([])

  const getTypeLabel = (key) => {
    const t = modelTypes.value.find(t => t.key === key)
    return t ? t.label : key
  }

  const getTypeColor = (key) => {
    const t = modelTypes.value.find(t => t.key === key)
    return t ? t.color : '#888'
  }

  const getTypeBg = (key) => getTypeColor(key) + '26'

  const getConsoleUrl = (key) => {
    const t = modelTypes.value.find(t => t.key === key)
    return t ? t.consoleUrl : '#'
  }

  onMounted(async () => {
    modelTypes.value = await getModelTypes()
  })

  return { modelTypes, getTypeLabel, getTypeColor, getTypeBg, getConsoleUrl }
}
