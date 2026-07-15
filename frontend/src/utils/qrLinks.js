export function buildActivityQrLinks(origin, activityId) {
  const base = String(origin || '').replace(/\/+$/, '')
  const id = encodeURIComponent(String(activityId))

  return {
    registerUrl: `${base}/activities/${id}/register`,
    checkinUrl: `${base}/activities/${id}/checkin`
  }
}
