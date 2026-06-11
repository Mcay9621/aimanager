import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { initTheme } from './composables/useTheme'
import { initResponsive } from './composables/useResponsive'
import i18n from './i18n'
import { useBrandingStore } from './stores/branding'
import App from './App.vue'
import router from './router'

initTheme()
initResponsive()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(i18n)

// Init branding after pinia is registered
const brandStore = useBrandingStore()
brandStore.init()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
