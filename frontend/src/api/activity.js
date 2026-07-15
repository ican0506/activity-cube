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
