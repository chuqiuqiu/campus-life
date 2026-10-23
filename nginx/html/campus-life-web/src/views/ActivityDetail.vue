<template>
    <div v-if="act" class="page-card">
        <div class="detail-head">
            <div class="cover-lg">
                <span>🎪</span>
                <img v-if="act.image" class="cover-img" :src="act.image" alt=""
                     @error="$event.target.style.display='none'"/>
            </div>
            <div class="detail-info">
                <div class="title">{{ act.title }}</div>
                <el-tag size="small" :type="statusType(act.status)">{{ statusText(act.status) }}</el-tag>
                <div class="meta">
                    <p>📍 地点：{{ act.location }}</p>
                    <p>🕐 时间：{{ formatTime(act.startTime) }} ~ {{ formatTime(act.endTime) }}</p>
                    <p>👥 报名：{{ act.signedCount }} / {{ act.maxPeople }}</p>
                    <p>👍 点赞：{{ act.likeCount }}</p>
                    <p>🔥 热度：{{ act.heat }}</p>
                    <p>👤 组织者：{{ act.organizerName || ('用户' + act.organizerId) }}</p>
                </div>
                <div class="actions">
                    <el-button v-if="!act.signedUp && act.status === 0" type="primary" icon="el-icon-plus"
                               @click="handleSignup">立即报名</el-button>
                    <el-button v-if="act.signedUp && act.status !== 2" type="danger" plain icon="el-icon-remove"
                               @click="handleCancelSignup">取消报名</el-button>
                    <el-button v-if="act.signedUp && !act.checkin && act.status === 1" type="success" icon="el-icon-check"
                               @click="handleCheckin">打卡</el-button>
                    <el-tag v-if="act.signedUp && act.checkin" type="success" size="small" style="margin-left:8px">已打卡</el-tag>
                    <el-button :type="act.liked ? 'warning' : 'default'" :plain="!act.liked" icon="el-icon-star-on"
                               @click="handleLike">点赞 {{ act.likeCount }}</el-button>
                    <el-button v-if="act.isOwner" type="danger" icon="el-icon-delete"
                               @click="handleDelete">删除活动</el-button>
                </div>
            </div>
        </div>
        <el-divider/>
        <div class="desc">
            <div class="desc-title">活动介绍</div>
            <p>{{ act.description || '暂无介绍' }}</p>
        </div>
    </div>
</template>

<script>
import { getActivityById, signupActivity, cancelSignup, checkinActivity, likeActivity, deleteActivity } from '../api/activity'
import { getToken } from '../utils/auth'

export default {
    name: 'ActivityDetail',
    data() {
        return {
            act: null
        }
    },
    created() {
        this.load()
    },
    methods: {
        async load() {
            this.act = await getActivityById(this.$route.params.id)
        },
        async handleSignup() {
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            const data = await signupActivity(this.$route.params.id)
            this.$message.success('报名成功！凭证编号：' + data.signNo)
            this.load()
        },
        async handleCancelSignup() {
            await cancelSignup(this.$route.params.id)
            this.$message.success('已取消报名，名额已释放')
            this.load()
        },
        async handleDelete() {
            this.$confirm('确定删除该活动吗？删除后不可恢复！', '删除确认', {
                type: 'warning'
            }).then(async () => {
                await deleteActivity(this.$route.params.id)
                this.$message.success('活动已删除')
                this.$router.push('/activities')
            }).catch(() => {})
        },
        async handleCheckin() {
            await checkinActivity(this.$route.params.id)
            this.$message.success('打卡成功')
            this.load()
        },
        async handleLike() {
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            await likeActivity(this.$route.params.id)
            this.load()
        },
        statusText(s) {
            return ['报名中', '进行中', '已结束'][s] || '未知'
        },
        statusType(s) {
            return ['success', 'warning', 'info'][s] || 'info'
        },
        formatTime(t) {
            return t ? t.replace('T', ' ').substring(0, 16) : ''
        }
    }
}
</script>

<style scoped>
.detail-head {
    display: flex;
    gap: 20px;
}

.cover-lg {
    width: 260px;
    height: 200px;
    background: #e8eef5;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 60px;
    flex-shrink: 0;
}

.detail-info {
    flex: 1;
}

.title {
    font-size: 22px;
    font-weight: bold;
    margin-bottom: 10px;
}

.meta {
    color: #555;
    line-height: 2;
    margin: 12px 0;
}

.desc-title {
    font-weight: bold;
    margin-bottom: 10px;
}

.desc p {
    color: #555;
    line-height: 1.8;
}
</style>
