import { ref, computed, watch } from 'vue'

const THEME_KEY = 'app-theme'

const theme = ref('light')

function initTheme() {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark' || saved === 'light') {
    theme.value = saved
  } else {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    theme.value = prefersDark ? 'dark' : 'light'
  }
  document.documentElement.dataset.theme = theme.value
}

function setTheme(mode) {
  theme.value = mode
  document.documentElement.dataset.theme = mode
  localStorage.setItem(THEME_KEY, mode)
}

function toggleTheme() {
  setTheme(theme.value === 'dark' ? 'light' : 'dark')
}

const isDark = computed(() => theme.value === 'dark')

export function useTheme() {
  return {
    theme,
    isDark,
    setTheme,
    toggleTheme
  }
}

export { initTheme }
