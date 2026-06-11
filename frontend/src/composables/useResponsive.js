import { ref, computed, onMounted, onUnmounted } from 'vue'

const width = ref(window.innerWidth)

function onResize() {
  width.value = window.innerWidth
}

export function useResponsive() {
  const isMobile = computed(() => width.value < 768)
  const isTablet = computed(() => width.value >= 768 && width.value < 1024)
  const isDesktop = computed(() => width.value >= 1024)
  const isSmallMobile = computed(() => width.value < 480)

  return { width, isMobile, isTablet, isDesktop, isSmallMobile }
}

export function initResponsive() {
  window.addEventListener('resize', onResize)
}
