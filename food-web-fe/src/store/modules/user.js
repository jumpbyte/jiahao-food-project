import router from '@/router'
import cache from '@/plugins/cache'
import { ElMessageBox, } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from "@/utils/validate"
import useLockStore from '@/store/modules/lock'
import defAva from '@/assets/images/profile.jpg'

const useUserStore = defineStore(
  'user',
  {
    state: () => ({
      token: getToken(),
      id: '',
      name: '',
      nickName: '',
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      // 登录
      login(userInfo) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        return new Promise((resolve, reject) => {
          login(username, password).then(res => {
            // 后端返回格式：{ code: 0, data: { token: '...' }, message: 'success' }
            // 经过 request.js 适配后 code:0 -> code:200，res 就是 res.data
            const token = res.data?.token || res.token
            if (token) {
              setToken(token)
              this.token = token
              useLockStore().unlockScreen()
              resolve()
            } else {
              reject(new Error('登录失败，未获取到token'))
            }
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 获取用户信息
      getInfo() {
        return new Promise((resolve, reject) => {
          getInfo().then(res => {
            const responseData = res.data || res;
            const user = responseData.user;
            let avatar = user?.avatar || "";
            if (!isHttp(avatar)) {
              avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
            }
            if (responseData.roles && responseData.roles.length > 0) {
              this.roles = responseData.roles
              this.permissions = responseData.permissions || []
            } else {
              this.roles = ['ROLE_DEFAULT']
            }
            this.id = user?.userId
            this.name = user?.userName
            this.nickName = user?.nickName
            this.avatar = avatar
            cache.session.set('pwrChrtype', responseData.pwdChrtype ?? 0)
            resolve(responseData)
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 退出系统
      logOut() {
        return new Promise((resolve, reject) => {
          logout(this.token).then(() => {
            this.token = ''
            this.roles = []
            this.permissions = []
            removeToken()
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
