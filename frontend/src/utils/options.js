export const campuses = ['全部', '全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
export const userCampuses = ['龙子湖校区', '文化路校区', '许昌校区']
export const activityCampuses = ['全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
export const activityModes = [
  { label: '线下活动', value: 'offline' },
  { label: '线上活动', value: 'online' },
  { label: '混合活动', value: 'hybrid' }
]
export const checkinModes = [
  { label: '线上签到', value: 'online' },
  { label: '现场扫码签到', value: 'qr' },
  { label: '线上签到 + 现场扫码签到', value: 'both' }
]
export const feedbackTypes = [
  { label: '活动建议', value: 'suggestion' },
  { label: '问题反馈', value: 'issue' },
  { label: '活动评价', value: 'evaluation' }
]
export const activityCategories = ['公益活动', '实践活动', '志愿服务', '讲座培训', '文体活动', '竞赛活动', '社团活动', '其他']
export const rewardTypes = ['无', '课外学时', '积分', '证书', '实物奖励']
export const rewardStatuses = [
  { label: '不设置奖励', value: false },
  { label: '设置奖励', value: true }
]
export const statuses = ['全部', 'DRAFT', 'PENDING_REVIEW', 'REJECTED', 'NOT_STARTED', 'REGISTERING', 'WAITING_START', 'ONGOING', 'ENDED', 'CANCELLED']
export const studentActivityStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '报名中', value: 'REGISTERING' },
  { label: '进行中', value: 'ONGOING' },
  { label: '签到中', value: 'CHECKIN' },
  { label: '已结束', value: 'ENDED' }
]
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

