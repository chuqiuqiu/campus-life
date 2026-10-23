<template>
    <div>
        <div class="banner">
            <div class="banner-title">🎓 轻享校园 · 让校园生活更简单</div>
            <div class="banner-sub">闲置互助 · 活动打卡 · 校园资讯 · 失物招领</div>
        </div>
        <el-row :gutter="20">
            <el-col :span="16">
                <!-- 闲置精选 -->
                <div class="page-card">
                    <div class="section-title">
                        <span>🔥 闲置精选</span>
                        <el-button type="text" @click="$router.push('/items')">查看更多 &gt;</el-button>
                    </div>
                    <div class="item-grid">
                        <div v-for="item in items" :key="item.id" class="item-card" @click="goItem(item.id)">
                            <div class="cover">
                                <span>📦</span>
                                <img v-if="item.images" class="cover-img" :src="item.images.split(',')[0]" alt=""
                                     @error="$event.target.style.display='none'"/>
                            </div>
                            <div class="body">
                                <div class="title">{{ item.title }}</div>
                                <div class="price">¥{{ item.price | fen2yuan }}</div>
                                <div class="meta">{{ item.campusArea }} · {{ item.viewCount }}浏览</div>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- 最新资讯 -->
                <div class="page-card">
                    <div class="section-title">
                        <span>📰 最新资讯</span>
                        <el-button type="text" @click="$router.push('/infos')">查看更多 &gt;</el-button>
                    </div>
                    <el-table :data="infos" @row-click="row => $router.push('/infos/' + row.id)">
                        <el-table-column prop="title" label="标题"/>
                        <el-table-column prop="category" label="分类" width="120"/>
                        <el-table-column prop="viewCount" label="浏览" width="80"/>
                        <el-table-column label="发布时间" width="170">
                            <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
                        </el-table-column>
                    </el-table>
                </div>
            </el-col>
            <el-col :span="8">
                <!-- 活动热度榜 -->
                <div class="page-card">
                    <div class="section-title">
                        <span>🏆 活动热度榜</span>
                        <el-button type="text" @click="$router.push('/activities')">更多 &gt;</el-button>
                    </div>
                    <div v-for="act in tops" :key="act.id" class="rank-item" @click="$router.push('/activities/' + act.id)">
                        <span class="rank-no" :class="'rank-' + act.rank">{{ act.rank }}</span>
                        <span class="rank-title">{{ act.title }}</span>
                        <span class="rank-heat">🔥 {{ act.heat }}</span>
                    </div>
                    <div v-if="!tops.length" class="empty">暂无数据</div>
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script>
import { queryItemPage } from '../api/item'
import { topActivities } from '../api/activity'
import { queryInfoPage } from '../api/info'

export default {
    name: 'Home',
    data() {
        return {
            items: [],
            infos: [],
            tops: []
        }
    },
    created() {
        this.loadData()
    },
    methods: {
        async loadData() {
            const [items, infos, tops] = await Promise.all([
                queryItemPage({ pageNo: 1, pageSize: 4 }),
                queryInfoPage({ pageNo: 1, pageSize: 5 }),
                topActivities(5)
            ])
            this.items = items.list || []
            this.infos = infos.list || []
            this.tops = tops || []
        },
        goItem(id) {
            this.$router.push('/items/' + id)
        },
        formatTime(t) {
            return t ? t.replace('T', ' ').substring(0, 16) : ''
        }
    }
}
</script>

<style scoped>
.banner {
    background: linear-gradient(135deg, #409EFF, #7cc4ff);
    border-radius: 10px;
    color: #fff;
    padding: 40px 30px;
    margin-bottom: 20px;
}

.banner-title {
    font-size: 26px;
    font-weight: bold;
}

.banner-sub {
    margin-top: 10px;
    opacity: .9;
}

.section-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 17px;
    font-weight: bold;
    margin-bottom: 16px;
}

.rank-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
}

.rank-no {
    width: 22px;
    height: 22px;
    border-radius: 4px;
    background: #c0c4cc;
    color: #fff;
    text-align: center;
    line-height: 22px;
    font-size: 12px;
    margin-right: 10px;
    flex-shrink: 0;
}

.rank-1 { background: #f56c6c; }
.rank-2 { background: #e6a23c; }
.rank-3 { background: #67c23a; }

.rank-title {
    flex: 1;
    font-size: 14px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.rank-heat {
    color: #f56c6c;
    font-size: 12px;
    flex-shrink: 0;
}

.empty {
    color: #999;
    text-align: center;
    padding: 20px;
}
</style>
