import assert from 'node:assert/strict'
import test from 'node:test'

import {
  canCheckin,
  canFeedback,
  checkinDisabledReason,
  feedbackDisabledReason,
  activityCampusText,
  activityModeText,
  activityLocationText,
  activityScopeMatches,
  activityCategoryText,
  isStudentVisibleActivity,
  isOnlineActivity,
  registerDisabledReason,
  studentActivityStatus,
  studentActivityStatusText,
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

test('blocks student actions while an activity is pending review or rejected', () => {
  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()

  assert.equal(canCheckin({ status: 'PENDING_REVIEW', checkinStartTime: started, checkinEndTime: later }), false)
  assert.equal(checkinDisabledReason({ status: 'PENDING_REVIEW' }), '活动正在审核中')
  assert.equal(registerDisabledReason({ status: 'REJECTED' }), '活动审核未通过')
})

test('activity mode helpers default missing mode to offline', () => {
  assert.equal(isOnlineActivity({ activityMode: 'online' }), true)
  assert.equal(isOnlineActivity({ activityMode: 'offline' }), false)
  assert.equal(activityModeText({ activityMode: 'online' }), '线上活动')
  assert.equal(activityModeText({ activityMode: 'hybrid' }), '混合活动')
  assert.equal(activityModeText({}), '线下活动')
})

test('student activity status hides backend workflow states', () => {
  assert.equal(isStudentVisibleActivity({ status: 'DRAFT' }), false)
  assert.equal(isStudentVisibleActivity({ status: 'PENDING_REVIEW' }), false)
  assert.equal(isStudentVisibleActivity({ status: 'REJECTED' }), false)
  assert.equal(isStudentVisibleActivity({ status: 'CANCELLED' }), false)
  assert.equal(studentActivityStatusText({ status: 'REGISTERING' }), '报名中')
  assert.equal(studentActivityStatusText({ status: 'ENDED' }), '已结束')
})

test('student activity status derives checkin window as checkin active', () => {
  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()

  const activity = { status: 'ONGOING', checkinStartTime: started, checkinEndTime: later }

  assert.equal(studentActivityStatus(activity), 'CHECKIN')
  assert.equal(studentActivityStatusText(activity), '签到中')
})

test('activity category helper limits student-facing categories', () => {
  assert.equal(activityCategoryText({ activityCategory: '公益活动' }), '公益活动')
  assert.equal(activityCategoryText({ activityCategory: '学院活动' }), '其他')
})

test('activity scope helper supports student hall range filters', () => {
  assert.equal(activityScopeMatches({ campus: '龙子湖校区' }, '全部'), true)
  assert.equal(activityScopeMatches({ campus: '全校区' }, '全校区'), true)
  assert.equal(activityScopeMatches({ activityMode: 'online' }, '线上活动'), true)
  assert.equal(activityScopeMatches({ activityMode: 'offline', campus: '龙子湖校区' }, '线上活动'), false)
})

test('activity card display falls back for online activities without location', () => {
  assert.equal(activityLocationText({ activityMode: 'online', location: '' }), '线上活动')
  assert.equal(activityLocationText({ activityMode: 'offline', location: '' }), '地点待定')
  assert.equal(activityCampusText({ campus: '全校区' }), '全校区')
})

test('user role and status labels support student role migration', () => {
  assert.equal(userRoleText('user'), '学生')
  assert.equal(userRoleText('student'), '学生')
  assert.equal(userRoleText('organizer'), '活动负责人')
  assert.equal(userStatusText(1), '启用')
  assert.equal(userStatusText(0), '禁用')
})
