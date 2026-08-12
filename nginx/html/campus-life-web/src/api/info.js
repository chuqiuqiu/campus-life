import request from '../utils/request'

// 资讯服务 API
export const addInfo = (data) => request.post('/infos', data)
export const queryInfoPage = (params) => request.get('/infos/page', { params })
export const getInfoById = (id) => request.get(`/infos/${id}`)
export const likeInfo = (id) => request.post(`/infos/${id}/like`)
export const deleteInfo = (id) => request.delete(`/infos/${id}`)

// 失物招领服务 API
export const addLost = (data) => request.post('/losts', data)
export const queryLostPage = (params) => request.get('/losts/page', { params })
export const getLostById = (id) => request.get(`/losts/${id}`)
export const updateLostStatus = (id, status) => request.put(`/losts/${id}/status`, null, { params: { status } })
export const deleteLost = (id) => request.delete(`/losts/${id}`)
