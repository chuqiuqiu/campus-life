import axios from 'axios'
import { Message } from 'element-ui'
import router from '../router'

// 统一 axios 封装：自动携带 token、统一错误处理
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：携带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['authorization'] = token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器：统一处理业务码与 401
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      Message.error('请先登录')
      router.push('/login')
    } else if (error.response && error.response.status === 429) {
      Message.error('请求过于频繁，请稍后再试')
    } else {
      Message.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
