-- =====================================================================
-- 轻享校园生活服务平台（微服务版）数据库初始化脚本
-- 环境：192.168.100.128 虚拟机 Docker MySQL 8.0
-- 说明：一个业务一个库，对应一个微服务，分库分治（对标黑马商城）
-- 预置账号：13800000001/123（张三）、13800000002/123（李四）、13800000003/123456（admin）
-- 注意：campus-user、campus-item 库需建 Seata AT 模式要求的 undo_log 表（见文末）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 用户库 campus-user（user-service）
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `campus_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus_user`;

DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户id',
    `phone`       VARCHAR(11)  NOT NULL COMMENT '手机号（登录账号）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `nick_name`   VARCHAR(50)  NOT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `gender`      TINYINT      DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `college`     VARCHAR(50)  DEFAULT NULL COMMENT '所在学院',
    `credit`      INT          DEFAULT 0 COMMENT '校园积分（发布闲置/参与活动获得）',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB AUTO_INCREMENT = 1000 COMMENT = '用户表';

INSERT INTO `tb_user` (`id`, `phone`, `password`, `nick_name`, `avatar`, `gender`, `college`, `credit`, `status`) VALUES
(1000, '13800000001', '$2a$10$362KAjOppLvangloDyLSh.xaSkeyieqA5tZOaEGYu6yA59xfKjuOO', '张三', 'https://i.imgur.com/avatar1.png', 1, '计算机学院', 120, 1),
(1001, '13800000002', '$2a$10$2Za5f.gATuUxsHQ0wbQzRu0azBC8rmj69i5QpoVc4VpX8xNbtnpIK', '李四', 'https://i.imgur.com/avatar2.png', 2, '软件学院', 60, 1),
(1002, '13800000003', '$2a$10$F.1YBsOKcwpAQbiVS08U6.jL3DeuYN0rhPediyTh6bChKUzHEKbgy', '管理员', 'https://i.imgur.com/avatar3.png', 0, '信息工程学院', 200, 1);

-- ---------------------------------------------------------------------
-- 2. 校园闲置库 campus-item（item-service）
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `campus-item` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus-item`;

DROP TABLE IF EXISTS `tb_idle_item`;
CREATE TABLE `tb_idle_item` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '闲置id',
    `title`          VARCHAR(100)  NOT NULL COMMENT '标题',
    `description`    TEXT          COMMENT '描述',
    `images`         VARCHAR(1000) DEFAULT NULL COMMENT '图片地址，逗号分隔',
    `price`          INT           DEFAULT 0 COMMENT '价格（单位：分）',
    `category`       VARCHAR(20)   NOT NULL COMMENT '分类：教材/数码/生活/运动/其他',
    `campus_area`    VARCHAR(50)   DEFAULT NULL COMMENT '所在校区',
    `seller_id`      BIGINT        NOT NULL COMMENT '发布者id',
    `status`         TINYINT       DEFAULT 0 COMMENT '状态：0在售 1已售出 2已下架',
    `view_count`     INT           DEFAULT 0 COMMENT '浏览量',
    `favorite_count` INT           DEFAULT 0 COMMENT '收藏数',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_seller` (`seller_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 2000 COMMENT = '校园闲置表';

INSERT INTO `tb_idle_item` (`id`, `title`, `description`, `images`, `price`, `category`, `campus_area`, `seller_id`, `status`, `view_count`, `favorite_count`) VALUES
(2000, '高等数学上册 教材 9成新', '大二用过的教材，无笔记，9成新，需要可小刀', 'https://i.imgur.com/book1.png,https://i.imgur.com/book2.png', 1500, '教材', '东校区', 1000, 0, 56, 8),
(2001, '罗技无线鼠标 手感好', '罗技M590，双模连接，几乎全新，毕业出', 'https://i.imgur.com/mouse1.png', 4500, '数码', '东校区', 1001, 0, 120, 23),
(2002, '考研英语词汇书 附笔记', '红宝书，附个人笔记，可送真题', 'https://i.imgur.com/word1.png', 800, '教材', '西校区', 1001, 0, 32, 5),
(2003, '宿舍小台灯 护眼', '充电式护眼台灯，三档调光', 'https://i.imgur.com/lamp1.png', 2500, '生活', '南校区', 1000, 1, 88, 12),
(2004, '羽毛球拍 尤尼克斯', '尤尼克斯NR7000，含球拍袋', 'https://i.imgur.com/racket1.png', 8000, '运动', '西校区', 1002, 0, 45, 9);

DROP TABLE IF EXISTS `tb_idle_favorite`;
CREATE TABLE `tb_idle_favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`     BIGINT   NOT NULL COMMENT '闲置id',
    `user_id`     BIGINT   NOT NULL COMMENT '收藏用户id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_user` (`item_id`, `user_id`)
) ENGINE = InnoDB COMMENT = '闲置收藏表';

