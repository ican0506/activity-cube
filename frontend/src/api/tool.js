import request from './request'

export function draw(id, data) {
  return request.post(`/activities/${id}/tools/draw`, data)
}

export function group(id, data) {
  return request.post(`/activities/${id}/tools/group`, data)
}
