import test from 'node:test'
import assert from 'node:assert/strict'

import { parseStudentNo } from './studentNo.js'

test('parses student number into grade major and sequence', () => {
  assert.deepEqual(parseStudentNo('2321241389'), {
    valid: true,
    gradeYear: '2023级',
    majorCode: '21241',
    majorName: '软件工程',
    sequenceNo: '389'
  })
})

test('returns invalid result for non ten digit student number', () => {
  assert.deepEqual(parseStudentNo('T2024001'), {
    valid: false,
    message: '学号必须为10位数字'
  })
})
