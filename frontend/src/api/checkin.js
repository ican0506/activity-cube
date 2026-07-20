import request from './request'

export function checkinActivity(id, code) {
  return request.post(`/activities/${id}/checkin`, null, {
    params: code ? { code } : undefined
  })
}

export function manualCheckinActivity(id, data) {
  return request.post(`/activities/${id}/checkins/manual`, data)
}

export function listCheckins(id) {
  return request.get(`/activities/${id}/checkins`)
}

export function listAbsences(id) {
  return request.get(`/activities/${id}/absences`)
}

export function listAbsentees(id) {
  return request.get(`/activities/${id}/absentees`)
}

export function myCheckins() {
  return request.get('/my/checkins')
}
