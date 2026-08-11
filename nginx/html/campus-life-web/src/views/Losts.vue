<template>
    <div>
        <div class="page-card">
            <div class="toolbar">
                <el-select v-model="query.type" placeholder="全部类型" clearable style="width:140px" @change="loadData">
                    <el-option label="寻物启事" :value="0"/>
                    <el-option label="失物招领" :value="1"/>
                </el-select>
                <el-input v-model="query.keyword" placeholder="搜索" clearable style="width:220px"
                          @keyup.enter.native="loadData" @clear="loadData"/>
                <el-button type="primary" @click="loadData">查询</el-button>
                <el-button type="success" @click="dialogVisible = true">发布信息</el-button>
            </div>
        </div>
        <div v-for="lost in losts" :key="lost.id" class="page-card lost-item">
            <div class="lost-cover">🔍</div>
            <div class="lost-body">
                <div class="lost-title">
                    {{ lost.title }}
                    <el-tag size="mini" :type="lost.type === 0 ? 'danger' : 'success'">
                        {{ lost.type === 0 ? '寻物' : '招领' }}
                    </el-tag>
                    <el-tag size="mini" :type="lost.status === 0 ? 'warning' : 'success'">
                        {{ lost.status === 0 ? '寻找中' : '已解决' }}
                    </el-tag>
                </div>
                <div class="lost-meta">
                    <span>编号：{{ lost.lostNo }}</span>
                    <span>物品：{{ lost.itemType }}</span>
                    <span>地点：{{ lost.location }}</span>
                    <span>联系：{{ lost.contact }}</span>
                    <span>{{ formatTime(lost.createTime) }}</span>
                </div>
                <div class="lost-desc">{{ lost.description }}</div>
            </div>
        </div>
        <div v-if="!losts.length" class="empty">暂无信息</div>
        <div class="pager">
            <el-pagination background layout="prev, pager, next" :total="total"
                           :page-size="query.pageSize" :current-page.sync="query.pageNo" @current-change="loadData"/>
        </div>

        <el-dialog title="发布寻物/招领" :visible.sync="dialogVisible" width="520px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="类型" required>
                    <el-radio-group v-model="form.type">
                        <el-radio :label="0">寻物启事</el-radio>
                        <el-radio :label="1">失物招领</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="标题" required>
                    <el-input v-model="form.title" maxlength="100"/>
                </el-form-item>
                <el-form-item label="物品类型">
                    <el-select v-model="form.itemType" style="width:200px">
                        <el-option v-for="t in itemTypes" :key="t" :label="t" :value="t"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="地点">
                    <el-input v-model="form.location"/>
                </el-form-item>
                <el-form-item label="联系方式" required>
                    <el-input v-model="form.contact"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" :rows="3"/>
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
import { queryLostPage, addLost } from '../api/info'
import { getToken } from '../utils/auth'

export default {
    name: 'Losts',
    data() {
        return {
            itemTypes: ['证件', '数码', '钥匙', '钱包', '其他'],
            query: { pageNo: 1, pageSize: 8, type: null, keyword: null },
            losts: [],
            total: 0,
            dialogVisible: false,
            form: { type: 0, title: '', itemType: '其他', location: '', contact: '', description: '' }
        }
    },
    created() {
        this.loadData()
    },
    methods: {
        async loadData() {
            const data = await queryLostPage(this.query)
            this.losts = data.list || []
            this.total = Number(data.total || 0)
        },
        async handleAdd() {
            if (!this.form.title || !this.form.contact) return this.$message.warning('请填写完整信息')
            if (!getToken()) return this.$message.warning('请先登录')
            await addLost(this.form)
            this.$message.success('发布成功')
            this.dialogVisible = false
            this.form = { type: 0, title: '', itemType: '其他', location: '', contact: '', description: '' }
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

.lost-item {
    display: flex;
    gap: 16px;
}

.lost-cover {
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

.lost-title {
    font-size: 16px;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 8px;
}

.lost-meta {
    color: #888;
    font-size: 13px;
    margin: 8px 0;
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
}

.lost-desc {
    color: #555;
    font-size: 13px;
}

.empty {
    color: #999;
    text-align: center;
    padding: 30px;
}
</style>
