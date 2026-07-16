export function buildActivityQrLinks(origin, activityId, options = {}) {
  const base = String(origin || '').replace(/\/+$/, '')
  const id = encodeURIComponent(String(activityId))
  const code = options.checkinCode
    ? `?code=${encodeURIComponent(String(options.checkinCode))}`
    : ''

  return {
    registerUrl: `${base}/activities/${id}/register`,
    checkinUrl: `${base}/activities/${id}/checkin${code}`
  }
}
