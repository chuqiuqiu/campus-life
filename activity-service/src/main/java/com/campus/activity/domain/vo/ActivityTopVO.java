package com.campus.activity.domain.vo;

import lombok.Data;

/**
 * 热度排行榜 VO
 */
@Data
public class ActivityTopVO {
    /**
     * 活动id
     */
    private Long id;
    /**
     * 活动标题
     */
    private String title;
    /**
     * 活动封面
     */
    private String image;
    /**
     * 热度值
     */
    private Double heat;
    /**
     * 排名
     */
    private Integer rank;
}
