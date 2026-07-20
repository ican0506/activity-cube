import request from './request'

export function listMessages(params) {
  return request.get('/messages', { params })
}

export function getUnreadMessageCount() {
  return request.get('/messages/unread-count')
}

export function markMessageRead(id) {
  return request.put(`/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}
