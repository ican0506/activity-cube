export function buildActivityQrLinks(origin, activityId, options = {}) {
  const base = String(origin || '').replace(/\/+$/, '')
  if (!activityId) {
    return { registerUrl: '', checkinUrl: '' }
  }
  const id = encodeURIComponent(String(activityId))
  const checkinCode = String(options.checkinCode || '').trim()

  return {
    registerUrl: `${base}/scan/resolve?type=register&activityId=${id}`,
    checkinUrl: checkinCode
      ? `${base}/scan/resolve?type=checkin&activityId=${id}&code=${encodeURIComponent(checkinCode)}`
      : ''
  }
}
