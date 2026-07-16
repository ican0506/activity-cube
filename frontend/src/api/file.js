import request from './request'
import { resolveApiOrigin } from './requestConfig'

export function uploadFile(file, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function uploadBatch(files, onUploadProgress) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post('/files/upload-batch', formData, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function resolveFileUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const normalizedUrl = url.startsWith('/') ? url : `/${url}`
  const apiOrigin = resolveApiOrigin(import.meta.env)
  return apiOrigin ? `${apiOrigin}${normalizedUrl}` : normalizedUrl
}