export function activityDisplayStatusText(activityOrStatus) {
  const status = typeof activityOrStatus === 'string' ? activityOrStatus : activityOrStatus?.status
  const map = {
    NOT_STARTED: '未开始报名',
    REGISTERING: '报名中',
    WAITING_START: '待开始',
    ONGOING: '进行中',
    ENDED: '已结束',
    CANCELLED: '已取消',
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    PUBLISHED: '已发布'
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

export function studentActivityStatus(activity) {
  if (!activity) return ''
  if (activity.status === 'ENDED') return 'ENDED'
  if (activity.status === 'REGISTERING') return 'REGISTERING'
  if (canCheckin(activity)) return 'CHECKIN'
  if (activity.status === 'ONGOING') return 'ONGOING'
  return ''
}

export function isStudentVisibleActivity(activity) {
  return Boolean(studentActivityStatus(activity))
}

export function studentActivityStatusText(activity) {
  const map = {
    REGISTERING: '报名中',
    ONGOING: '进行中',
    CHECKIN: '签到中',
    ENDED: '已结束'
  }
  return map[studentActivityStatus(activity)] || '-'
}

export function studentActivityStatusTagType(activity) {
  const map = {
    REGISTERING: 'success',
    ONGOING: 'primary',
    CHECKIN: 'success',
    ENDED: 'info'
  }
  return map[studentActivityStatus(activity)] || 'info'
}

export function canRegister(activity) {
  if (activity?.canRegister !== undefined && activity?.canRegister !== null) {
    return Boolean(activity.canRegister)
  }
  return activity?.status === 'REGISTERING' && !isActivityFull(activity)
}

export function isActivityFull(activity) {
  const maxParticipants = Number(activity?.maxParticipants || 0)
  if (!maxParticipants) return false
  return Number(activity?.registrationCount || 0) >= maxParticipants
}

export function registrationCountText(activity) {
  const current = Number(activity?.registrationCount || 0)
  const maxParticipants = Number(activity?.maxParticipants || 0)
  return maxParticipants ? `${current}/${maxParticipants}` : `${current}/不限`
}

export function studentActivityAction(activity) {
  if (!activity) {
    return { label: '查看详情', to: 'detail', disabled: true, type: 'info' }
  }
  if (activity.status === 'CANCELLED') {
    return { label: '已取消', to: 'none', disabled: true, type: 'danger' }
  }
  if (activity.status === 'ENDED') {
    if (activity.checkedIn && !activity.feedbackSubmitted) {
      return { label: '去反馈', to: 'feedback', disabled: false, type: 'warning' }
    }
    if (activity.checkedIn && activity.feedbackSubmitted) {
      return { label: '已完成', to: 'none', disabled: true, type: 'success' }
    }
    return { label: '已结束', to: 'none', disabled: true, type: 'info' }
  }
  if (activity.checkedIn) {
    return { label: '已签到', to: 'none', disabled: true, type: 'success' }
  }
  if (activity.registered && (activity.canCheckin || canCheckin(activity))) {
    if (canOnlineCheckin(activity)) {
      return { label: '去签到', to: 'checkin', disabled: false, type: 'primary' }
    }
    if (canQrCheckin(activity)) {
      return { label: '扫码签到', to: 'scan', disabled: false, type: 'primary' }
    }
    return { label: '当前不能签到', to: 'none', disabled: true, type: 'info' }
  }
  if (activity.registered) {
    return { label: '已报名', to: 'myActivities', disabled: false, type: 'success' }
  }
  if (isActivityFull(activity)) {
    return { label: '名额已满', to: 'none', disabled: true, type: 'info' }
  }
  if (canRegister(activity)) {
    return { label: '立即报名', to: 'register', disabled: false, type: 'primary' }
  }
  return { label: studentActivityStatusText(activity), to: 'none', disabled: true, type: 'info' }
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
  if (activity?.canCheckin === true) {
    return true
  }
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

export function defaultCheckinMode(activityMode) {
  return activityMode === 'offline' ? 'qr' : 'online'
}

export function normalizedCheckinMode(activity) {
  if (['online', 'qr', 'both'].includes(activity?.checkinMode)) {
    return activity.checkinMode
  }
  return defaultCheckinMode(activity?.activityMode)
}

export function canOnlineCheckin(activity) {
  if (activity?.canOnlineCheckin !== undefined && activity?.canOnlineCheckin !== null) {
    return Boolean(activity.canOnlineCheckin)
  }
  return canCheckin(activity) && ['online', 'both'].includes(normalizedCheckinMode(activity))
}

export function canQrCheckin(activity) {
  if (activity?.canQrCheckin !== undefined && activity?.canQrCheckin !== null) {
    return Boolean(activity.canQrCheckin)
  }
  return canCheckin(activity) && ['qr', 'both'].includes(normalizedCheckinMode(activity))
}

export function activityScopeMatches(activity, scope = '全部') {
  if (!scope || scope === '全部') return true
  if (scope === '线上活动') return activity?.activityMode === 'online'
  if (scope === '全校区') return activity?.campus === '全校区'
  return activity?.campus === scope
}

export function activityModeText(activity) {
  if (activity?.activityMode === 'online') return '线上活动'
  if (activity?.activityMode === 'hybrid') return '混合活动'
  return '线下活动'
}

export function checkinModeText(activity) {
  const mode = normalizedCheckinMode(activity)
  if (mode === 'both') return '线上签到 + 现场扫码签到'
  if (mode === 'qr') return '现场扫码签到'
  return '线上签到'
}

export function feedbackTypeText(type) {
  const map = {
    suggestion: '活动建议',
    issue: '问题反馈',
    evaluation: '活动评价'
  }
  return map[type] || '活动评价'
}

export function activityCategoryText(activity) {
  return activityCategories.includes(activity?.activityCategory) ? activity.activityCategory : '其他'
}

export function activityCampusText(activity) {
  return activity?.campus || '校区待定'
}

export function activityLocationText(activity) {
  if (activity?.location) return activity.location
  if (activity?.activityMode === 'online') return '线上活动'
  return '地点待定'
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

export function checkinStatusText(activity, now = new Date()) {
  const start = parseDateTime(activity?.checkinStartTime || activity?.startTime)
  const end = parseDateTime(activity?.checkinEndTime || activity?.endTime)
  if (!start || !end) return '签到未设置'
  if (now < start) return '签到未开始'
  if (now > end) return '签到结束'
  return '签到中'
}

export function canFeedback(activity) {
  const blocked = ['DRAFT', 'PENDING_REVIEW', 'REJECTED', 'CANCELLED']
  return !blocked.includes(activity?.status) && !blocked.includes(activity?.reviewStatus)
}

export function feedbackDisabledReason(activity) {
  const status = activity?.reviewStatus || activity?.status
  if (status === 'DRAFT') return '当前活动尚未发布'
  if (status === 'PENDING_REVIEW') return '活动正在审核中'
  if (status === 'REJECTED') return '活动审核未通过'
  if (activity?.status === 'CANCELLED' || status === 'CANCELLED') return '当前活动已取消'
  return '当前活动不能提交反馈'
}

export function parseDateTime(value) {
  if (!value) return null
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateMinute(value, fallback = '-') {
  const date = parseDateTime(value)
  if (!date) return fallback
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
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
