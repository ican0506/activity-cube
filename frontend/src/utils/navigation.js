const studentNav = [
  { label: '活动大厅', to: '/activities' },
  { label: '消息中心', to: '/messages', badge: 'messages' }
]

const organizerNav = [
  { label: '活动管理中心', to: '/admin/activities' },
  { label: '创建活动', to: '/admin/activities/create' }
]

const adminNav = [
  { label: '活动管理中心', to: '/admin/activities' },
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
