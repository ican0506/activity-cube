import assert from 'node:assert/strict'
import test from 'node:test'

import {
  canCheckin,
  canFeedback,
  checkinDisabledReason,
  feedbackDisabledReason,
  activityModeText,
  isOnlineActivity,
  registerDisabledReason,
  userRoleText,
  userStatusText
} from './options.js'

test('disables checkin when activity status is ended even if checkin window is still open', () => {
  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()

  assert.equal(canCheckin({ status: 'ENDED', checkinStartTime: started, checkinEndTime: later }), false)
  assert.equal(checkinDisabledReason({ status: 'ENDED', checkinStartTime: started, checkinEndTime: later }), '当前活动已结束')
})

test('only ended activity can submit feedback', () => {
  assert.equal(canFeedback({ status: 'ENDED' }), true)
  assert.equal(canFeedback({ status: 'ONGOING' }), false)
  assert.equal(feedbackDisabledReason({ status: 'ONGOING' }), '活动结束后才可以提交反馈')
})

test('registration disabled reasons are user friendly', () => {
  assert.equal(registerDisabledReason({ status: 'NOT_STARTED' }), '当前活动未开始报名')
  assert.equal(registerDisabledReason({ status: 'WAITING_START' }), '当前活动报名已结束')
})

test('activity mode helpers default missing mode to offline', () => {
  assert.equal(isOnlineActivity({ activityMode: 'online' }), true)
  assert.equal(isOnlineActivity({ activityMode: 'offline' }), false)
  assert.equal(activityModeText({ activityMode: 'online' }), '线上活动')
  assert.equal(activityModeText({}), '线下活动')
})

test('user role and status labels support student role migration', () => {
  assert.equal(userRoleText('user'), '学生')
  assert.equal(userRoleText('student'), '学生')
  assert.equal(userRoleText('organizer'), '活动负责人')
  assert.equal(userStatusText(1), '启用')
  assert.equal(userStatusText(0), '禁用')
})
