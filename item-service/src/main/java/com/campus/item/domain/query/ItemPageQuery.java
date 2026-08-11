package com.campus.item.domain.query;

import com.campus.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 闲置分页查询条件（扩展通用分页：分类/关键词/校区筛选）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ItemPageQuery extends PageQuery {
    /**
     * 分类：教材/数码/生活/运动/其他
     */
    private String category;
    /**
     * 关键词（模糊匹配标题）
     */
    private String keyword;
    /**
     * 校区
     */
    private String campusArea;
}
