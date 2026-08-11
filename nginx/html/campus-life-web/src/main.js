import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'

Vue.use(ElementUI)
Vue.config.productionTip = false

// 全局过滤器：分转元
Vue.filter('fen2yuan', function (val) {
  if (val === null || val === undefined) return '0.00'
  return (val / 100).toFixed(2)
})

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
