import assert from 'node:assert/strict'
import test from 'node:test'
import {
  appendRegistrationNotice,
  buildAppliedCopywritingDescription,
  buildActivityCopywritingPayload,
  canGenerateActivityCopywriting
} from './activityCopywriting.js'

test('AI copywriting button requires activity title', () => {
  assert.equal(canGenerateActivityCopywriting({ title: '' }), false)
  assert.equal(canGenerateActivityCopywriting({ title: ' 校园活动 ' }), true)
})

test('builds copywriting payload without sensitive student fields', () => {
  const payload = buildActivityCopywritingPayload({
    title: '校园摄影分享会',
    activityCategory: '文体活动',
    campus: '龙子湖校区',
    location: '大学生活动中心201',
    activityMode: 'offline',
    maxParticipants: 120,
    allowCrossCampus: true,
    phone: '13800000000',
    studentNo: '2321241389'
  })

  assert.equal(payload.activityName, '校园摄影分享会')
  assert.equal(payload.activityType, '文体活动')
  assert.equal(JSON.stringify(payload).includes('13800000000'), false)
  assert.equal(JSON.stringify(payload).includes('2321241389'), false)
})

test('appends registration notice only after user confirmation', () => {
  const result = appendRegistrationNotice('活动介绍', '请准时参加')

  assert.equal(result, '活动介绍\n\n报名须知：请准时参加')
  assert.equal(appendRegistrationNotice(result, '请准时参加'), result)
})

test('builds full applied copywriting description from summary and notice', () => {
  const description = buildAppliedCopywritingDescription({
    summary: '这是一场校园活动。',
    registrationNotice: '报名成功后请准时参加。'
  })

  assert.equal(description, '这是一场校园活动。\n\n报名须知：报名成功后请准时参加。')
})
