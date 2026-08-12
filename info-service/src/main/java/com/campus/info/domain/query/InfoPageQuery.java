package com.campus.info.domain.query;

import com.campus.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资讯分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfoPageQuery extends PageQuery {
    /**
     * 分类：校园资讯/通知公告/讲座活动
     */
    private String category;
    /**
     * 关键词（模糊匹配标题）
     */
    private String keyword;
    /**
     * 发布者id（"我的发布"筛选用，为空则查全部）
     */
    private Long userId;
}
