import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export function upload(file, region, school, anonymousId, isPublic = true) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('region', region || '未知地区')
  formData.append('school', school || '未知学校')
  formData.append('isPublic', isPublic)
  return apiClient.post('/upload', formData, {
    timeout: 120000,
    headers: {
      'X-Anonymous-Id': anonymousId || ''
    }
  })
}

export function detect(paperId) {
  return apiClient.post(`/detect/${paperId}`, null, {
    timeout: 60000
  })
}

export function shitsify(paperId, style) {
  return apiClient.post(`/shitsify/${paperId}`, { style }, {
    timeout: 120000
  })
}

export function getReview(paperId) {
  return apiClient.get(`/review/${paperId}`)
}

export function getLeaderboard(type = 'individual', page = 1, size = 20) {
  return apiClient.get('/leaderboard', {
    params: { type, page, size }
  })
}

export function getPaper(id, anonymousId) {
  return apiClient.get(`/paper/${id}`, {
    headers: {
      'X-Anonymous-Id': anonymousId || ''
    }
  })
}

export function getUserPapers(anonymousId) {
  return apiClient.get(`/paper/user/${anonymousId}`)
}

export function donate(amount, anonymousId) {
  return apiClient.post('/donate', { amount, anonymousId })
}

export function getServerStatus() {
  return apiClient.get('/server-status')
}

export function getStats() {
  return apiClient.get('/stats')
}

export default apiClient
