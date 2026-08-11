package com.campus.common.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页返回结果（对标黑马商城 PageDTO）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private Long total;
    private List<T> list;

    /**
     * 将 MyBatis-Plus 分页对象转换为 PageDTO
     */
    public static <PO, VO> PageDTO<VO> of(Page<PO> p, Function<PO, VO> convertor) {
        List<VO> list = p.getRecords().stream().map(convertor).collect(Collectors.toList());
        return new PageDTO<>(p.getTotal(), list);
    }

    /**
     * 直接构造
     */
    public static <T> PageDTO<T> of(Long total, List<T> list) {
        return new PageDTO<>(total, list);
    }
}
