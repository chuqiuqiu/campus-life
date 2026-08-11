package com.campus.common.utils;

import cn.hutool.core.convert.Convert;

/**
 * 类型转换工具（基于 hutool Convert）
 */
public class ConvertUtils {

    /**
     * 转换为 Long，失败返回 null
     */
    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Convert.toLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换为 Integer，失败返回 null
     */
    public static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Convert.toInt(value);
        } catch (Exception e) {
            return null;
        }
    }
}
