import request from '../utils/request'

// 用户服务 API
export const login = (data) => request.post('/users/login', data)
export const getMe = () => request.get('/users/me')
export const updateMe = (data) => request.put('/users/me', data)
export const getUserById = (id) => request.get(`/users/${id}`)
