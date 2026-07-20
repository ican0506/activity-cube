export const majorCodeMap = {
  21241: '软件工程',
  21242: '计算机科学与技术',
  21243: '数据科学与大数据技术'
}

export function parseStudentNo(studentNo) {
  const value = String(studentNo || '').trim()
  if (!/^\d{10}$/.test(value)) {
    return { valid: false, message: '学号必须为10位数字' }
  }
  const gradeYear = `20${value.slice(0, 2)}级`
  const majorCode = value.slice(2, 7)
  const sequenceNo = value.slice(7)
  return {
    valid: true,
    gradeYear,
    majorCode,
    majorName: majorCodeMap[majorCode] || '',
    sequenceNo
  }
}
