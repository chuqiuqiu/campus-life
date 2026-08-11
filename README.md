# 🎓 轻享校园生活服务平台（微服务版）

> 面向高校师生的轻量化校园综合服务微服务平台：**校园闲置互助 · 校园活动打卡 · 校园资讯 · 失物招领**
> 融合黑马商城（微服务架构）、黑马点评（Redis 高级场景）、苍穹外卖（业务规范）三大项目技术栈，无商城/支付/订单模块。

## ✨ 功能亮点

| 模块 | 核心功能 | 技术场景 |
|---|---|---|
| 闲置市场 | 发布/检索/详情/收藏 | **Redis 三级缓存**（Caffeine + Redis + DB，互斥锁防击穿、空值防穿透、随机 TTL 防雪崩）、收藏 Set 幂等、浏览量 Redis 计数异步落库、**Seata 分布式事务**（发布闲置跨服务加积分） |
| 校园活动 | 发布/报名/打卡/点赞/热度榜 | **Lua 脚本防超卖**、**Redis 全局唯一 ID**、点赞 Set 一赞、**热度排行榜 ZSet**、状态按时间动态计算、组织者/管理员删除权限 |
| 校园资讯 | 发布/点赞/浏览量 | 热点列表缓存、浏览量实时口径统一（DB 存量 + Redis 增量）、点赞去重 |
| 失物招领 | 发布/检索/状态流转 | Redis 全局唯一 ID 编号 |
| 用户中心 | 登录/JWT/会话/积分 | BCrypt 密码、**IP 限流防刷（Lua）**、Redis 会话缓存 + 滑动续期 |
| 网关 | 统一入口/鉴权/跨域 | JWT 鉴权（白名单接口粒度 + 登录态透传）、路由转发 |

## 🏗️ 架构

```
浏览器 ──> Nginx(:8088) ──> campus-gateway(:8080)
                              ├── user-service(:8081)     ── campus-user     库
                              ├── item-service(:8082)     ── campus-item     库（Seata 参与方）
                              ├── activity-service(:8083) ── campus-activity 库
                              └── info-service(:8084)     ── campus-info     库
          Nacos(:8848 注册/配置) · Redis(:6379 缓存/计数) · Seata(:8091 AT 分布式事务)
```

## 🛠️ 技术栈

SpringBoot 2.7.12 · SpringCloud 2021.0.3 · SpringCloud Alibaba 2021.0.4.0 · MyBatis-Plus 3.5.3.2 · MySQL 8.0 · Redis · Nacos · Seata(AT) · Sentinel · Gateway · OpenFeign · Vue2 + ElementUI · Nginx

## 🚀 快速开始

环境要求：JDK 17+（或 11）、Maven 3.8+、Node 14+，以及一台装有 Docker 的服务器（MySQL/Redis/Nacos/Seata/Nginx）。

```bash
# 1. 初始化数据库（sql/campus_init.sql，四个独立业务库 + Seata undo_log 表）
docker exec -i mysql mysql -uroot -p123 < sql/campus_init.sql

# 2. Nacos 配置中心导入 docs/nacos/ 下的三个共享配置（shared-log / shared-swagger / shared-seata）

# 3. 打包启动（顺序：user → item → activity → info → gateway）
mvn clean package -DskipTests
java -jar user-service/target/user-service.jar
java -jar item-service/target/item-service.jar
java -jar activity-service/target/activity-service.jar
java -jar info-service/target/info-service.jar
java -jar campus-gateway/target/campus-gateway.jar

# 4. 前端（nginx/html/campus-life-web）
npm install && npm run serve        # 开发模式 :8088，代理 /api 到网关
npm run build                        # 生产构建，用 nginx/conf/nginx.conf 部署
```

> 详细步骤见 [docs/部署文档.md](docs/部署文档.md)（含常见问题排查与踩坑记录）。

### 预置账号

| 手机号 | 密码 | 说明 |
|---|---|---|
| 13800000001 | 123 | 普通用户（张三） |
| 13800000002 | 123 | 普通用户（李四） |
| 13800000003 | 123456 | 管理员（可删除活动） |

## 📁 目录结构

```
campus-life/
├── campus-common/          # 通用模块：统一返回/异常/工具/RedisIdWorker/限流器/公共配置
├── campus-gateway/         # 网关：路由/JWT鉴权/跨域
├── user-service/           # 用户服务
├── item-service/           # 闲置服务（Seata 全局事务发起方）
├── activity-service/       # 活动服务（Redis 核心场景）
├── info-service/           # 资讯失物服务
├── sql/                    # 数据库初始化
├── nginx/                  # 前端源码 + Nginx 配置
└── docs/                   # 部署文档与 Nacos 配置
```

## 🔐 配置说明

- 中间件地址/密码在 `application-dev.yaml`（`hm.db.host/pw`、`hm.redis.host/pw`），按需修改
- **JWT 密钥**通过环境变量 `JWT_SECRET` 注入，请勿使用默认值（见 `user-service` / `campus-gateway` 的 `application.yaml`）
- 数据库 `campus-user`、`campus-item` 库含 Seata AT 模式要求的 `undo_log` 表

## 📜 License

[MIT](LICENSE)
