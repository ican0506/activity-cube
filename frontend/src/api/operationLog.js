import request from './request'

export function listOperationLogs(params) {
  return request.get('/admin/operation-logs', { params })
}
