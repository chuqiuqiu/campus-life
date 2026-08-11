package com.campus.common.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bean 拷贝工具（对标黑马商城 BeanUtils，基于 hutool）
 */
public class BeanUtils {

    /**
     * 单个对象拷贝
     */
    public static <T> T copyBean(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetType);
    }

    /**
     * 列表拷贝
     */
    public static <T> List<T> copyList(List<?> source, Class<T> targetType) {
        if (CollUtil.isEmpty(source)) {
            return CollUtil.newArrayList();
        }
        return source.stream()
                .map(item -> BeanUtil.copyProperties(item, targetType))
                .collect(Collectors.toList());
    }

    /**
     * 拷贝属性到已有对象
     */
    public static void copyProperties(Object source, Object target) {
        BeanUtil.copyProperties(source, target);
    }

    /**
     * 将 Set 拷贝为 List
     */
    public static <T> List<T> copySetToList(Set<?> source, Class<T> targetType) {
        if (CollUtil.isEmpty(source)) {
            return CollUtil.newArrayList();
        }
        return source.stream()
                .map(item -> BeanUtil.copyProperties(item, targetType))
                .collect(Collectors.toList());
    }
}
