import assert from 'node:assert/strict'
import test from 'node:test'
import { buildActivityQrLinks } from './qrLinks.js'

test('builds registration and checkin qr links from current origin and activity id', () => {
  const links = buildActivityQrLinks('http://127.0.0.1:5174', 12, { checkinCode: 'checkin-12' })

  assert.equal(links.registerUrl, 'http://127.0.0.1:5174/scan/resolve?type=register&activityId=12')
  assert.equal(links.checkinUrl, 'http://127.0.0.1:5174/scan/resolve?type=checkin&activityId=12&code=checkin-12')
})

test('does not generate a checkin link before a checkin code exists', () => {
  const links = buildActivityQrLinks('http://localhost:5173/', '8')

  assert.equal(links.registerUrl, 'http://localhost:5173/scan/resolve?type=register&activityId=8')
  assert.equal(links.checkinUrl, '')
})

test('does not generate qr links before an activity id exists', () => {
  const links = buildActivityQrLinks('http://localhost:5173', null, { checkinCode: 'abc' })

  assert.equal(links.registerUrl, '')
  assert.equal(links.checkinUrl, '')
})

test('adds encoded checkin code to checkin qr link when provided', () => {
  const links = buildActivityQrLinks('http://localhost:5173', 8, { checkinCode: 'abc 123' })

  assert.equal(links.registerUrl, 'http://localhost:5173/scan/resolve?type=register&activityId=8')
  assert.equal(links.checkinUrl, 'http://localhost:5173/scan/resolve?type=checkin&activityId=8&code=abc%20123')
})
