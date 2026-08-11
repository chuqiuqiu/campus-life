package com.campus.gateway.filters;

import com.campus.common.exception.UnauthorizedException;
import com.campus.gateway.config.AuthProperties;
import com.campus.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 全局鉴权过滤器（对标黑马商城 AuthGlobalFilter）：
 * 1. 白名单路径直接放行
 * 2. 校验 JWT，将 userId 写入 user-info 请求头透传给下游服务
 */
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;

    /**
     * Ant 风格路径匹配器
     */
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        boolean isExclude = isExcludePath(path);
        // 2.获取 token（白名单路径也尝试解析：登录用户浏览公开页面时同样透传身份，用于点赞/收藏状态判断）
        String token = request.getHeaders().getFirst("authorization");
        Long userId = null;
        if (token != null) {
            try {
                userId = jwtTool.parseToken(token);
            } catch (UnauthorizedException e) {
                // 白名单路径 token 无效按未登录处理（浏览仍公开）；非白名单路径直接 401
                if (!isExclude) {
                    return unauthorized(exchange.getResponse(), e.getMessage());
                }
            }
        }
        // 3.非白名单路径必须登录
        if (!isExclude && userId == null) {
            return unauthorized(exchange.getResponse(), "未登录");
        }
        // 4.登录用户将 userId 写入请求头透传下游（服务端 UserContext 由此获取当前用户）
        if (userId != null) {
            ServerHttpRequest newRequest = request.mutate()
                    .header("user-info", userId.toString())
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 判断是否白名单路径
     */
    private boolean isExcludePath(String path) {
        List<String> excludePaths = authProperties.getExcludePaths();
        if (excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        for (String excludePath : excludePaths) {
            if (antPathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}").getBytes())));
    }
}
