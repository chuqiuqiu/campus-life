<template>
    <div class="login-wrap">
        <el-card class="login-card">
            <div class="login-title">🎓 轻享校园生活服务平台</div>
            <el-tabs v-model="mode" class="login-tabs" @tab-click="handleTabClick">
                <el-tab-pane label="登录" name="login"></el-tab-pane>
                <el-tab-pane label="注册" name="register"></el-tab-pane>
            </el-tabs>
            <el-form :model="form" :rules="rules" ref="loginForm">
                <el-form-item prop="phone">
                    <el-input v-model="form.phone" placeholder="手机号" size="large" prefix-icon="el-icon-mobile-phone"/>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                              prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleLogin"/>
                </el-form-item>
                <el-form-item v-if="mode === 'register'" prop="confirmPassword">
                    <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large"
                              prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleLogin"/>
                </el-form-item>
                <el-form-item v-if="mode === 'register'" prop="nickName">
                    <el-input v-model="form.nickName" placeholder="昵称（选填）" size="large" prefix-icon="el-icon-user"/>
                </el-form-item>
                <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
                    {{ mode === 'login' ? '登录' : '注册' }}
                </el-button>
            </el-form>
            <div class="tips" v-if="mode === 'login'">预置账号：13800000001 / 123</div>
            <div class="tips" v-else>注册成功即自动登录，无需再次输入</div>
        </el-card>
    </div>
</template>

<script>
import { login, register } from '../api/user'
import { setToken, setUserInfo } from '../utils/auth'

export default {
    name: 'Login',
    data() {
        // 手机号格式校验（与后端 UserRegisterDTO 的 @Pattern 保持一致）
        const phoneValidator = (rule, value, callback) => {
            if (!value) return callback(new Error('请输入手机号'))
            if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式错误'))
            callback()
        }
        // 确认密码校验：必须与密码一致
        const confirmPasswordValidator = (rule, value, callback) => {
            if (!value) return callback(new Error('请再次输入密码'))
            if (value !== this.form.password) return callback(new Error('两次输入的密码不一致'))
            callback()
        }
        return {
            loading: false,
            mode: 'login',
            form: { phone: '', password: '', confirmPassword: '', nickName: '' },
            loginRules: {
                phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            },
            registerRules: {
                phone: [{ required: true, validator: phoneValidator, trigger: 'blur' }],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, max: 32, message: '密码长度需在 6~32 位之间', trigger: 'blur' }
                ],
                confirmPassword: [{ required: true, validator: confirmPasswordValidator, trigger: 'blur' }],
                nickName: [{ max: 50, message: '昵称过长', trigger: 'blur' }]
            }
        }
    },
    computed: {
        rules() {
            return this.mode === 'register' ? this.registerRules : this.loginRules
        }
    },
    methods: {
        handleTabClick() {
            // 切换登录/注册时清空旧校验提示
            this.$refs.loginForm && this.$refs.loginForm.clearValidate()
        },
        handleLogin() {
            this.$refs.loginForm.validate(async valid => {
                if (!valid) return
                this.loading = true
                try {
                    const isRegister = this.mode === 'register'
                    const data = isRegister ? await register(this.form) : await login(this.form)
                    setToken(data.token)
                    setUserInfo({ userId: data.userId, nickName: data.nickName, avatar: data.avatar })
                    this.$message.success(isRegister ? '注册成功，已自动登录！' : '登录成功，欢迎回来！')
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
                        if (this.mode === 'register') this.form.confirmPassword = ''
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
    margin-bottom: 20px;
    color: #409EFF;
}

.login-tabs {
    margin-bottom: 10px;
}

.tips {
    margin-top: 16px;
    text-align: center;
    color: #999;
    font-size: 12px;
}
</style>
