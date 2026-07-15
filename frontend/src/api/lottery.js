import request from './request'

export function drawLottery(id, data) {
  return request.post(`/activities/${id}/lottery/draw`, data)
}

export function listLotteryResults(id) {
  return request.get(`/activities/${id}/lottery/results`)
}
