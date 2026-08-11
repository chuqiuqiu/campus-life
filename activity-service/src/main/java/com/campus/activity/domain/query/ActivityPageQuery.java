package com.campus.activity.domain.query;

import com.campus.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 活动分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ActivityPageQuery extends PageQuery {
    /**
     * 活动状态：0报名中 1进行中 2已结束，为空查全部
     */
    private Integer status;
    /**
     * 关键词（模糊匹配标题）
     */
    private String keyword;
}
