import Vue from 'vue'
import axios from 'axios'
import router from '../router'

// 统一 axios 封装：自动携带 token、统一错误处理
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 延迟获取 $message，确保 Vue.use(ElementUI) 已执行
function msg(text) {
  Vue.prototype.$message.error(text)
}

// 请求拦截器：携带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['authorization'] = token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器：统一处理业务码与 HTTP 错误，优先显示后端返回的中文错误消息
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      msg(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res.data
  },
  error => {
    if (error.response) {
      const status = error.response.status
      // 优先取后端返回的错误消息（格式: { code: xxx, msg: "中文错误" }）
      const backendMsg = error.response.data && error.response.data.msg
      switch (status) {
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          msg(backendMsg || '登录已过期，请重新登录')
          router.push('/login')
          break
        case 403:
          msg(backendMsg || '无权执行此操作')
          break
        case 404:
          msg(backendMsg || '请求的资源不存在')
          break
        case 429:
          msg('请求过于频繁，请稍后再试')
          break
        case 500:
          msg(backendMsg || '服务器内部错误，请稍后重试')
          break
        default:
          // 4xx 其他状态码（如 400 参数错误、自定义业务异常）
          msg(backendMsg || ('请求失败（' + status + '）'))
      }
    } else if (error.code === 'ECONNABORTED') {
      msg('请求超时，请检查网络后重试')
    } else {
      msg('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
