import request from './request'

export function generateActivityCopywriting(data) {
  return request.post('/ai/activity/copywriting', data)
}

export function generateActivitySummary(activityId) {
  return request.post(`/ai/activity/${activityId}/summary`)
}
