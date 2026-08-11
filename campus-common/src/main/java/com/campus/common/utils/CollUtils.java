package com.campus.common.utils;

import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合工具（对标黑马商城 CollUtils）
 */
public class CollUtils {

    public static boolean isEmpty(Collection<?> coll) {
        return CollUtil.isEmpty(coll);
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return CollUtil.isNotEmpty(coll);
    }

    /**
     * 集合元素转换
     */
    public static <T, R> List<R> convert(Collection<T> coll, Function<T, R> func) {
        if (isEmpty(coll)) {
            return CollUtil.newArrayList();
        }
        return coll.stream().map(func).collect(Collectors.toList());
    }
}
