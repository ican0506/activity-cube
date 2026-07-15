import * as XLSX from 'xlsx'

const ILLEGAL_FILE_CHARS = /[\\/:*?"<>|]/g

export function safeExcelFileName(activityName, suffix) {
  const safeName = String(activityName || '活动')
    .replace(ILLEGAL_FILE_CHARS, '_')
    .replace(/\s+/g, ' ')
    .trim() || '活动'

  return `${safeName}_${suffix}.xlsx`
}

export function checkedRegistrations(registrations = [], checkins = []) {
  const checkedKeys = checkinKeys(checkins)
  return registrations.filter((item) => checkedKeys.has(keyByRegistration(item)) || checkedKeys.has(keyByUser(item)))
}

export function uncheckedRegistrations(registrations = [], checkins = []) {
  const checkedKeys = checkinKeys(checkins)
  return registrations.filter((item) => !checkedKeys.has(keyByRegistration(item)) && !checkedKeys.has(keyByUser(item)))
}

export function buildRosterRows(registrations = [], checkins = []) {
  const checkinByKey = new Map()
  for (const item of checkins) {
    checkinByKey.set(keyByRegistration(item), item)
    checkinByKey.set(keyByUser(item), item)
  }

  return registrations.map((item) => {
    const checkin = checkinByKey.get(keyByRegistration(item)) || checkinByKey.get(keyByUser(item))
    return {
      姓名: item.name || '',
      学号: item.studentNo || '',
      学院: item.college || '',
      班级: item.majorClass || item.className || '',
      手机号: item.phone || '',
      校区: item.campus || checkin?.campus || '',
      报名时间: item.createdAt || item.createTime || '',
      签到时间: checkin?.checkinTime || item.checkinTime || ''
    }
  })
}

export function exportRosterExcel({ activityName, suffix, registrations = [], checkins = [] }) {
  const rows = buildRosterRows(registrations, checkins)
  const worksheet = XLSX.utils.json_to_sheet(rows, {
    header: ['姓名', '学号', '学院', '班级', '手机号', '校区', '报名时间', '签到时间']
  })
  worksheet['!cols'] = [
    { wch: 12 },
    { wch: 16 },
    { wch: 18 },
    { wch: 18 },
    { wch: 16 },
    { wch: 14 },
    { wch: 22 },
    { wch: 22 }
  ]

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, suffix)
  XLSX.writeFile(workbook, safeExcelFileName(activityName, suffix))
}

function checkinKeys(checkins) {
  const keys = new Set()
  for (const item of checkins) {
    keys.add(keyByRegistration(item))
    keys.add(keyByUser(item))
  }
  return keys
}

function keyByRegistration(item) {
  const id = item?.registrationId ?? item?.id
  return id == null ? '' : `registration:${id}`
}

function keyByUser(item) {
  const id = item?.userId
  return id == null ? '' : `user:${id}`
}
