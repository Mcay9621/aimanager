import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US']
export const DEFAULT_LOCALE = 'zh-CN'

function getBrowserLocale() {
  const lang = navigator.language || navigator.userLanguage
  if (lang.startsWith('zh')) return 'zh-CN'
  if (lang.startsWith('en')) return 'en-US'
  return DEFAULT_LOCALE
}

function getInitialLocale() {
  const saved = localStorage.getItem('locale')
  if (saved && SUPPORTED_LOCALES.includes(saved)) return saved
  return getBrowserLocale()
}

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export default i18n
