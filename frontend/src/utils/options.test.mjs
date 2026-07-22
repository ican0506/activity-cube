import assert from 'node:assert/strict'
import test from 'node:test'

import {
  canCheckin,
  canOnlineCheckin,
  canQrCheckin,
  canFeedback,
  checkinModeText,
  checkinStatusText,
  checkinDisabledReason,
  feedbackDisabledReason,
  formatDateMinute,
  activityDisplayStatusText,
  activityCampusText,
  activityModeText,
  defaultCheckinMode,
  activityLocationText,
  activityScopeMatches,
  activityCategoryText,
  feedbackTypeText,
  studentActivityAction,
  isStudentVisibleActivity,
  isOnlineActivity,
  registerDisabledReason,
  studentActivityStatus,
  studentActivityStatusText,
  userRoleText,
  userStatusText
} from './options.js'

test('formats activity time to minute precision for forms and detail pages', () => {
  assert.equal(formatDateMinute('2026-07-20T14:05:30'), '2026-07-20 14:05')
  assert.equal(formatDateMinute('2026-07-20 14:05:30'), '2026-07-20 14:05')
  assert.equal(formatDateMinute(''), '-')
})

test('activity and checkin status are displayed separately', () => {
  assert.equal(activityDisplayStatusText({ status: 'NOT_STARTED' }), '未开始报名')
  assert.equal(activityDisplayStatusText({ status: 'REGISTERING' }), '报名中')
  assert.equal(activityDisplayStatusText({ status: 'WAITING_START' }), '待开始')
  assert.equal(activityDisplayStatusText({ status: 'CANCELLED' }), '已取消')

  assert.equal(checkinStatusText({
    checkinStartTime: '2026-07-20T10:00:00',
    checkinEndTime: '2026-07-20T11:00:00'
  }, new Date('2026-07-20T09:00:00')), '签到未开始')
  assert.equal(checkinStatusText({
    checkinStartTime: '2026-07-20T10:00:00',
    checkinEndTime: '2026-07-20T11:00:00'
  }, new Date('2026-07-20T10:30:00')), '签到中')
  assert.equal(checkinStatusText({
    checkinStartTime: '2026-07-20T10:00:00',
    checkinEndTime: '2026-07-20T11:00:00'
  }, new Date('2026-07-20T12:00:00')), '签到结束')
})

test('disables checkin when activity status is ended even if checkin window is still open', () => {
  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()

  assert.equal(canCheckin({ status: 'ENDED', checkinStartTime: started, checkinEndTime: later }), false)
  assert.equal(checkinDisabledReason({ status: 'ENDED', checkinStartTime: started, checkinEndTime: later }), '当前活动已结束')
})

test('published activity can submit feedback suggestions before it ends', () => {
  assert.equal(canFeedback({ status: 'ENDED' }), true)
  assert.equal(canFeedback({ status: 'ONGOING', reviewStatus: 'PUBLISHED' }), true)
  assert.equal(canFeedback({ status: 'PENDING_REVIEW' }), false)
  assert.equal(feedbackDisabledReason({ status: 'PENDING_REVIEW' }), '活动正在审核中')
  assert.equal(feedbackTypeText('suggestion'), '活动建议')
  assert.equal(feedbackTypeText('issue'), '问题反馈')
  assert.equal(feedbackTypeText('evaluation'), '活动评价')
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

test('checkin mode is independent from activity mode', () => {
  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()

  assert.equal(defaultCheckinMode('offline'), 'qr')
  assert.equal(defaultCheckinMode('hybrid'), 'online')
  assert.equal(checkinModeText({ activityMode: 'hybrid' }), '线上签到')
  assert.equal(checkinModeText({ checkinMode: 'both' }), '线上签到 + 现场扫码签到')
  assert.equal(canOnlineCheckin({ status: 'ONGOING', activityMode: 'hybrid', checkinStartTime: started, checkinEndTime: later }), true)
  assert.equal(canQrCheckin({ status: 'ONGOING', activityMode: 'hybrid', checkinStartTime: started, checkinEndTime: later }), false)
  assert.equal(canQrCheckin({ status: 'ONGOING', checkinMode: 'qr', checkinStartTime: started, checkinEndTime: later }), true)
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

test('student activity action follows registration checkin feedback lifecycle', () => {
  const registering = { status: 'REGISTERING', registered: false, checkedIn: false }
  assert.deepEqual(studentActivityAction(registering), {
    label: '立即报名',
    to: 'register',
    disabled: false,
    type: 'primary'
  })

  assert.equal(studentActivityAction({ status: 'REGISTERING', registered: true }).label, '已报名')

  const now = new Date()
  const started = new Date(now.getTime() - 60 * 1000).toISOString()
  const later = new Date(now.getTime() + 60 * 60 * 1000).toISOString()
  assert.equal(studentActivityAction({
    status: 'ONGOING',
    registered: true,
    checkedIn: false,
    checkinStartTime: started,
    checkinEndTime: later
  }).label, '去签到')

  assert.equal(studentActivityAction({ status: 'ONGOING', registered: true, checkedIn: true }).label, '已签到')
  assert.equal(studentActivityAction({ status: 'ENDED', registered: true, checkedIn: true, feedbackSubmitted: false }).label, '去反馈')
  assert.equal(studentActivityAction({ status: 'ENDED', registered: true, checkedIn: true, feedbackSubmitted: true }).label, '已完成')
  assert.equal(studentActivityAction({ status: 'CANCELLED' }).label, '已取消')
  assert.equal(studentActivityAction({ status: 'REGISTERING', canRegister: false, maxParticipants: 10, registrationCount: 10 }).label, '名额已满')
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
