export function canGenerateActivityCopywriting(form) {
  return Boolean(String(form?.title || '').trim())
}

export function buildActivityCopywritingPayload(form) {
  return {
    activityName: String(form?.title || '').trim(),
    activityType: form?.activityCategory || '',
    campus: form?.campus || '',
    startTime: form?.startTime || null,
    endTime: form?.endTime || null,
    location: form?.location || '',
    targetAudience: '河南农业大学在校学生',
    highlights: summarizeHighlights(form),
    registrationRequirements: summarizeRegistrationRequirements(form),
    tone: '青春'
  }
}

export function appendRegistrationNotice(description, notice) {
  const text = String(notice || '').trim()
  if (!text) return description || ''
  const current = String(description || '').trim()
  if (!current) return `报名须知：${text}`
  if (current.includes(text)) return current
  return `${current}\n\n报名须知：${text}`
}

export function buildAppliedCopywritingDescription(result) {
  const summary = String(result?.summary || '').trim()
  return appendRegistrationNotice(summary, result?.registrationNotice)
}

function summarizeHighlights(form) {
  const pieces = []
  if (form?.activityCategory) pieces.push(form.activityCategory)
  if (form?.rewardEnabled && form?.rewardType && form.rewardType !== '无') {
    pieces.push(`奖励类型：${form.rewardType}`)
  }
  if (form?.activityMode) pieces.push(`活动形式：${modeText(form.activityMode)}`)
  return pieces.join('；')
}

function summarizeRegistrationRequirements(form) {
  const pieces = []
  if (form?.maxParticipants) pieces.push(`最大报名人数 ${form.maxParticipants} 人`)
  pieces.push(form?.allowCrossCampus ? '允许跨校区报名' : '仅限本校区报名')
  return pieces.join('；')
}

function modeText(value) {
  return {
    online: '线上活动',
    offline: '线下活动',
    hybrid: '混合活动'
  }[value] || value
}
