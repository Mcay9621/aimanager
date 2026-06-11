export function getEChartsTheme(isDark) {
  return {
    textColor: isDark ? 'rgba(255,255,255,0.8)' : 'rgba(0,0,0,0.75)',
    textColorSecondary: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(0,0,0,0.45)',
    textColorMuted: isDark ? '#888' : '#999',
    axisLineColor: isDark ? 'rgba(74,111,165,0.1)' : 'rgba(0,0,0,0.1)',
    splitLineColor: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.06)',
    tooltipBg: isDark ? 'rgba(18,18,26,0.95)' : 'rgba(255,255,255,0.95)',
    tooltipBorder: isDark ? 'rgba(74,111,165,0.2)' : 'rgba(0,0,0,0.1)',
    tooltipTextColor: isDark ? '#fff' : '#333',
    legendTextColor: isDark ? '#888' : '#666',
    axisLabelColor: isDark ? '#666' : '#888',
    axisLabelColorSecondary: isDark ? '#666' : '#999',
    axisLineStrokeColor: isDark ? '#333' : '#ddd',
    barCategoryColor: isDark ? '#ccc' : '#555',
  }
}
