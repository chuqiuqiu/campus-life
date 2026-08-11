package com.campus.common.domain;

import lombok.Data;

/**
 * 分页查询基类：前端传入 pageNo/pageSize/sortBy/sortAsc（对标黑马商城 PageQuery）
 */
@Data
public class PageQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 5;
    private String sortBy;
    private Boolean isAsc;

    public Integer getPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 5 : pageSize;
    }
}
