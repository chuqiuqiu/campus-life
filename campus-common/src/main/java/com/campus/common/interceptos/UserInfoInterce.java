package com.campus.common.interceptos;

import com.campus.common.utils.UserContext;
import com.campus.common.utils.WebUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户信息拦截器：解析网关透传的 user-info 请求头，写入 ThreadLocal（对标黑马商城 UserInfoInterce）
 */
public class UserInfoInterce implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = WebUtils.getUserId();
        if (userId != null) {
            // 存在登录用户，保存用户信息
            UserContext.setUser(userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理用户信息
        UserContext.removeUser();
    }
}
