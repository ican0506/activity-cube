import request from './request'

export function listActivities(params) {
  return request.get('/activities', { params })
}

export function getActivity(id) {
  return request.get(`/activities/${id}`)
}

export function createActivity(data) {
  return request.post('/activities', data)
}

export function updateActivity(id, data) {
  return request.put(`/activities/${id}`, data)
}

export function deleteActivity(id) {
  return request.delete(`/activities/${id}`)
}

export function submitActivityReview(id) {
  return request.post(`/activities/${id}/submit-review`)
}

export function cancelActivity(id) {
  return request.post(`/activities/${id}/cancel`)
}

export function listPendingActivityReviews() {
  return request.get('/activities/admin/reviews')
}

export function approveActivityReview(id) {
  return request.post(`/activities/admin/${id}/review/approve`)
}

export function rejectActivityReview(id, data) {
  return request.post(`/activities/admin/${id}/review/reject`, data)
}

export function getActivityQrCode(id) {
  return request.get(`/admin/activities/${id}/qrcode`)
}

export function generateActivityCheckinCode(id) {
  return request.post(`/admin/activities/${id}/checkin-code`)
}
