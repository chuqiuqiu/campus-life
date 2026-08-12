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
        <el-tabs v-model="tab" class="page-card" @tab-click="handleTabChange">
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
            <el-tab-pane label="我发布的闲置" name="myItems">
                <div v-for="item in myItems" :key="item.id" class="my-item">
                    <div class="my-item-info" @click="$router.push('/items/' + item.id)">
                        <span class="fav-title">{{ item.title }}</span>
                        <span class="fav-price">¥{{ item.price | fen2yuan }}</span>
                        <el-tag size="mini" :type="item.status === 0 ? 'success' : 'info'">{{ itemStatusText(item.status) }}</el-tag>
                    </div>
                    <el-button size="mini" type="danger" plain @click="handleDeleteItem(item.id)">删除</el-button>
                </div>
                <div v-if="!myItems.length" class="empty">暂未发布闲置</div>
            </el-tab-pane>
            <el-tab-pane label="我发布的活动" name="myActivities">
                <div v-for="act in myPublishedActs" :key="act.id" class="my-item">
                    <div class="my-item-info" @click="$router.push('/activities/' + act.id)">
                        <span class="fav-title">{{ act.title }}</span>
                        <el-tag size="mini">{{ statusText(act.status) }}</el-tag>
                    </div>
                    <el-button size="mini" type="danger" plain @click="handleDeleteAct(act.id)">删除</el-button>
                </div>
                <div v-if="!myPublishedActs.length" class="empty">暂未发布活动</div>
            </el-tab-pane>
            <el-tab-pane label="我发布的资讯" name="myInfos">
                <div v-for="info in myInfos" :key="info.id" class="my-item">
                    <div class="my-item-info" @click="$router.push('/infos/' + info.id)">
                        <span class="fav-title">{{ info.title }}</span>
                        <el-tag size="mini">{{ info.category }}</el-tag>
                    </div>
                    <el-button size="mini" type="danger" plain @click="handleDeleteInfo(info.id)">删除</el-button>
                </div>
                <div v-if="!myInfos.length" class="empty">暂未发布资讯</div>
            </el-tab-pane>
            <el-tab-pane label="我发布的失物" name="myLosts">
                <div v-for="lost in myLosts" :key="lost.id" class="my-item">
                    <div class="my-item-info" @click="$router.push('/losts/' + lost.id)">
                        <span class="fav-title">{{ lost.title }}</span>
                        <el-tag size="mini" :type="lost.type === 0 ? 'warning' : 'success'">{{ lost.type === 0 ? '寻物' : '招领' }}</el-tag>
                        <el-tag size="mini" :type="lost.status === 0 ? 'info' : 'success'">{{ lost.status === 0 ? '未归还' : '已归还' }}</el-tag>
                    </div>
                    <el-button size="mini" type="danger" plain @click="handleDeleteLost(lost.id)">删除</el-button>
                </div>
                <div v-if="!myLosts.length" class="empty">暂未发布失物信息</div>
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
import { myFavorites, unfavoriteItem, deleteItem, queryItemPage } from '../api/item'
import { myActivities, deleteActivity, queryActivityPage } from '../api/activity'
import { deleteInfo, queryInfoPage, deleteLost, queryLostPage } from '../api/info'

export default {
    name: 'Profile',
    data() {
        return {
            tab: 'favorites',
            user: {},
            editForm: {},
            favorites: [],
            myActs: [],
            myItems: [],
            myPublishedActs: [],
            myInfos: [],
            myLosts: []
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
        // Tab 切换时按需加载"我的发布"数据
        handleTabChange(tab) {
            const name = tab.name
            if (name === 'myItems' && !this.myItems.length) this.loadMyItems()
            if (name === 'myActivities' && !this.myPublishedActs.length) this.loadMyActivities()
            if (name === 'myInfos' && !this.myInfos.length) this.loadMyInfos()
            if (name === 'myLosts' && !this.myLosts.length) this.loadMyLosts()
        },
        async loadMyItems() {
            const data = await queryItemPage({ pageNo: 1, pageSize: 50, userId: this.user.userId })
            this.myItems = data.list || []
        },
        async loadMyActivities() {
            const data = await queryActivityPage({ pageNo: 1, pageSize: 50, userId: this.user.userId })
            this.myPublishedActs = data.list || []
        },
        async loadMyInfos() {
            const data = await queryInfoPage({ pageNo: 1, pageSize: 50, userId: this.user.userId })
            this.myInfos = data.list || []
        },
        async loadMyLosts() {
            const data = await queryLostPage({ pageNo: 1, pageSize: 50, userId: this.user.userId })
            this.myLosts = data.list || []
        },
        // 删除操作（带二次确认）
        handleDeleteItem(id) {
            this.$confirm('确定删除该闲置吗？删除后不可恢复！', '删除确认', { type: 'warning' })
                .then(async () => {
                    await deleteItem(id)
                    this.$message.success('删除成功')
                    this.myItems = this.myItems.filter(i => i.id !== id)
                }).catch(() => {})
        },
        handleDeleteAct(id) {
            this.$confirm('确定删除该活动吗？删除后不可恢复！', '删除确认', { type: 'warning' })
                .then(async () => {
                    await deleteActivity(id)
                    this.$message.success('删除成功')
                    this.myPublishedActs = this.myPublishedActs.filter(a => a.id !== id)
                }).catch(() => {})
        },
        handleDeleteInfo(id) {
            this.$confirm('确定删除该资讯吗？删除后不可恢复！', '删除确认', { type: 'warning' })
                .then(async () => {
                    await deleteInfo(id)
                    this.$message.success('删除成功')
                    this.myInfos = this.myInfos.filter(i => i.id !== id)
                }).catch(() => {})
        },
        handleDeleteLost(id) {
            this.$confirm('确定删除该失物记录吗？删除后不可恢复！', '删除确认', { type: 'warning' })
                .then(async () => {
                    await deleteLost(id)
                    this.$message.success('删除成功')
                    this.myLosts = this.myLosts.filter(l => l.id !== id)
                }).catch(() => {})
        },
        statusText(s) {
            return ['报名中', '进行中', '已结束'][s] || '未知'
        },
        itemStatusText(s) {
            return ['在售', '已售', '已下架'][s] || '未知'
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

.my-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
}

.my-item-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
}

.empty {
    color: #999;
    text-align: center;
    padding: 30px;
}
</style>
