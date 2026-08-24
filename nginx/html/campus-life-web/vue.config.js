const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    port: 8088,
    proxy: {
      // 开发环境代理到本机网关（后端跑在 IDEA 里）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        pathRewrite: { '^/api': '' }
      }
    }
  }
})
