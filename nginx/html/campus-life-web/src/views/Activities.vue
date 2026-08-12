<template>
    <div>
        <el-row :gutter="20">
            <el-col :span="18">
                <div class="page-card">
                    <div class="toolbar">
                        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:140px" @change="loadData">
                            <el-option label="报名中" :value="0"/>
                            <el-option label="进行中" :value="1"/>
                            <el-option label="已结束" :value="2"/>
                        </el-select>
                        <el-button type="success" @click="dialogVisible = true">发布活动</el-button>
                    </div>
                    <div v-for="act in activities" :key="act.id" class="activity-item" @click="$router.push('/activities/' + act.id)">
                        <div class="act-cover">🎪</div>
                        <div class="act-info">
                            <div class="act-title">{{ act.title }}</div>
                            <div class="act-meta">
                                <span>📍 {{ act.location }}</span>
                                <span>🕐 {{ formatTime(act.startTime) }} ~ {{ formatTime(act.endTime) }}</span>
                                <span>👥 {{ act.signedCount }}/{{ act.maxPeople }}</span>
                                <span>👍 {{ act.likeCount }}</span>
                                <span>🔥 {{ act.heat }}</span>
                            </div>
                            <div class="act-status">
                                <el-tag size="small" :type="statusType(act.status)">{{ statusText(act.status) }}</el-tag>
                            </div>
                        </div>
                    </div>
                    <div v-if="!activities.length" class="empty">暂无活动</div>
                    <div class="pager">
                        <el-pagination background layout="prev, pager, next" :total="total"
                                       :page-size="query.pageSize" :current-page.sync="query.pageNo" @current-change="loadData"/>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="page-card">
                    <div class="section-title">🏆 热度排行榜</div>
                    <div v-for="act in tops" :key="act.id" class="rank-item" @click="$router.push('/activities/' + act.id)">
                        <span class="rank-no" :class="'rank-' + act.rank">{{ act.rank }}</span>
                        <span class="rank-title">{{ act.title }}</span>
                        <span class="rank-heat">{{ act.heat }}</span>
                    </div>
                </div>
            </el-col>
        </el-row>

        <el-dialog title="发布活动" :visible.sync="dialogVisible" width="520px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="标题" required>
                    <el-input v-model="form.title"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" :rows="3"/>
                </el-form-item>
                <el-form-item label="地点">
                    <el-input v-model="form.location"/>
                </el-form-item>
                <el-form-item label="开始时间" required>
                    <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss"
                                    placeholder="选择开始时间" style="width:100%"/>
                </el-form-item>
                <el-form-item label="结束时间" required>
                    <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss"
                                    placeholder="选择结束时间" style="width:100%"/>
                </el-form-item>
                <el-form-item label="人数上限" required>
                    <el-input-number v-model="form.maxPeople" :min="1" :max="1000"/>
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
import { queryActivityPage, addActivity, topActivities } from '../api/activity'
import { getToken } from '../utils/auth'

export default {
    name: 'Activities',
    data() {
        return {
            query: { pageNo: 1, pageSize: 6, status: null },
            activities: [],
            tops: [],
            total: 0,
            dialogVisible: false,
            form: { title: '', description: '', location: '', startTime: '', endTime: '', maxPeople: 100 }
        }
    },
    created() {
        this.loadData()
        this.loadTops()
    },
    methods: {
        async loadData() {
            const data = await queryActivityPage(this.query)
            this.activities = data.list || []
            this.total = Number(data.total || 0)
        },
        async loadTops() {
            this.tops = await topActivities(10)
        },
        async handleAdd() {
            if (!this.form.title || !this.form.startTime || !this.form.endTime) {
                return this.$message.warning('请填写完整信息')
            }
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            await addActivity(this.form)
            this.$message.success('发布成功')
            this.dialogVisible = false
            this.loadData()
            this.loadTops()
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
.toolbar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
}

.activity-item {
    display: flex;
    gap: 16px;
    padding: 14px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
}

.activity-item:hover {
    background: #f8fbff;
}

.act-cover {
    width: 90px;
    height: 90px;
    background: #e8eef5;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    flex-shrink: 0;
}

.act-info {
    flex: 1;
}

.act-title {
    font-size: 16px;
    font-weight: bold;
}

.act-meta {
    color: #888;
    font-size: 13px;
    margin-top: 8px;
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
}

.act-status {
    margin-top: 8px;
}

.section-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 12px;
}

.rank-item {
    display: flex;
    align-items: center;
    padding: 8px 0;
    cursor: pointer;
    border-bottom: 1px solid #f5f5f5;
}

.rank-no {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    background: #c0c4cc;
    color: #fff;
    text-align: center;
    line-height: 20px;
    font-size: 12px;
    margin-right: 8px;
    flex-shrink: 0;
}

.rank-1 { background: #f56c6c; }
.rank-2 { background: #e6a23c; }
.rank-3 { background: #67c23a; }

.rank-title {
    flex: 1;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.rank-heat {
    color: #f56c6c;
    font-size: 12px;
}

.empty {
    color: #999;
    text-align: center;
    padding: 30px;
}
</style>
