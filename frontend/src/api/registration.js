import request from './request'

export function registerActivity(id, data) {
  return request.post(`/activities/${id}/register`, data)
}

export function listRegistrations(id) {
  return request.get(`/activities/${id}/registrations`)
}

export function myRegistrations() {
  return request.get('/my/registrations')
}

export function cancelMyRegistration(id) {
  return request.delete(`/activities/${id}/registrations/me`)
}
