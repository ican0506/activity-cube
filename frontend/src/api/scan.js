import request from './request'

export function resolveCheckinCode(code) {
  return request.get('/scan/checkin-code/resolve', { params: { code } })
}
