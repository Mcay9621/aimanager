/**
 * 常用模型 API 预设 — 用于模型管理页面快速添加
 * 每个预设包含创建模型所需的全部字段
 */
export const modelPresets = [
  // ===== OpenAI =====
  {
    name: 'GPT-4o',
    type: 'openai',
    endpoint: 'https://api.openai.com/v1',
    modelName: 'gpt-4o',
    enabled: 1,
  },
  {
    name: 'GPT-4o-mini',
    type: 'openai',
    endpoint: 'https://api.openai.com/v1',
    modelName: 'gpt-4o-mini',
    enabled: 1,
  },
  {
    name: 'o1',
    type: 'openai',
    endpoint: 'https://api.openai.com/v1',
    modelName: 'o1',
    enabled: 1,
  },
  {
    name: 'o3-mini',
    type: 'openai',
    endpoint: 'https://api.openai.com/v1',
    modelName: 'o3-mini',
    enabled: 1,
  },
  // ===== Anthropic =====
  {
    name: 'Claude 3.5 Sonnet',
    type: 'anthropic',
    endpoint: 'https://api.anthropic.com/v1',
    modelName: 'claude-3-5-sonnet-20241022',
    enabled: 1,
  },
  {
    name: 'Claude 3.5 Haiku',
    type: 'anthropic',
    endpoint: 'https://api.anthropic.com/v1',
    modelName: 'claude-3-5-haiku-20241022',
    enabled: 1,
  },
  {
    name: 'Claude 3 Opus',
    type: 'anthropic',
    endpoint: 'https://api.anthropic.com/v1',
    modelName: 'claude-3-opus-20240229',
    enabled: 1,
  },
  // ===== DeepSeek =====
  {
    name: 'DeepSeek-V3',
    type: 'deepseek',
    endpoint: 'https://api.deepseek.com/v1',
    modelName: 'deepseek-chat',
    enabled: 1,
  },
  {
    name: 'DeepSeek-R1',
    type: 'deepseek',
    endpoint: 'https://api.deepseek.com/v1',
    modelName: 'deepseek-reasoner',
    enabled: 1,
  },
  // ===== 阿里通义千问 =====
  {
    name: 'Qwen-Max',
    type: 'ali',
    endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    modelName: 'qwen-max',
    enabled: 1,
  },
  {
    name: 'Qwen2.5-72B',
    type: 'ali',
    endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    modelName: 'qwen2.5-72b-instruct',
    enabled: 1,
  },
  {
    name: 'Qwen-Turbo',
    type: 'ali',
    endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    modelName: 'qwen-turbo',
    enabled: 1,
  },
  // ===== 百度文心 =====
  {
    name: 'ERNIE 4.0',
    type: 'baidu',
    endpoint: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat',
    modelName: 'completions_pro',
    enabled: 1,
  },
  {
    name: 'ERNIE 3.5',
    type: 'baidu',
    endpoint: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat',
    modelName: 'ernie-3.5-8k',
    enabled: 1,
  },
  // ===== 字节豆包 =====
  {
    name: '豆包 Pro',
    type: 'byte',
    endpoint: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions',
    modelName: 'doubao-pro-32k',
    enabled: 1,
  },
  {
    name: '豆包 1.5',
    type: 'byte',
    endpoint: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions',
    modelName: 'doubao-1.5-pro',
    enabled: 1,
  },
  // ===== 腾讯混元 =====
  {
    name: '混元 Large',
    type: 'tencent',
    endpoint: 'https://api.hunyuan.cloud.tencent.com/v1/chat/completions',
    modelName: 'hunyuan-large',
    enabled: 1,
  },
  {
    name: '混元 Standard',
    type: 'tencent',
    endpoint: 'https://api.hunyuan.cloud.tencent.com/v1/chat/completions',
    modelName: 'hunyuan-standard',
    enabled: 1,
  },
  // ===== 智谱 GLM =====
  {
    name: 'GLM-4-Plus',
    type: 'zhipu',
    endpoint: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    modelName: 'glm-4-plus',
    enabled: 1,
  },
  {
    name: 'GLM-4-Air',
    type: 'zhipu',
    endpoint: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    modelName: 'glm-4-air',
    enabled: 1,
  },
  // ===== Kimi =====
  {
    name: 'Kimi K2',
    type: 'kimi',
    endpoint: 'https://api.moonshot.cn/v1',
    modelName: 'kimi-k2',
    enabled: 1,
  },
  // ===== 零一万物 Yi =====
  {
    name: 'Yi-Lightning',
    type: 'yi',
    endpoint: 'https://api.01.ai/v1',
    modelName: 'yi-lightning',
    enabled: 1,
  },
  {
    name: 'Yi-Large',
    type: 'yi',
    endpoint: 'https://api.01.ai/v1',
    modelName: 'yi-large',
    enabled: 1,
  },
  // ===== 百川 =====
  {
    name: 'Baichuan 4',
    type: 'baichuan',
    endpoint: 'https://api.baichuan-ai.com/v1',
    modelName: 'Baichuan4',
    enabled: 1,
  },
  // ===== 讯飞星火 =====
  {
    name: '星火 4.0',
    type: 'spark',
    endpoint: 'https://spark-api.xf-yun.com/v4.0/chat',
    modelName: 'spark-4.0',
    enabled: 1,
  },
  // ===== MiniMax =====
  {
    name: 'MiniMax-Text-01',
    type: 'minimax',
    endpoint: 'https://api.minimax.chat/v1',
    modelName: 'MiniMax-Text-01',
    enabled: 1,
  },
  // ===== 阶跃星辰 =====
  {
    name: 'Step-2',
    type: 'step',
    endpoint: 'https://api.stepfun.com/v1',
    modelName: 'step-2-16k',
    enabled: 1,
  },
  // ===== Google Gemini =====
  {
    name: 'Gemini 2.0 Flash',
    type: 'google',
    endpoint: 'https://generativelanguage.googleapis.com/v1beta',
    modelName: 'gemini-2.0-flash',
    enabled: 1,
  },
  // ===== Mistral =====
  {
    name: 'Mistral Large',
    type: 'mistral',
    endpoint: 'https://api.mistral.ai/v1',
    modelName: 'mistral-large-latest',
    enabled: 1,
  },
  // ===== Grok =====
  {
    name: 'Grok 3',
    type: 'grok',
    endpoint: 'https://api.x.ai/v1',
    modelName: 'grok-3',
    enabled: 1,
  },
]

/**
 * 按 provider 分组
 */
export function groupPresetsByType() {
  const map = {}
  modelPresets.forEach(p => {
    if (!map[p.type]) map[p.type] = []
    map[p.type].push(p)
  })
  return map
}
