// token 与用户信息本地管理
export function getToken() {
  return localStorage.getItem('token')
}

export function setToken(token) {
  localStorage.setItem('token', token)
}

export function getUserInfo() {
  const info = localStorage.getItem('userInfo')
  return info ? JSON.parse(info) : null
}

export function setUserInfo(info) {
  localStorage.setItem('userInfo', JSON.stringify(info))
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}
