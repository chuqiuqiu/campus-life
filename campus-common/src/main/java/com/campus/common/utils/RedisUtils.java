package com.campus.common.utils;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Redis 工具：提供 SCAN 迭代代替 KEYS 命令
 * <p>
 * 注意：KEYS 是 O(N) 阻塞命令，大 keyspace 下会导致 Redis 卡顿；SCAN 为游标迭代，不阻塞。
 */
public class RedisUtils {

    /**
     * 使用 SCAN 安全地按 pattern 收集 key（代替 keys()）
     *
     * @param template StringRedisTemplate
     * @param pattern  匹配模式，如 "cache:item:list:*"
     * @return 匹配的 key 集合
     */
    public static Set<String> scanKeys(StringRedisTemplate template, String pattern) {
        Set<String> keys = new HashSet<>();
        template.execute((RedisCallback<Object>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return null;
        });
        return keys;
    }
}
