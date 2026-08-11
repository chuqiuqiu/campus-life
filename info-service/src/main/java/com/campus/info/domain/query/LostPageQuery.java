package com.campus.info.domain.query;

import com.campus.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 失物招领分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LostPageQuery extends PageQuery {
    /**
     * 类型：0寻物 1招领
     */
    private Integer type;
    /**
     * 物品类型
     */
    private String itemType;
    /**
     * 关键词（模糊匹配标题）
     */
    private String keyword;
}
