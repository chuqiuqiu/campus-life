package com.campus.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校园活动实体
 */
@Data
@TableName("tb_activity")
public class Activity {
    /**
     * 活动id
     */
    @TableId(type = IdType.AUTO)
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
     * 热度值（Redis ZSet 定期同步）
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
     * 创建时间
     */
    private LocalDateTime createTime;
}
