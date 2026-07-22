import test from 'node:test'
import assert from 'node:assert/strict'
import { navItemsForRole, shouldShowScanShortcut } from './navigation.js'

test('student navigation contains only student pages', () => {
  const labels = navItemsForRole('student').map((item) => item.label)

  assert.deepEqual(labels, ['活动大厅', '消息中心', '我的活动'])
  assert.ok(!labels.includes('扫一扫'))
})

test('admin navigation does not contain student personal pages', () => {
  const labels = navItemsForRole('admin').map((item) => item.label)

  assert.ok(!labels.includes('我的报名'))
  assert.ok(!labels.includes('我的签到'))
  assert.ok(!labels.includes('我的活动'))
  assert.ok(!labels.includes('扫一扫'))
  assert.deepEqual(labels, ['数据总览', '活动管理中心', '活动审核', '用户管理', '系统通知', '操作日志'])
})

test('organizer navigation focuses on management pages', () => {
  const labels = navItemsForRole('organizer').map((item) => item.label)

  assert.deepEqual(labels, ['活动管理中心', '创建活动', '消息通知', '数据统计'])
})

test('scan shortcut belongs only to student roles', () => {
  assert.equal(shouldShowScanShortcut('student'), true)
  assert.equal(shouldShowScanShortcut('user'), true)
  assert.equal(shouldShowScanShortcut('organizer'), false)
  assert.equal(shouldShowScanShortcut('admin'), false)
})
