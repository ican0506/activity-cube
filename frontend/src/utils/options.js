export const campuses = ['全部', '全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
export const userCampuses = ['龙子湖校区', '文化路校区', '许昌校区']
export const statuses = ['全部', 'REGISTERING', 'ONGOING', 'ENDED', 'CANCELLED', 'DRAFT']

export function statusText(status) {
  const map = {
    DRAFT: '草稿',
    REGISTERING: '报名中',
    ONGOING: '进行中',
    ENDED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status
}
