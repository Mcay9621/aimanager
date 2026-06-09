import request from './request'

let cachedTypes = null

/**
 * 加载模型类型列表（带缓存）
 * 返回 [{ key, label, color, consoleUrl }, ...]
 */
export async function getModelTypes() {
  if (cachedTypes) return cachedTypes
  try {
    const data = await request.get('/dict/model-types')
    cachedTypes = data || []
  } catch {
    cachedTypes = []
  }
  return cachedTypes
}

/**
 * 获取类型显示名
 */
export function getTypeLabel(types, key) {
  const t = types.find(t => t.key === key)
  return t ? t.label : key
}

/**
 * 获取类型颜色
 */
export function getTypeColor(types, key) {
  const t = types.find(t => t.key === key)
  return t ? t.color : '#888'
}

/**
 * 获取类型背景色（带透明度）
 */
export function getTypeBg(types, key) {
  const color = getTypeColor(types, key)
  return color + '26' // 15% 透明度
}

/**
 * 获取控制台 URL
 */
export function getConsoleUrl(types, key) {
  const t = types.find(t => t.key === key)
  return t ? t.consoleUrl : '#'
}

/**
 * 清空缓存（用于重新加载）
 */
export function clearCache() {
  cachedTypes = null
}
