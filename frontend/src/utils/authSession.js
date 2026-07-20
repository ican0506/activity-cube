export const AUTH_TOKEN_KEY = 'activity_cube_token'
export const AUTH_USER_KEY = 'activity_cube_user'
export const DEFAULT_LOGIN_TARGET = '/activities'

export function persistAuthSession(storage, token, userInfo) {
  storage.setItem(AUTH_TOKEN_KEY, token || '')
  storage.setItem(AUTH_USER_KEY, JSON.stringify(userInfo || null))
}

export function clearAuthSession(storage) {
  storage.removeItem(AUTH_TOKEN_KEY)
  storage.removeItem(AUTH_USER_KEY)
}

export function getStoredToken(storage) {
  return storage.getItem(AUTH_TOKEN_KEY) || ''
}

export function getStoredUser(storage) {
  const rawUser = storage.getItem(AUTH_USER_KEY)
  if (!rawUser) return null
  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export function applyAuthHeader(config, storage) {
  const token = getStoredToken(storage)
  if (!token) return config
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
  return config
}

export function buildLoginRedirect(pathname, search = '') {
  return `/login?redirect=${encodeURIComponent(`${pathname}${search}`)}`
}

export function normalizeLoginTarget(target, fallback = DEFAULT_LOGIN_TARGET) {
  const value = Array.isArray(target) ? target[0] : target
  if (!value || typeof value !== 'string') return fallback
  return value.startsWith('/') && !value.startsWith('//') ? value : fallback
}

export function defaultTargetForRole(role) {
  if (role === 'admin') return '/admin/activities'
  if (role === 'organizer') return '/admin/activities'
  return DEFAULT_LOGIN_TARGET
}
