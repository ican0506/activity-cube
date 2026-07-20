export const campuses = ['全部', '全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
export const userCampuses = ['龙子湖校区', '文化路校区', '许昌校区']
export const activityCampuses = ['全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
export const activityModes = [
  { label: '线下活动', value: 'offline' },
  { label: '线上活动', value: 'online' },
  { label: '线上线下结合', value: 'hybrid' }
]
export const activityCategories = ['公益活动', '实践活动', '志愿服务', '讲座培训', '文体活动', '竞赛活动', '社团活动', '学院活动', '其他']
export const rewardTypes = ['无', '课外学时', '积分', '证书', '实物奖励']
export const rewardStatuses = [
  { label: '不设置奖励', value: false },
  { label: '设置奖励', value: true }
]
export const statuses = ['全部', 'DRAFT', 'PENDING_REVIEW', 'REJECTED', 'NOT_STARTED', 'REGISTERING', 'WAITING_START', 'ONGOING', 'ENDED', 'CANCELLED']
export const userRoleOptions = [
  { label: '全部', value: '' },
  { label: '学生', value: 'student' },
  { label: '活动负责人', value: 'organizer' },
  { label: '管理员', value: 'admin' }
]
export const userStatusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

export function statusText(status) {
  const map = {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    PUBLISHED: '已发布',
    NOT_STARTED: '未开始',
    REGISTERING: '报名中',
    WAITING_START: '待开始',
    ONGOING: '进行中',
    ENDED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status || '-'
}

export function statusTagType(status) {
  const map = {
    DRAFT: 'info',
    PENDING_REVIEW: 'warning',
    REJECTED: 'danger',
    PUBLISHED: 'success',
    NOT_STARTED: 'primary',
    REGISTERING: 'success',
    WAITING_START: 'warning',
    ONGOING: 'success',
    ENDED: 'info',
    CANCELLED: 'danger'
  }
  return map[status] || ''
}

export function canRegister(activity) {
  return activity?.status === 'REGISTERING'
}

export function registerDisabledReason(activity) {
  const status = activity?.status
  const map = {
    DRAFT: '当前活动尚未发布',
    PENDING_REVIEW: '活动正在审核中',
    REJECTED: '活动审核未通过',
    NOT_STARTED: '当前活动未开始报名',
    WAITING_START: '当前活动报名已结束',
    ONGOING: '活动已开始，报名已结束',
    ENDED: '当前活动已结束',
    CANCELLED: '当前活动已取消'
  }
  return map[status] || '当前活动不能报名'
}

export function canCheckin(activity) {
  const now = new Date()
  const start = parseDateTime(activity?.checkinStartTime || activity?.startTime)
  const end = parseDateTime(activity?.checkinEndTime || activity?.endTime)
  if (!start || !end) return false
  return now >= start
    && now <= end
    && activity?.status !== 'DRAFT'
    && activity?.status !== 'PENDING_REVIEW'
    && activity?.status !== 'REJECTED'
    && activity?.status !== 'CANCELLED'
    && activity?.status !== 'ENDED'
}

export function isOnlineActivity(activity) {
  return activity?.activityMode === 'online'
}

export function activityModeText(activity) {
  if (activity?.activityMode === 'online') return '线上活动'
  if (activity?.activityMode === 'hybrid') return '线上线下结合'
  return '线下活动'
}

export function activityCategoryText(activity) {
  return activity?.activityCategory || '其他'
}

export function rewardSummary(activity) {
  if (!activity?.rewardEnabled || !activity?.rewardType || activity.rewardType === '无') return '无奖励'
  if (activity.rewardType === '课外学时') return `${activity.rewardHours || 0} 课外学时`
  if (activity.rewardType === '积分') return `${activity.rewardPoints || 0} 积分`
  return activity.rewardDescription || activity.rewardType
}

export function checkinDisabledReason(activity) {
  if (activity?.status === 'DRAFT') return '当前活动尚未发布'
  if (activity?.status === 'PENDING_REVIEW') return '活动正在审核中'
  if (activity?.status === 'REJECTED') return '活动审核未通过'
  if (activity?.status === 'CANCELLED') return '当前活动已取消'
  if (activity?.status === 'ENDED') return '当前活动已结束'
  const now = new Date()
  const start = parseDateTime(activity?.checkinStartTime || activity?.startTime)
  const end = parseDateTime(activity?.checkinEndTime || activity?.endTime)
  if (start && now < start) return '签到尚未开始'
  if (end && now > end) return '签到已结束'
  return '当前不能签到'
}

export function canFeedback(activity) {
  return activity?.status === 'ENDED'
}

export function feedbackDisabledReason(activity) {
  if (activity?.status === 'DRAFT') return '当前活动尚未发布'
  if (activity?.status === 'PENDING_REVIEW') return '活动正在审核中'
  if (activity?.status === 'REJECTED') return '活动审核未通过'
  if (activity?.status === 'CANCELLED') return '当前活动已取消'
  return '活动结束后才可以提交反馈'
}

export function parseDateTime(value) {
  if (!value) return null
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatEmpty(value, fallback = '-') {
  return value === undefined || value === null || value === '' ? fallback : value
}

export function userRoleText(role) {
  const map = {
    user: '学生',
    student: '学生',
    organizer: '活动负责人',
    admin: '管理员'
  }
  return map[role] || role || '-'
}

export function userRoleTagType(role) {
  const map = {
    user: 'success',
    student: 'success',
    organizer: 'warning',
    admin: 'danger'
  }
  return map[role] || 'info'
}

export function userStatusText(status) {
  return Number(status) === 0 ? '禁用' : '启用'
}

export function userStatusTagType(status) {
  return Number(status) === 0 ? 'info' : 'success'
}
