package com.campus.activity.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动详情 VO
 */
@Data
public class ActivityVO {
    /**
     * 活动id
     */
    private Long id;
    /**
     * 活动标题
     */
    private String title;
    /**
     * 活动描述
     */
    private String description;
    /**
     * 活动封面
     */
    private String image;
    /**
     * 活动地点
     */
    private String location;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 报名人数上限
     */
    private Integer maxPeople;
    /**
     * 已报名人数
     */
    private Integer signedCount;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 热度值
     */
    private Double heat;
    /**
     * 状态：0报名中 1进行中 2已结束
     */
    private Integer status;
    /**
     * 组织者id
     */
    private Long organizerId;
    /**
     * 组织者昵称（Feign 填充）
     */
    private String organizerName;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 当前用户是否已点赞
     */
    private Boolean liked;
    /**
     * 当前用户是否已报名
     */
    private Boolean signedUp;
    /**
     * 当前用户是否可管理该活动（组织者或管理员）
     */
    private Boolean isOwner;
}
