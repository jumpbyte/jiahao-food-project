import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

export function getToken() {
  // 先尝试从 Cookie 读取，如果没有则从 localStorage 读取
  let token = Cookies.get(TokenKey, { path: '/' })
  if (!token) {
    token = localStorage.getItem(TokenKey)
  }
  return token
}

export function setToken(token) {
  // 同时写入 Cookie 和 localStorage
  Cookies.set(TokenKey, token, { path: '/' })
  localStorage.setItem(TokenKey, token)
}

export function removeToken() {
  Cookies.remove(TokenKey, { path: '/' })
  localStorage.removeItem(TokenKey)
}
