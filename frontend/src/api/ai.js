import request from './request'

export function generateActivityCopywriting(data) {
  return request.post('/ai/activity/copywriting', data)
}
