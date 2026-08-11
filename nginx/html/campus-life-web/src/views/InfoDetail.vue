<template>
    <div v-if="info" class="page-card">
        <h2>{{ info.title }}</h2>
        <div class="meta">
            <el-tag size="mini">{{ info.category }}</el-tag>
            <span>浏览 {{ info.viewCount }}</span>
            <span>发布 {{ formatTime(info.createTime) }}</span>
            <el-button size="mini" :type="info.liked ? 'warning' : 'default'" :plain="!info.liked"
                       icon="el-icon-star-on" @click="handleLike">点赞 {{ info.likeCount }}</el-button>
        </div>
        <el-divider/>
        <div class="content">{{ info.content }}</div>
    </div>
</template>

<script>
import { getInfoById, likeInfo } from '../api/info'
import { getToken } from '../utils/auth'

export default {
    name: 'InfoDetail',
    data() {
        return { info: null }
    },
    created() {
        this.load()
    },
    methods: {
        async load() {
            this.info = await getInfoById(this.$route.params.id)
        },
        async handleLike() {
            if (!getToken()) return this.$message.warning('请先登录')
            await likeInfo(this.$route.params.id)
            this.load()
        },
        formatTime(t) {
            return t ? t.replace('T', ' ').substring(0, 16) : ''
        }
    }
}
</script>

<style scoped>
.meta {
    color: #888;
    font-size: 13px;
    display: flex;
    gap: 14px;
    align-items: center;
    margin-top: 10px;
}

.content {
    color: #444;
    line-height: 2;
}
</style>
