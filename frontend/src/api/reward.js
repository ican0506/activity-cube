import request from './request'

export function issueActivityRewards(activityId, data = {}) {
  return request.post(`/admin/activities/${activityId}/rewards/issue`, data)
}

export function listActivityRewards(activityId) {
  return request.get(`/admin/activities/${activityId}/rewards`)
}

export function listMyRewards() {
  return request.get('/student/rewards')
}
