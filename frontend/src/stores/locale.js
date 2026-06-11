import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import i18n from '../i18n'

export const useLocaleStore = defineStore('locale', () => {
  const currentLocale = ref(i18n.global.locale.value)

  const isZhCN = computed(() => currentLocale.value === 'zh-CN')

  function setLocale(locale) {
    currentLocale.value = locale
    i18n.global.locale.value = locale
    localStorage.setItem('locale', locale)
    document.documentElement.lang = locale === 'zh-CN' ? 'zh' : 'en'
  }

  function toggleLocale() {
    const next = currentLocale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
    setLocale(next)
  }

  return {
    currentLocale,
    isZhCN,
    setLocale,
    toggleLocale,
  }
})
