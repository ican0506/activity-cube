import request from './request'

export function listActivityNotices(activityId) {
  return request.get(`/admin/activities/${activityId}/notices`)
}

export function publishActivityNotice(activityId, data) {
  return request.post(`/admin/activities/${activityId}/notices`, data)
}

export function publishSystemNotice(data) {
  return request.post('/admin/notices/system', data)
}
