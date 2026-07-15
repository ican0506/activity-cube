import request from './request'

export function submitFeedback(id, data) {
  return request.post(`/activities/${id}/feedback`, data)
}

export function listFeedbacks(id) {
  return request.get(`/activities/${id}/feedbacks`)
}

export function getFeedbackStats(id) {
  return request.get(`/activities/${id}/feedback-stats`)
}
