import api from './client'

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  deleteAccount: () => api.delete('/auth/account'),
}

export const questionsApi = {
  list: (params) => api.get('/questions', { params }),
  get: (id) => api.get(`/questions/${id}`),
}

export const sessionsApi = {
  create: (questionId) => api.post('/sessions', { questionId }),
  submitResponse: (id, data) => api.post(`/sessions/${id}/response`, data),
  getAnalysis: (id, type = 'MAIN') => api.get(`/sessions/${id}/analysis`, { params: { type } }),
  getFollowUp: (id) => api.get(`/sessions/${id}/followup`),
  submitFollowUp: (id, answerText) => api.post(`/sessions/${id}/followup-response`, { answerText }),
  get: (id) => api.get(`/sessions/${id}`),
  list: (params) => api.get('/sessions', { params }),
}

export const progressApi = {
  summary: (category = 'DSA') => api.get('/progress/summary', { params: { category } }),
}
