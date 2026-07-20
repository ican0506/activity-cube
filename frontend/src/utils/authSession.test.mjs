import assert from 'node:assert/strict'
import test from 'node:test'

import {
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  applyAuthHeader,
  buildLoginRedirect,
  clearAuthSession,
  defaultTargetForRole,
  normalizeLoginTarget,
  persistAuthSession
} from './authSession.js'

function memoryStorage() {
  const data = new Map()
  return {
    getItem: (key) => data.get(key) || null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key)
  }
}

test('persists token and user info to localStorage compatible storage', () => {
  const storage = memoryStorage()

  persistAuthSession(storage, 'mock-token-1', { username: 'student001', role: 'user' })

  assert.equal(storage.getItem(AUTH_TOKEN_KEY), 'mock-token-1')
  assert.equal(JSON.parse(storage.getItem(AUTH_USER_KEY)).username, 'student001')
})

test('adds Authorization bearer token header from storage', () => {
  const storage = memoryStorage()
  storage.setItem(AUTH_TOKEN_KEY, 'mock-token-2')
  const config = { headers: {} }

  applyAuthHeader(config, storage)

  assert.equal(config.headers.Authorization, 'Bearer mock-token-2')
})

test('clears token and user info from storage', () => {
  const storage = memoryStorage()
  storage.setItem(AUTH_TOKEN_KEY, 'mock-token-3')
  storage.setItem(AUTH_USER_KEY, '{"username":"admin"}')

  clearAuthSession(storage)

  assert.equal(storage.getItem(AUTH_TOKEN_KEY), null)
  assert.equal(storage.getItem(AUTH_USER_KEY), null)
})

test('keeps scanned qr target as login redirect', () => {
  assert.equal(
    buildLoginRedirect('/activities/8/register', '?from=qr'),
    '/login?redirect=%2Factivities%2F8%2Fregister%3Ffrom%3Dqr'
  )
})

test('normalizes login redirect query target', () => {
  assert.equal(normalizeLoginTarget('/activities/8/checkin'), '/activities/8/checkin')
  assert.equal(normalizeLoginTarget(['/activities/8/checkin']), '/activities/8/checkin')
  assert.equal(normalizeLoginTarget('https://example.com/evil'), '/activities')
  assert.equal(normalizeLoginTarget(''), '/activities')
})

test('chooses default login target by role when redirect is absent', () => {
  assert.equal(defaultTargetForRole('student'), '/activities')
  assert.equal(defaultTargetForRole('user'), '/activities')
  assert.equal(defaultTargetForRole('organizer'), '/admin/activities')
  assert.equal(defaultTargetForRole('admin'), '/admin/activities')
})
