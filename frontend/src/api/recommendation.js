import request from './request'

export function listActivityRecommendations() {
  return request.get('/activities/recommendations')
}
