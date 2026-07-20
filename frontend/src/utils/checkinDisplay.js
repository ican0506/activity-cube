const CHECKIN_TYPE_LABELS = {
  qr: '扫码签到',
  online: '线上签到',
  manual: '人工补签'
}

export function getCheckinTypeLabel(type) {
  return CHECKIN_TYPE_LABELS[type] || '签到'
}

export function enrichCheckins(checkins = [], registrations = []) {
  const registrationsByUserId = new Map(registrations.map((item) => [item.userId, item]))
  const registrationsById = new Map(registrations.map((item) => [item.id, item]))
  return checkins.map((checkin) => ({
    ...(registrationsById.get(checkin.registrationId) || registrationsByUserId.get(checkin.userId) || {}),
    ...checkin
  }))
}
