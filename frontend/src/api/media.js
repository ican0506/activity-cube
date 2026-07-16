import request from './request'

export function listActivityMedia(activityId) {
  return request.get(`/activities/${activityId}/media`)
}

export function saveActivityMedia(activityId, items) {
  return request.post(`/activities/${activityId}/media`, { items })
}

export function deleteActivityMedia(mediaId) {
  return request.delete(`/activities/media/${mediaId}`)
}

export function listFeedbackMedia(feedbackId) {
  return request.get(`/feedbacks/${feedbackId}/media`)
}

export function saveFeedbackMedia(feedbackId, items) {
  return request.post(`/feedbacks/${feedbackId}/media`, { items })
}
