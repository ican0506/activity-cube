import request from './request'

export function listAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function createOrganizer(data) {
  return request.post('/admin/users/organizers', data)
}

export function updateAdminUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

export function updateAdminUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

export function resetAdminUserPassword(id, password) {
  return request.put(`/admin/users/${id}/reset-password`, { password })
}

export function updateAdminUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, { role })
}
