package com.campus.activity.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 发布活动表单
 */
@Data
public class ActivityFormDTO {
    /**
     * 活动标题
     */
    @NotBlank(message = "活动标题不能为空")
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
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    /**
     * 报名人数上限
     */
    @NotNull(message = "报名上限不能为空")
    private Integer maxPeople;
}
