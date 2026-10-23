<template>
    <div>
        <div class="page-card">
            <div class="toolbar">
                <el-input v-model="query.keyword" placeholder="搜索资讯" clearable style="width:220px"
                          @keyup.enter.native="loadData" @clear="loadData"/>
                <el-select v-model="query.category" placeholder="全部分类" clearable style="width:140px" @change="loadData">
                    <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
                </el-select>
                <el-button type="primary" @click="loadData">查询</el-button>
                <el-button type="success" @click="dialogVisible = true">发布资讯</el-button>
            </div>
        </div>
        <div v-for="info in infos" :key="info.id" class="page-card info-item" @click="$router.push('/infos/' + info.id)">
            <div class="info-cover">
                <span>📰</span>
                <img v-if="info.cover" class="cover-img" :src="info.cover" alt=""
                     @error="$event.target.style.display='none'"/>
            </div>
            <div class="info-body">
                <div class="info-title">{{ info.title }}</div>
                <div class="info-meta">
                    <el-tag size="mini">{{ info.category }}</el-tag>
                    <span>浏览 {{ info.viewCount }}</span>
                    <span>点赞 {{ info.likeCount }}</span>
                    <span>{{ formatTime(info.createTime) }}</span>
                </div>
            </div>
        </div>
        <div v-if="!infos.length" class="empty">暂无资讯</div>
        <div class="pager">
            <el-pagination background layout="prev, pager, next" :total="total"
                           :page-size="query.pageSize" :current-page.sync="query.pageNo" @current-change="loadData"/>
        </div>

        <el-dialog title="发布资讯" :visible.sync="dialogVisible" width="520px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="标题" required>
                    <el-input v-model="form.title" maxlength="100"/>
                </el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="form.category" style="width:200px">
                        <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="内容" required>
                    <el-input v-model="form.content" type="textarea" :rows="5"/>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleAdd">发布</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import { queryInfoPage, addInfo } from '../api/info'
import { getToken } from '../utils/auth'

export default {
    name: 'Infos',
    data() {
        return {
            categories: ['校园资讯', '通知公告', '讲座活动'],
            query: { pageNo: 1, pageSize: 8, category: null, keyword: null },
            infos: [],
            total: 0,
            dialogVisible: false,
            form: { title: '', content: '', category: '校园资讯' }
        }
    },
    created() {
        this.loadData()
    },
    methods: {
        async loadData() {
            const data = await queryInfoPage(this.query)
            this.infos = data.list || []
            this.total = Number(data.total || 0)
        },
        async handleAdd() {
            if (!this.form.title || !this.form.content) return this.$message.warning('请填写完整信息')
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            await addInfo(this.form)
            this.$message.success('发布成功')
            this.dialogVisible = false
            this.loadData()
        },
        formatTime(t) {
            return t ? t.replace('T', ' ').substring(0, 16) : ''
        }
    }
}
</script>

<style scoped>
.toolbar {
    display: flex;
    gap: 10px;
}

.info-item {
    display: flex;
    gap: 16px;
    cursor: pointer;
}

.info-cover {
    width: 90px;
    height: 70px;
    background: #e8eef5;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    flex-shrink: 0;
}

.info-title {
    font-size: 16px;
    font-weight: bold;
}

.info-meta {
    color: #888;
    font-size: 13px;
    margin-top: 10px;
    display: flex;
    gap: 14px;
    align-items: center;
}

.empty {
    color: #999;
    text-align: center;
    padding: 30px;
}
</style>
