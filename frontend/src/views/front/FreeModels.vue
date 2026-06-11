<template>
  <div class="free-models-page">
    <h2 class="page-title-sm">{{ $t('freeModels.title') }}</h2>
    <p class="page-desc">{{ $t('freeModels.desc') }}</p>

    <!-- 搜索与筛选 -->
    <div class="free-toolbar">
      <el-input
        v-model="searchQuery"
        :placeholder="$t('freeModels.search')"
        clearable
        :prefix-icon="Search"
        size="default"
        class="free-search"
      />
      <div class="category-tabs">
        <el-button
          v-for="cat in categories"
          :key="cat.id"
          :type="activeCategory === cat.id ? 'primary' : 'default'"
          size="small"
          @click="activeCategory = cat.id"
        >{{ localeStore.isZhCN ? cat.label : cat.labelEn }}</el-button>
      </div>
    </div>

    <!-- 模型卡片网格 -->
    <div v-if="filteredModels.length > 0" class="model-grid">
      <div
        v-for="m in filteredModels"
        :key="m.id"
        class="free-card"
        :style="{
          '--brand': m.color,
          '--brand-rgb': hexToRgb(m.color),
        }"
        @click="openUrl(m.url)"
      >
        <!-- 顶部彩色装饰条 -->
        <div class="card-accent" :style="{ background: m.color }"></div>
        <!-- 角落光晕 -->
        <div class="card-glow" :style="{ background: 'radial-gradient(circle at 30% 0%, ' + m.color + '22, transparent 70%)' }"></div>

        <div class="card-top-row">
          <div class="card-icon-wrap" :style="{ background: m.color + '18', color: m.color }">
            <img v-if="m.simpleIconSlug" :src="'https://cdn.simpleicons.org/' + m.simpleIconSlug + '/' + m.color.slice(1)" :alt="m.name" class="brand-icon" @error="e => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = '' }">
            <span :class="['brand-fallback', { 'brand-fallback-hidden': m.simpleIconSlug }]">{{ m.name.charAt(0) }}</span>
          </div>
          <div class="card-body">
            <h3 class="card-provider">{{ m.name }}</h3>
            <p class="card-desc">{{ localeStore.isZhCN ? m.description : m.descriptionEn }}</p>
          </div>
        </div>

        <div class="card-models">
          <span v-for="mdl in m.models" :key="mdl" class="model-tag" :style="{ background: m.color + '14', borderColor: m.color + '28', color: m.color }">{{ mdl }}</span>
        </div>

        <div class="card-footer">
          <span class="card-hint">{{ $t('freeModels.clickHint') }}</span>
          <span class="card-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"/><polyline points="7 7 17 7 17 17"/></svg>
          </span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else :description="$t('freeModels.noResult')" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, TopRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useLocaleStore } from '../../stores/locale'
import { freeModels, modelCategories } from '../../utils/freeModels'

const { t } = useI18n()
const localeStore = useLocaleStore()

const searchQuery = ref('')
const activeCategory = ref('all')

const categories = computed(() => [
  { id: 'all', label: t('freeModels.all'), labelEn: 'All' },
  ...modelCategories,
])

const filteredModels = computed(() => {
  let list = freeModels
  const q = searchQuery.value.toLowerCase().trim()
  if (q) {
    list = list.filter(m =>
      m.name.toLowerCase().includes(q) ||
      m.models.some(mdl => mdl.toLowerCase().includes(q)) ||
      m.id.toLowerCase().includes(q)
    )
  }
  if (activeCategory.value !== 'all') {
    list = list.filter(m => m.category === activeCategory.value)
  }
  return list
})

const openUrl = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

const hexToRgb = (hex) => {
  const c = hex.replace('#', '')
  return parseInt(c.substring(0, 2), 16) + ',' + parseInt(c.substring(2, 4), 16) + ',' + parseInt(c.substring(4, 6), 16)
}
</script>

<style scoped>
.free-models-page {
  width: 100%;
}
.page-title-sm {
  font-size: 20px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0 0 4px;
  letter-spacing: 1px;
}
.page-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 24px;
  font-weight: 300;
}

.free-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.free-search {
  width: 320px;
}
.free-search :deep(.el-input__wrapper) {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color-light) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  transition: all 0.3s;
}
.free-search :deep(.el-input__wrapper:hover),
.free-search :deep(.el-input__wrapper.is-focus) {
  border-color: var(--border-color-hover) !important;
  background: var(--bg-card-hover) !important;
}
.free-search :deep(.el-input__inner) {
  color: var(--text-primary);
}
.free-search :deep(.el-input__inner::placeholder) {
  color: var(--text-muted);
}
.category-tabs {
  display: flex;
  gap: 4px;
}

/* ========== 卡片网格 ========== */
.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

/* ========== 卡片本体 ========== */
.free-card {
  --brand: #4a6fa5;
  --brand-rgb: 74, 111, 165;

  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  padding: 0;
  backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  user-select: none;
}

/* 彩色顶条 */
.card-accent {
  height: 4px;
  width: 100%;
  flex-shrink: 0;
  opacity: 0.7;
}

/* 角落光晕 */
.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 160px;
  pointer-events: none;
  opacity: 0.6;
  transition: opacity 0.4s;
}

/* ========== Hover / 亮暗主题适配 ========== */
.free-card:hover {
  border-color: rgba(var(--brand-rgb), 0.35);
  transform: translateY(-4px);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.25),
    0 0 60px rgba(var(--brand-rgb), 0.06);
}
.free-card:hover .card-glow {
  opacity: 1;
}
.free-card:active {
  transform: translateY(-1px);
}

/* 亮色主题下阴影更明显 */
[data-theme='light'] .free-card:hover {
  box-shadow:
    0 8px 30px rgba(0, 0, 0, 0.08),
    0 0 40px rgba(var(--brand-rgb), 0.08);
}

/* ========== 内容布局 ========== */
.card-top-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px 0;
  position: relative;
  z-index: 1;
}

/* 图标区 — 带微妙的 3D 悬浮感 */
.card-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  transition: transform 0.3s;
}
.free-card:hover .card-icon-wrap {
  transform: scale(1.06) translateY(-1px);
}
.brand-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}
.brand-fallback {
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}
.brand-fallback-hidden {
  display: none;
}

/* 右侧文字 */
.card-body {
  flex: 1;
  min-width: 0;
}
.card-provider {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
  letter-spacing: 0.3px;
}
.card-desc {
  font-size: 12.5px;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.5;
  font-weight: 300;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ========== 模型标签 ========== */
.card-models {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 12px 20px 0;
  position: relative;
  z-index: 1;
}
.model-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 8px;
  border: 1px solid;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  transition: all 0.2s;
}
.free-card:hover .model-tag {
  filter: brightness(1.1);
}

/* ========== 底部 ========== */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin-top: 12px;
  border-top: 1px solid var(--border-color);
  position: relative;
  z-index: 1;
}
.card-hint {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 300;
}
.card-arrow {
  color: var(--text-muted);
  opacity: 0.4;
  transition: all 0.3s;
}
.free-card:hover .card-arrow {
  opacity: 0.8;
  color: rgba(var(--brand-rgb), 1);
  transform: translateX(2px);
}

/* 响应式 */
@media (max-width: 768px) {
  .free-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .free-search {
    width: 100%;
  }
  .model-grid {
    grid-template-columns: 1fr;
  }
  .free-models-page { padding: 0; }
  .free-card { padding: 16px; }
}
</style>
