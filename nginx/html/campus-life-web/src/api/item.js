import request from '../utils/request'

// 闲置服务 API
export const addItem = (data) => request.post('/items', data)
export const queryItemPage = (params) => request.get('/items/page', { params })
export const getItemById = (id) => request.get(`/items/${id}`)
export const updateItemStatus = (id, status) => request.put(`/items/${id}/status`, null, { params: { status } })
export const favoriteItem = (id) => request.post(`/items/${id}/favorite`)
export const unfavoriteItem = (id) => request.delete(`/items/${id}/favorite`)
export const isFavorite = (id) => request.get(`/items/${id}/isFavorite`)
export const myFavorites = (params) => request.get('/items/favorites', { params })
export const deleteItem = (id) => request.delete(`/items/${id}`)
