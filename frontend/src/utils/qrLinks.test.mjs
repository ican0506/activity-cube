import assert from 'node:assert/strict'
import test from 'node:test'
import { buildActivityQrLinks } from './qrLinks.js'

test('builds registration and checkin qr links from current origin and activity id', () => {
  const links = buildActivityQrLinks('http://127.0.0.1:5174', 12)

  assert.equal(links.registerUrl, 'http://127.0.0.1:5174/activities/12/register')
  assert.equal(links.checkinUrl, 'http://127.0.0.1:5174/activities/12/checkin')
})

test('removes trailing slash from origin before building qr links', () => {
  const links = buildActivityQrLinks('http://localhost:5173/', '8')

  assert.equal(links.registerUrl, 'http://localhost:5173/activities/8/register')
  assert.equal(links.checkinUrl, 'http://localhost:5173/activities/8/checkin')
})
