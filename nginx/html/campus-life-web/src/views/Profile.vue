<template>
    <div>
        <div class="page-card profile-head">
            <el-avatar :size="72" :src="user.avatar">{{ user.nickName ? user.nickName[0] : 'U' }}</el-avatar>
            <div class="profile-info">
                <div class="nick">{{ user.nickName }}</div>
                <div class="meta">
                    <span>📱 {{ user.phone }}</span>
                    <span>🏫 {{ user.college }}</span>
                    <span>⭐ 积分：{{ user.credit }}</span>
                </div>
            </div>
            <el-button type="primary" plain @click="refresh">刷新</el-button>
        </div>
        <el-tabs v-model="tab" class="page-card">
            <el-tab-pane label="我的收藏" name="favorites">
                <div v-for="item in favorites" :key="item.id" class="fav-item" @click="$router.push('/items/' + item.id)">
                    <span class="fav-title">{{ item.title }}</span>
                    <span class="fav-price">¥{{ item.price | fen2yuan }}</span>
                    <el-button size="mini" type="text" @click.stop="unfavorite(item.id)">取消收藏</el-button>
                </div>
                <div v-if="!favorites.length" class="empty">暂无收藏</div>
            </el-tab-pane>
            <el-tab-pane label="我报名的活动" name="activities">
                <div v-for="act in myActs" :key="act.id" class="fav-item" @click="$router.push('/activities/' + act.id)">
                    <span class="fav-title">{{ act.title }}</span>
                    <el-tag size="mini">{{ statusText(act.status) }}</el-tag>
                </div>
                <div v-if="!myActs.length" class="empty">暂未报名活动</div>
            </el-tab-pane>
            <el-tab-pane label="个人信息" name="info">
                <el-form :model="editForm" label-width="80px" style="max-width:420px">
                    <el-form-item label="昵称">
                        <el-input v-model="editForm.nickName"/>
                    </el-form-item>
                    <el-form-item label="学院">
                        <el-input v-model="editForm.college"/>
                    </el-form-item>
                    <el-form-item label="性别">
                        <el-radio-group v-model="editForm.gender">
                            <el-radio :label="1">男</el-radio>
                            <el-radio :label="2">女</el-radio>
                            <el-radio :label="0">保密</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-button type="primary" @click="save">保存</el-button>
                </el-form>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import { getMe, updateMe } from '../api/user'
import { myFavorites, unfavoriteItem } from '../api/item'
import { myActivities } from '../api/activity'

export default {
    name: 'Profile',
    data() {
        return {
            tab: 'favorites',
            user: {},
            editForm: {},
            favorites: [],
            myActs: []
        }
    },
    created() {
        this.loadAll()
    },
    methods: {
        async loadAll() {
            this.user = await getMe()
            this.editForm = { nickName: this.user.nickName, college: this.user.college, gender: this.user.gender }
            const fav = await myFavorites({ pageNo: 1, pageSize: 20 })
            this.favorites = fav.list || []
            this.myActs = await myActivities()
        },
        async refresh() {
            await this.loadAll()
            this.$message.success('刷新成功')
        },
        async unfavorite(id) {
            await unfavoriteItem(id)
            this.$message.success('已取消收藏')
            this.favorites = this.favorites.filter(f => f.id !== id)
        },
        async save() {
            await updateMe(this.editForm)
            this.$message.success('保存成功')
            this.loadAll()
        },
        statusText(s) {
            return ['报名中', '进行中', '已结束'][s] || '未知'
        }
    }
}
</script>

<style scoped>
.profile-head {
    display: flex;
    align-items: center;
    gap: 20px;
}

.nick {
    font-size: 20px;
    font-weight: bold;
}

.meta {
    color: #888;
    font-size: 14px;
    margin-top: 8px;
    display: flex;
    gap: 20px;
}

.fav-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
}

.fav-title {
    flex: 1;
}

.fav-price {
    color: #f56c6c;
    font-weight: bold;
}

.empty {
    color: #999;
    text-align: center;
    padding: 30px;
}
</style>
