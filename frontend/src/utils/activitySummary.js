function text(value) {
  const normalized = String(value || '').trim()
  return normalized || '暂无内容'
}

function numberedList(values) {
  const items = Array.isArray(values)
    ? values.map((item) => String(item || '').trim()).filter(Boolean)
    : []
  if (!items.length) return '暂无内容'
  return items.map((item, index) => `${index + 1}. ${item}`).join('\n')
}

export function buildActivitySummaryReport(summary = {}) {
  return [
    '# AI 活动复盘报告',
    '',
    '## 活动概况',
    text(summary.overview),
    '',
    '## 数据分析',
    text(summary.dataAnalysis),
    '',
    '## 活动亮点',
    numberedList(summary.highlights),
    '',
    '## 用户反馈',
    text(summary.feedbackSummary),
    '',
    '## 存在问题',
    numberedList(summary.problems),
    '',
    '## 优化建议',
    numberedList(summary.suggestions),
    '',
    '## 下一步行动',
    text(summary.nextAction)
  ].join('\n')
}
