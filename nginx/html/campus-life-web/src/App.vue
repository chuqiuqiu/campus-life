<template>
    <div id="app">
        <div class="header">
            <div class="header-inner">
                <div class="logo" @click="$router.push('/')">🎓 轻享校园</div>
                <el-menu mode="horizontal" :default-active="activeMenu" router background-color="#409EFF"
                         text-color="#fff" active-text-color="#ffd04b" class="menu">
                    <el-menu-item index="/">首页</el-menu-item>
                    <el-menu-item index="/items">闲置市场</el-menu-item>
                    <el-menu-item index="/activities">校园活动</el-menu-item>
                    <el-menu-item index="/infos">校园资讯</el-menu-item>
                    <el-menu-item index="/losts">失物招领</el-menu-item>
                </el-menu>
                <div class="user-area">
                    <template v-if="userInfo">
                        <el-dropdown @command="handleCommand">
                            <span class="user-name">
                                <el-avatar :size="32" :src="userInfo.avatar">{{ userInfo.nickName[0] }}</el-avatar>
                                {{ userInfo.nickName }}（积分 {{ userInfo.credit }}）
                            </span>
                            <el-dropdown-menu slot="dropdown">
                                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </el-dropdown>
                    </template>
                    <el-button v-else type="text" @click="$router.push('/login')" style="color:#fff">登录</el-button>
                </div>
            </div>
        </div>
        <div class="main">
            <router-view/>
        </div>
    </div>
</template>

<script>
import { getUserInfo, clearAuth } from './utils/auth'

export default {
    name: 'App',
    data() {
        return {
            userInfo: getUserInfo()
        }
    },
    computed: {
        activeMenu() {
            return this.$route.path
        }
    },
    watch: {
        // 路由变化时重新读取本地登录态（登录/退出后跳转，右上角立即刷新用户名）
        '$route'() {
            this.userInfo = getUserInfo()
        }
    },
    methods: {
        handleCommand(command) {
            if (command === 'profile') {
                this.$router.push('/profile').catch(() => {})
            } else if (command === 'logout') {
                clearAuth()
                this.userInfo = null
                this.$message.success('已退出登录')
                // 跳转回首页，catch 抑制 NavigationDuplicated（已在首页时）
                this.$router.push('/').catch(() => {})
            }
        }
    }
}
</script>

<style>
* {
    margin: 0;
    padding: 0;
}

body {
    background: #f5f7fa;
    font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}

.header {
    background: #409EFF;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-inner {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    height: 60px;
}

.logo {
    color: #fff;
    font-size: 20px;
    font-weight: bold;
    cursor: pointer;
    margin-right: 30px;
    white-space: nowrap;
}

.menu {
    border-bottom: none !important;
    flex: 1;
    background: transparent !important;
}

.menu .el-menu-item {
    color: #fff !important;
}

.user-area {
    color: #fff;
}

.user-name {
    color: #fff;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
}

.main {
    max-width: 1200px;
    margin: 20px auto;
    min-height: calc(100vh - 120px);
}

.page-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    margin-bottom: 20px;
}

.item-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
}

.item-card {
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: all .2s;
}

.item-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.item-card .cover {
    height: 160px;
    background: #e8eef5;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40px;
    color: #a0b4c8;
}

.item-card .body {
    padding: 12px;
}

.item-card .title {
    font-size: 15px;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.item-card .price {
    color: #f56c6c;
    font-size: 16px;
    font-weight: bold;
    margin-top: 6px;
}

.item-card .meta {
    color: #999;
    font-size: 12px;
    margin-top: 6px;
}

.pager {
    margin-top: 20px;
    text-align: center;
}
</style>
