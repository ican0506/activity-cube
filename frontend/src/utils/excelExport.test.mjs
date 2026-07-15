import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildRosterRows,
  checkedRegistrations,
  safeExcelFileName,
  uncheckedRegistrations
} from './excelExport.js'

const registrations = [
  {
    id: 11,
    userId: 3,
    name: '张三',
    studentNo: '2026001',
    college: '信息工程学院',
    majorClass: '软件 2301',
    phone: '13800000001',
    campus: '龙子湖校区',
    createdAt: '2026-07-15 09:00:00'
  },
  {
    id: 12,
    userId: 4,
    name: '李四',
    studentNo: '2026002',
    college: '管理学院',
    majorClass: '工商 2302',
    phone: '13800000002',
    campus: '文化路校区',
    createdAt: '2026-07-15 10:00:00'
  }
]

const checkins = [
  {
    id: 21,
    activityId: 1,
    userId: 3,
    registrationId: 11,
    campus: '龙子湖校区',
    checkinTime: '2026-07-16 14:00:00'
  }
]

test('builds required Excel filename and strips illegal characters', () => {
  assert.equal(safeExcelFileName('活动/魔方:测试', '报名名单'), '活动_魔方_测试_报名名单.xlsx')
})

test('maps roster rows to required Chinese columns', () => {
  const rows = buildRosterRows(registrations, checkins)

  assert.deepEqual(rows[0], {
    姓名: '张三',
    学号: '2026001',
    学院: '信息工程学院',
    班级: '软件 2301',
    手机号: '13800000001',
    校区: '龙子湖校区',
    报名时间: '2026-07-15 09:00:00',
    签到时间: '2026-07-16 14:00:00'
  })
  assert.equal(rows[1].签到时间, '')
})

test('filters checked and unchecked registrations by checkin records', () => {
  assert.deepEqual(checkedRegistrations(registrations, checkins).map((item) => item.id), [11])
  assert.deepEqual(uncheckedRegistrations(registrations, checkins).map((item) => item.id), [12])
})
