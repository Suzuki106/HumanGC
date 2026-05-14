import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export function upload(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export function detect(paperId) {
  return apiClient.post(`/detect/${paperId}`)
}

export function shitsify(paperId, style) {
  return apiClient.post(`/shitsify/${paperId}`, { style })
}

export function getReview(paperId) {
  return apiClient.get(`/review/${paperId}`)
}

export function getLeaderboard(type = 'individual', page = 1, size = 20) {
  return apiClient.get('/leaderboard', {
    params: { type, page, size }
  })
}

export function getPaper(id) {
  return apiClient.get(`/paper/${id}`)
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

export default apiClient
