import request from './request'

export async function downloadExport(activityId, type) {
  const blob = await request.get(`/activities/${activityId}/export/${type}`, { responseType: 'blob' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${type}.csv`
  link.click()
  URL.revokeObjectURL(url)
}
