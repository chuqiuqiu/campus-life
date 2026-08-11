package com.campus.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis ZSet 成员值对象（成员 + 分数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZSetItem {
    /**
     * 成员值
     */
    private String value;
    /**
     * 分数
     */
    private Double score;
}
