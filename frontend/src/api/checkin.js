import request from './request'

export function checkinActivity(id) {
  return request.post(`/activities/${id}/checkin`)
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
