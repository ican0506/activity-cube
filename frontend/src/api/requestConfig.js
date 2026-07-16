const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'

export function resolveApiBaseUrl(env = {}) {
  const baseUrl = env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL
  return String(baseUrl).replace(/\/+$/, '')
}

export function resolveApiOrigin(env = {}) {
  const baseUrl = resolveApiBaseUrl(env)
  try {
    return new URL(baseUrl).origin
  } catch {
    return ''
  }
}
