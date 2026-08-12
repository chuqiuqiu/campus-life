<template>
    <div v-if="item" class="page-card">
        <div class="detail-head">
            <div class="cover-lg">📦</div>
            <div class="detail-info">
                <div class="title">{{ item.title }}</div>
                <div class="price">¥{{ item.price | fen2yuan }}</div>
                <div class="meta">
                    <el-tag size="small">{{ item.category }}</el-tag>
                    <el-tag size="small" type="success">{{ item.campusArea }}</el-tag>
                    <span>卖家：{{ item.sellerName || ('用户' + item.sellerId) }}</span>
                    <span>浏览：{{ item.viewCount }}</span>
                    <span>收藏：{{ item.favoriteCount }}</span>
                    <span>发布于：{{ formatTime(item.createTime) }}</span>
                </div>
                <div class="actions">
                    <el-button v-if="!favorited" type="warning" icon="el-icon-star-off" @click="handleFavorite">
                        收藏
                    </el-button>
                    <el-button v-else type="warning" plain icon="el-icon-star-on" @click="handleFavorite">
                        取消收藏
                    </el-button>
                </div>
            </div>
        </div>
        <el-divider/>
        <div class="desc">
            <div class="desc-title">物品描述</div>
            <p>{{ item.description || '暂无描述' }}</p>
        </div>
    </div>
</template>

<script>
import { getItemById, favoriteItem, unfavoriteItem, isFavorite } from '../api/item'
import { getToken } from '../utils/auth'

export default {
    name: 'ItemDetail',
    data() {
        return {
            item: null,
            favorited: false
        }
    },
    created() {
        this.load()
    },
    methods: {
        async load() {
            const id = this.$route.params.id
            this.item = await getItemById(id)
            if (getToken()) {
                try {
                    this.favorited = await isFavorite(id)
                } catch (e) { /* 未登录忽略 */ }
            }
        },
        async handleFavorite() {
            if (!getToken()) { this.$message.warning('请先登录'); return this.$router.push('/login') }
            const id = this.$route.params.id
            if (this.favorited) {
                await unfavoriteItem(id)
                this.$message.success('已取消收藏')
                this.favorited = false
            } else {
                await favoriteItem(id)
                this.$message.success('收藏成功')
                this.favorited = true
            }
            this.item = await getItemById(id)
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
}

.price {
    color: #f56c6c;
    font-size: 24px;
    font-weight: bold;
    margin: 10px 0;
}

.meta {
    color: #666;
    display: flex;
    gap: 14px;
    align-items: center;
    flex-wrap: wrap;
    margin-bottom: 16px;
}

.actions {
    margin-top: 10px;
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