-- ---------------------------------------------------------------------
-- 3. 校园活动库 campus-activity（activity-service）
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `campus-activity` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus-activity`;

DROP TABLE IF EXISTS `tb_activity`;
CREATE TABLE `tb_activity` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动id',
    `title`        VARCHAR(100) NOT NULL COMMENT '活动标题',
    `description`  TEXT         COMMENT '活动描述',
    `image`        VARCHAR(255) DEFAULT NULL COMMENT '活动封面',
    `location`     VARCHAR(100) DEFAULT NULL COMMENT '活动地点',
    `start_time`   DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`     DATETIME     NOT NULL COMMENT '结束时间',
    `max_people`   INT          DEFAULT 100 COMMENT '报名人数上限',
    `signed_count` INT          DEFAULT 0 COMMENT '已报名人数',
    `like_count`   INT          DEFAULT 0 COMMENT '点赞数',
    `heat`         DOUBLE       DEFAULT 0 COMMENT '热度值（由Redis ZSet定期同步）',
    `status`       TINYINT      DEFAULT 0 COMMENT '状态：0报名中 1进行中 2已结束',
    `organizer_id` BIGINT       NOT NULL COMMENT '组织者id',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_organizer` (`organizer_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 3000 COMMENT = '校园活动表';

INSERT INTO `tb_activity` (`id`, `title`, `description`, `image`, `location`, `start_time`, `end_time`, `max_people`, `signed_count`, `like_count`, `heat`, `status`, `organizer_id`) VALUES
(3000, '校园春季运动会', '一年一度的校园运动会，包含田径、球类等多个项目，欢迎大家踊跃报名！', 'https://i.imgur.com/sports1.png', '东校区田径场', '2024-04-20 09:00:00', '2024-04-21 18:00:00', 200, 156, 320, 98.5, 0, 1002),
(3001, '编程马拉松 Hackathon', '24小时编程马拉松，主题：智慧校园。优胜队伍有丰厚奖品！', 'https://i.imgur.com/hack1.png', '图书馆报告厅', '2024-05-10 08:00:00', '2024-05-11 08:00:00', 80, 63, 210, 76.3, 0, 1000),
(3002, '校园二手集市', '毕业季二手物品交易集市，支持以物换物，欢迎来淘好物！', 'https://i.imgur.com/market1.png', '中心广场', '2024-05-18 10:00:00', '2024-05-18 17:00:00', 150, 98, 156, 55.8, 0, 1001),
(3003, '社团招新嘉年华', '百团大战，各社团集中招新，现场有才艺表演和小礼品。', 'https://i.imgur.com/club1.png', '大学生活动中心', '2024-04-01 09:00:00', '2024-04-02 16:00:00', 300, 243, 402, 120.6, 1, 1002);

DROP TABLE IF EXISTS `tb_activity_signup`;
CREATE TABLE `tb_activity_signup` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sign_no`        VARCHAR(32) NOT NULL COMMENT '报名凭证编号（Redis全局唯一ID）',
    `activity_id`    BIGINT      NOT NULL COMMENT '活动id',
    `user_id`        BIGINT      NOT NULL COMMENT '报名用户id',
    `sign_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `checkin_time`   DATETIME    DEFAULT NULL COMMENT '打卡时间',
    `checkin_status` TINYINT     DEFAULT 0 COMMENT '打卡状态：0未打卡 1已打卡',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
    KEY `idx_sign_no` (`sign_no`)
) ENGINE = InnoDB COMMENT = '活动报名表';

