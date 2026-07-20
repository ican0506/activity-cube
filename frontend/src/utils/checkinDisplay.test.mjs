import assert from 'node:assert/strict'
import test from 'node:test'
import { enrichCheckins, getCheckinTypeLabel } from './checkinDisplay.js'

test('maps checkin types to clear Chinese labels', () => {
  assert.equal(getCheckinTypeLabel('qr'), '扫码签到')
  assert.equal(getCheckinTypeLabel('online'), '线上签到')
  assert.equal(getCheckinTypeLabel('manual'), '人工补签')
})

test('enriches checkin rows with registration information', () => {
  const rows = enrichCheckins([
    { id: 21, userId: 3, registrationId: 11, checkinType: 'manual', remark: '手机没电' }
  ], [
    { id: 11, userId: 3, name: '张三', studentNo: '2024001', college: '信息工程学院', majorClass: '软件工程2401', campus: '龙子湖校区' }
  ])

  assert.deepEqual(rows[0], {
    id: 21,
    userId: 3,
    registrationId: 11,
    checkinType: 'manual',
    remark: '手机没电',
    name: '张三',
    studentNo: '2024001',
    college: '信息工程学院',
    majorClass: '软件工程2401',
    campus: '龙子湖校区'
  })
})
