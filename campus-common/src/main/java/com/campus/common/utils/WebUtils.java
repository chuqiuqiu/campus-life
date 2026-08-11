package com.campus.common.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web 工具类（对标黑马商城 WebUtils）
 */
public class WebUtils {
    /**
     * 获取当前请求
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 获取当前响应
     */
    public static HttpServletResponse getResponse() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getResponse();
    }

    /**
     * 获取请求头中的用户id（网关透传的 user-info 头）
     */
    public static Long getUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String userInfo = request.getHeader("user-info");
        if (StrUtil.isBlank(userInfo)) {
            return null;
        }
        try {
            return Long.valueOf(userInfo);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
