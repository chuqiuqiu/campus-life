package com.campus.item.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闲置信息展示 VO
 */
@Data
public class ItemVO {
    /**
     * 闲置id
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String description;
    /**
     * 图片地址，逗号分隔
     */
    private String images;
    /**
     * 价格（单位：分）
     */
    private Integer price;
    /**
     * 分类
     */
    private String category;
    /**
     * 所在校区
     */
    private String campusArea;
    /**
     * 发布者id
     */
    private Long sellerId;
    /**
     * 发布者昵称（Feign 调用 user-service 填充）
     */
    private String sellerName;
    /**
     * 状态：0在售 1已售出 2已下架
     */
    private Integer status;
    /**
     * 浏览量（DB 存量 + Redis 增量）
     */
    private Integer viewCount;
    /**
     * 收藏数
     */
    private Integer favoriteCount;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