-- ---------------------------------------------------------------------
-- 4. 校园资讯失物库 campus-info（info-service）
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `campus-info` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus-info`;

DROP TABLE IF EXISTS `tb_info`;
CREATE TABLE `tb_info` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资讯id',
    `title`       VARCHAR(100) NOT NULL COMMENT '资讯标题',
    `content`     TEXT         COMMENT '资讯内容',
    `cover`       VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    `category`    VARCHAR(20)  DEFAULT '校园资讯' COMMENT '分类：校园资讯/通知公告/讲座活动',
    `author_id`   BIGINT       NOT NULL COMMENT '作者id',
    `view_count`  INT          DEFAULT 0 COMMENT '浏览量',
    `like_count`  INT          DEFAULT 0 COMMENT '点赞数',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态：0草稿 1已发布',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
) ENGINE = InnoDB AUTO_INCREMENT = 4000 COMMENT = '校园资讯表';

INSERT INTO `tb_info` (`id`, `title`, `content`, `cover`, `category`, `author_id`, `view_count`, `like_count`, `status`) VALUES
(4000, '图书馆暑期开放时间调整通知', '暑期图书馆开放时间调整为 8:00-20:00，请同学们合理安排自习时间。', 'https://i.imgur.com/lib1.png', '通知公告', 1002, 320, 45, 1),
(4001, '2024年校园歌手大赛开始报名', '校园歌手大赛海选报名通道开启，报名截止 5 月 20 日，欢迎大家一展歌喉！', 'https://i.imgur.com/sing1.png', '校园资讯', 1000, 512, 98, 1),
(4002, 'AI 前沿技术专题讲座', '特邀企业专家开展《大模型时代的软件工程》专题讲座，地点：报告厅。', 'https://i.imgur.com/ai1.png', '讲座活动', 1002, 268, 56, 1);

DROP TABLE IF EXISTS `tb_lost`;
CREATE TABLE `tb_lost` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lost_no`     VARCHAR(32)  NOT NULL COMMENT '招领/寻物编号（Redis全局唯一ID）',
    `title`       VARCHAR(100) NOT NULL COMMENT '标题',
    `description` TEXT         COMMENT '详细描述',
    `image`       VARCHAR(255) DEFAULT NULL COMMENT '图片',
    `item_type`   VARCHAR(20)  DEFAULT '其他' COMMENT '物品类型：证件/数码/钥匙/钱包/其他',
    `location`    VARCHAR(100) DEFAULT NULL COMMENT '丢失/拾到地点',
    `contact`     VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    `type`        TINYINT      DEFAULT 0 COMMENT '类型：0寻物 1招领',
    `status`      TINYINT      DEFAULT 0 COMMENT '状态：0寻找中 1已找到/已归还',
    `user_id`     BIGINT       NOT NULL COMMENT '发布者id',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 5000 COMMENT = '失物招领表';

INSERT INTO `tb_lost` (`id`, `lost_no`, `title`, `description`, `image`, `item_type`, `location`, `contact`, `type`, `status`, `user_id`) VALUES
(5000, 'L2024041500001', '丢失校园卡一张', '校园卡，姓名张伟，学号2021012345，捡到请联系我，谢谢！', 'https://i.imgur.com/card1.png', '证件', '二食堂', '微信:zhangwei', 0, 0, 1000),
(5001, 'L2024041600002', '捡到黑色钱包', '在三教 302 教室捡到黑色钱包一个，内有少量现金，请失主认领。', 'https://i.imgur.com/wallet1.png', '钱包', '三教302', '电话:13800000002', 1, 0, 1001),
(5002, 'L2024041700003', '遗失蓝牙耳机', '白色 AirPods 一代，左耳遗失，在操场跑步时丢失。', 'https://i.imgur.com/airpod1.png', '数码', '西操场', 'QQ:12345678', 0, 1, 1002);

-- ---------------------------------------------------------------------
-- Seata AT 模式分布式事务依赖：undo_log 表（参与全局事务的库都必须创建）
-- 参考 Seata 官方 SQL：https://github.com/seata/seata/blob/develop/script/client/at/db/mysql.sql
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `campus-user`.`undo_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `branch_id`     BIGINT       NOT NULL,
    `xid`           VARCHAR(128) NOT NULL,
    `context`       VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB     NOT NULL,
    `log_status`    INT          NOT NULL,
    `log_created`   DATETIME     NOT NULL,
    `log_modified`  DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT 模式回滚日志表';

CREATE TABLE IF NOT EXISTS `campus-item`.`undo_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `branch_id`     BIGINT       NOT NULL,
    `xid`           VARCHAR(128) NOT NULL,
    `context`       VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB     NOT NULL,
    `log_status`    INT          NOT NULL,
    `log_created`   DATETIME     NOT NULL,
    `log_modified`  DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT 模式回滚日志表';
