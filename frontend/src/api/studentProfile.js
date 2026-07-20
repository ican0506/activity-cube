import request from './request'

export function getStudentProfile() {
  return request.get('/student/profile')
}

export function updateStudentProfile(data) {
  return request.put('/student/profile', data)
}

export function uploadStudentAvatar(file, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/student/profile/avatar', formData, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function getStudentProfileSummary() {
  return request.get('/student/profile/summary')
}

export function getStudentProfileTodos() {
  return request.get('/student/profile/todos')
}

export function changeStudentPassword(data) {
  return request.put('/student/password', data)
}
