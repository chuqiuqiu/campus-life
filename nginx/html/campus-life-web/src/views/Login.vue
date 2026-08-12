<template>
    <div class="login-wrap">
        <el-card class="login-card">
            <div class="login-title">🎓 轻享校园生活服务平台</div>
            <el-form :model="form" :rules="rules" ref="loginForm">
                <el-form-item prop="phone">
                    <el-input v-model="form.phone" placeholder="手机号" size="large" prefix-icon="el-icon-mobile-phone"/>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                              prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleLogin"/>
                </el-form-item>
                <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
                    登录
                </el-button>
            </el-form>
            <div class="tips">预置账号：13800000001 / 123</div>
        </el-card>
    </div>
</template>

<script>
import { login } from '../api/user'
import { setToken, setUserInfo } from '../utils/auth'

export default {
    name: 'Login',
    data() {
        return {
            loading: false,
            form: { phone: '', password: '' },
            rules: {
                phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            }
        }
    },
    methods: {
        handleLogin() {
            this.$refs.loginForm.validate(async valid => {
                if (!valid) return
                this.loading = true
                try {
                    const data = await login(this.form)
                    setToken(data.token)
                    setUserInfo({ userId: data.userId, nickName: data.nickName, avatar: data.avatar })
                    this.$message.success('登录成功，欢迎回来！')
                    this.$router.push('/')
                } catch (e) {
                    // 错误已在 request.js 中统一提示（显示后端返回的消息）
                    // 这里做额外处理：密码类错误自动清空密码框
                    if (e.message && (
                        e.message.includes('密码') || 
                        e.message.includes('账号') || 
                        e.message.includes('手机号'))
                    ) {
                        this.form.password = ''
                    }
                    // 其他错误（如限流、系统繁忙等）由全局拦截器统一提示
                } finally {
                    this.loading = false
                }
            })
        }
    }
}
</script>

<style scoped>
.login-wrap {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 70vh;
}

.login-card {
    width: 420px;
    padding: 20px;
}

.login-title {
    font-size: 22px;
    font-weight: bold;
    text-align: center;
    margin-bottom: 30px;
    color: #409EFF;
}

.tips {
    margin-top: 16px;
    text-align: center;
    color: #999;
    font-size: 12px;
}
</style>
