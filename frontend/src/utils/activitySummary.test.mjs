import test from 'node:test'
import assert from 'node:assert/strict'
import { buildActivitySummaryReport } from './activitySummary.js'

test('builds copyable AI activity summary report with all required sections', () => {
  const report = buildActivitySummaryReport({
    overview: '活动整体组织有序。',
    dataAnalysis: '报名20人，签到16人。',
    highlights: ['参与积极', '流程清晰', '反馈正向'],
    feedbackSummary: '同学们认可活动安排。',
    problems: ['仍有未签到学生'],
    suggestions: ['提前发送签到提醒', '完善现场指引'],
    nextAction: '下次活动前一日发布提醒。'
  })

  assert.match(report, /# AI 活动复盘报告/)
  assert.match(report, /## 活动概况\n活动整体组织有序。/)
  assert.match(report, /## 数据分析\n报名20人，签到16人。/)
  assert.match(report, /## 活动亮点\n1\. 参与积极\n2\. 流程清晰\n3\. 反馈正向/)
  assert.match(report, /## 用户反馈\n同学们认可活动安排。/)
  assert.match(report, /## 存在问题\n1\. 仍有未签到学生/)
  assert.match(report, /## 优化建议\n1\. 提前发送签到提醒\n2\. 完善现场指引/)
  assert.match(report, /## 下一步行动\n下次活动前一日发布提醒。/)
})

test('uses fallback text when AI summary list sections are empty', () => {
  const report = buildActivitySummaryReport({})

  assert.match(report, /## 活动亮点\n暂无内容/)
  assert.match(report, /## 存在问题\n暂无内容/)
  assert.match(report, /## 优化建议\n暂无内容/)
})
