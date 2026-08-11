import request from '../utils/request'

// 活动服务 API
export const addActivity = (data) => request.post('/activities', data)
export const queryActivityPage = (params) => request.get('/activities/page', { params })
export const getActivityById = (id) => request.get(`/activities/${id}`)
export const signupActivity = (id) => request.post(`/activities/${id}/signup`)
export const cancelSignup = (id) => request.delete(`/activities/${id}/signup`)
export const deleteActivity = (id) => request.delete(`/activities/${id}`)
export const checkinActivity = (id) => request.post(`/activities/${id}/checkin`)
export const likeActivity = (id) => request.post(`/activities/${id}/like`)
export const topActivities = (n) => request.get('/activities/top', { params: { n } })
export const myActivities = () => request.get('/activities/my')
