import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

const STORAGE_KEY = 'branding'

const DEFAULT_BRANDING = {
  appName: 'AI Manager',
  primaryColor: '#4a6fa5',
}

function loadBranding() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) return { ...DEFAULT_BRANDING, ...JSON.parse(saved) }
  } catch { /* ignore */ }
  return { ...DEFAULT_BRANDING }
}

function applyCSSVariables(config) {
  const root = document.documentElement
  root.style.setProperty('--brand-primary', config.primaryColor)
  root.style.setProperty('--brand-primary-light', config.primaryColor + '33')
  // Override accent variables
  root.style.setProperty('--accent', config.primaryColor)
  root.style.setProperty('--accent-light', config.primaryColor)
  root.style.setProperty('--el-color-primary', config.primaryColor)
}

export const useBrandingStore = defineStore('branding', () => {
  const config = reactive(loadBranding())
  const loaded = ref(false)

  function init() {
    applyCSSVariables(config)
    loaded.value = true
  }

  function save(newConfig) {
    Object.assign(config, newConfig)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
    applyCSSVariables(config)
  }

  return {
    config,
    loaded,
    init,
    save,
  }
})
