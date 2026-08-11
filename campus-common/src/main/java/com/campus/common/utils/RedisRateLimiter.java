package com.campus.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;

/**
 * Redis 接口限流器（对标苍穹外卖限流防刷思想，基于 Lua 脚本原子执行）
 * <p>
 * 固定窗口计数器算法：key 为限流维度（如 ip、userId、接口），窗口内计数超过阈值则拒绝。
 * 整个判断 + 计数 + 过期在 Lua 脚本中原子完成，避免并发超限。
 */
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua 脚本：window 秒内最多允许 max 次请求
     */
    private DefaultRedisScript<Long> limitScript;

    public RedisRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        limitScript = new DefaultRedisScript<>();
        limitScript.setLocation(new ClassPathResource("limit.lua"));
        limitScript.setResultType(Long.class);
    }

    /**
     * 尝试获取一个许可
     *
     * @param key  限流维度 key，例如 "limit:ip:192.168.1.1"
     * @param max  窗口内最大请求数
     * @param window 窗口时长（秒）
     * @return true 放行，false 拒绝
     */
    public boolean tryAcquire(String key, long max, long window) {
        Long result = stringRedisTemplate.execute(
                limitScript,
                Collections.singletonList(key),
                String.valueOf(max), String.valueOf(window)
        );
        return result != null && result == 1L;
    }
}
