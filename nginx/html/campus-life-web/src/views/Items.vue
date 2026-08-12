<template>
    <div>
        <div class="page-card">
            <div class="toolbar">
                <el-input v-model="query.keyword" placeholder="搜索闲置" clearable style="width:220px"
                          @keyup.enter.native="loadData" @clear="loadData"/>
                <el-select v-model="query.category" placeholder="全部分类" clearable style="width:140px" @change="loadData">
                    <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
                </el-select>
                <el-select v-model="query.campusArea" placeholder="全部校区" clearable style="width:140px" @change="loadData">
                    <el-option v-for="a in areas" :key="a" :label="a" :value="a"/>
                </el-select>
                <el-button type="primary" @click="loadData">查询</el-button>
                <el-button type="success" @click="dialogVisible = true">发布闲置</el-button>
            </div>
        </div>
        <div class="item-grid">
            <div v-for="item in items" :key="item.id" class="item-card" @click="$router.push('/items/' + item.id)">
                <div class="cover">📦</div>
                <div class="body">
                    <div class="title">{{ item.title }}</div>
                    <div class="price">¥{{ item.price | fen2yuan }}</div>
                    <div class="meta">
                        {{ item.campusArea }} · {{ item.category }} · {{ item.viewCount }}浏览 · {{ item.favoriteCount }}收藏
                    </div>
                </div>
            </div>
        </div>
        <div v-if="!items.length" class="empty">暂无闲置，快来发布第一件吧～</div>
        <div class="pager">
            <el-pagination background layout="prev, pager, next" :total="total"
                           :page-size="query.pageSize" :current-page.sync="query.pageNo" @current-change="loadData"/>
        </div>

        <!-- 发布闲置弹窗 -->
        <el-dialog title="发布闲置" :visible.sync="dialogVisible" width="520px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="标题" required>
                    <el-input v-model="form.title" maxlength="100"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" :rows="3"/>
                </el-form-item>
                <el-form-item label="价格" required>
                    <el-input-number v-model="form.price" :min="0" :step="100" style="width:200px"/>
                    <span style="color:#999;margin-left:8px">单位：分</span>
                </el-form-item>
                <el-form-item label="分类" required>
                    <el-select v-model="form.category" style="width:200px">
                        <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="校区">
                    <el-select v-model="form.campusArea" style="width:200px">
                        <el-option v-for="a in areas" :key="a" :label="a" :value="a"/>
                    </el-select>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleAdd">发布（+10积分）</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import { queryItemPage, addItem } from '../api/item'
import { getToken } from '../utils/auth'

export default {
    name: 'Items',
    data() {
        return {
            categories: ['教材', '数码', '生活', '运动', '其他'],
            areas: ['东校区', '西校区', '南校区', '北校区'],
            query: { pageNo: 1, pageSize: 8, category: null, keyword: null, campusArea: null },
            items: [],
            total: 0,
            dialogVisible: false,
            form: { title: '', description: '', price: 100, category: '其他', campusArea: '东校区' }
        }
    },
    created() {
        this.loadData()
    },
    methods: {
        async loadData() {
            const data = await queryItemPage(this.query)
            this.items = data.list || []
            this.total = Number(data.total || 0)
        },
        async handleAdd() {
            if (!this.form.title) return this.$message.warning('请输入标题')
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            await addItem(this.form)
            this.$message.success('发布成功，已获得 10 积分（Seata 分布式事务）')
            this.dialogVisible = false
            this.form = { title: '', description: '', price: 100, category: '其他', campusArea: '东校区' }
            this.loadData()
        }
    }
}
</script>
