import assert from 'node:assert/strict'
import test from 'node:test'

import { buildScanTarget, resolveScanAction } from './scanResolve.js'

test('rejects invalid scan resolve query', () => {
  assert.deepEqual(resolveScanAction({ type: 'register' }), { error: '二维码无效' })
  assert.deepEqual(resolveScanAction({ type: 'unknown', activityId: '8' }), { error: '二维码无效' })
})

test('explains when a checkin scan is missing activity information', () => {
  assert.deepEqual(resolveScanAction({ type: 'checkin', code: 'abc 123' }), {
    error: '签到二维码缺少活动信息，请重新扫码或输入完整签到码。'
  })
  assert.deepEqual(resolveScanAction({ type: 'checkin' }), {
    error: '签到二维码缺少活动信息，请重新扫码或输入完整签到码。'
  })
})

test('resolves registration scan query to activity registration page', () => {
  assert.deepEqual(resolveScanAction({ type: 'register', activityId: '8' }), {
    route: '/activities/8/register'
  })
})

test('resolves checkin scan query to checkin page and preserves code', () => {
  assert.deepEqual(resolveScanAction({ type: 'checkin', activityId: '8', code: 'abc 123' }), {
    route: '/activities/8/checkin?code=abc%20123'
  })
})

test('builds scan target from full url, relative url, or manual checkin code', () => {
  assert.equal(
    buildScanTarget('http://localhost:5173/scan/resolve?type=register&activityId=8'),
    '/scan/resolve?type=register&activityId=8'
  )
  assert.equal(
    buildScanTarget('/scan/resolve?type=checkin&activityId=8&code=abc'),
    '/scan/resolve?type=checkin&activityId=8&code=abc'
  )
  assert.equal(
    buildScanTarget('abc 123', { activityId: '8' }),
    '/scan/resolve?type=checkin&activityId=8&code=abc%20123'
  )
})
