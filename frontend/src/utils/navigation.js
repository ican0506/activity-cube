const studentNav = [
  { label: 'Dashboard', to: '/' },
  { label: '活动大厅', to: '/activities' },
  { label: '消息中心', to: '/messages', badge: 'messages' },
  { label: '我的活动', to: '/my-activities' }
]

const organizerNav = [
  { label: 'Dashboard', to: '/' },
  { label: '活动管理', to: '/admin/activities' },
  { label: '消息反馈', to: '/admin/feedbacks' },
  { label: '数据统计', to: '/admin/dashboard' }
]

const adminNav = [
  { label: 'Dashboard', to: '/' },
  { label: '数据总览', to: '/admin/dashboard' },
  { label: '活动管理', to: '/admin/activities' },
  { label: '活动审核', to: '/admin/activity-reviews' },
  { label: '用户管理', to: '/admin/users' },
  { label: '系统通知', to: '/admin/notices/system' },
  { label: '操作日志', to: '/admin/operation-logs' }
]

export function navItemsForRole(role) {
  if (role === 'admin') return adminNav
  if (role === 'organizer') return organizerNav
  if (role === 'student' || role === 'user') return studentNav
  return [{ label: '活动大厅', to: '/activities' }]
}

export function shouldShowScanShortcut(role) {
  return role === 'student' || role === 'user'
}
