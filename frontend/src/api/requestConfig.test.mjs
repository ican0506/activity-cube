import assert from 'node:assert/strict'
import test from 'node:test'

import { resolveApiBaseUrl, resolveApiOrigin } from './requestConfig.js'

test('uses localhost backend api as default base url', () => {
  assert.equal(resolveApiBaseUrl({}), 'http://localhost:8080/api')
})

test('uses VITE_API_BASE_URL when provided', () => {
  assert.equal(
    resolveApiBaseUrl({ VITE_API_BASE_URL: 'http://localhost:8081/api' }),
    'http://localhost:8081/api'
  )
})

test('supports lan backend api base url for mobile devices', () => {
  assert.equal(
    resolveApiBaseUrl({ VITE_API_BASE_URL: 'http://10.74.51.156:8080/api' }),
    'http://10.74.51.156:8080/api'
  )
})

test('trims trailing slash from api base url', () => {
  assert.equal(
    resolveApiBaseUrl({ VITE_API_BASE_URL: 'http://localhost:8080/api/' }),
    'http://localhost:8080/api'
  )
})

test('derives backend origin from api base url', () => {
  assert.equal(
    resolveApiOrigin({ VITE_API_BASE_URL: 'http://localhost:8081/api' }),
    'http://localhost:8081'
  )
})
