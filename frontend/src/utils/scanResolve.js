export function resolveScanAction(query) {
  const type = firstQueryValue(query.type)
  const activityId = firstQueryValue(query.activityId)
  if (!['register', 'checkin'].includes(type)) {
    return { error: '二维码无效' }
  }

  const code = firstQueryValue(query.code)
  if (type === 'checkin' && !activityId) {
    return { error: '签到二维码缺少活动信息，请重新扫码或输入完整签到码。' }
  }
  if (!activityId) {
    return { error: '二维码无效' }
  }

  const id = encodeURIComponent(String(activityId))
  if (type === 'register') {
    return { route: `/activities/${id}/register` }
  }

  const suffix = code ? `?code=${encodeURIComponent(String(code))}` : ''
  return { route: `/activities/${id}/checkin${suffix}` }
}

export function buildScanTarget(text, options = {}) {
  const value = String(text || '').trim()
  if (!value) return ''

  const path = parseScanPath(value)
  if (path) return path

  if (options.activityId) {
    return `/scan/resolve?type=checkin&activityId=${encodeURIComponent(String(options.activityId))}&code=${encodeURIComponent(value)}`
  }

  return ''
}

function parseScanPath(value) {
  if (value.startsWith('/scan/resolve?')) {
    return value
  }
  try {
    const url = new URL(value)
    if (url.pathname === '/scan/resolve') {
      return `${url.pathname}${url.search}`
    }
  } catch {
    return ''
  }
  return ''
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}
