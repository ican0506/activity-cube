import test from 'node:test'
import assert from 'node:assert/strict'
import { navItemsForRole } from './navigation.js'

test('student navigation contains only student pages', () => {
  const labels = navItemsForRole('student').map((item) => item.label)

  assert.deepEqual(labels, ['活动大厅', '消息中心'])
})

test('admin navigation does not contain student personal pages', () => {
  const labels = navItemsForRole('admin').map((item) => item.label)

  assert.ok(!labels.includes('我的报名'))
  assert.ok(!labels.includes('我的签到'))
  assert.deepEqual(labels, ['活动管理中心', '活动审核', '用户管理', '系统通知', '操作日志'])
})

test('organizer navigation focuses on management pages', () => {
  const labels = navItemsForRole('organizer').map((item) => item.label)

  assert.deepEqual(labels, ['活动管理中心', '创建活动'])
})
