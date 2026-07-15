import request from './request'

export function getActivityStats(id) {
  return request.get(`/activities/${id}/stats`)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}
