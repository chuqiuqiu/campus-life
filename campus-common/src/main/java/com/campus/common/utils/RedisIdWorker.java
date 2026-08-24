package com.campus.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Redis 全局唯一 ID 生成器（对标黑马点评 RedisIdWorker）
 * <p>
 * 方案：时间戳(31bit) + 序列号(32bit)，序列号来自 Redis 自增，支持按日期重置。
 * 生成规则：<pre>
 *   | 符号位(1) | 时间戳(31) | 序列号(32) |
 * </pre>
 * 时间戳以 2023-01-01 为起始纪元，序列号通过 INCRBY key 1 获取，每天一个 key 自然重置。
 */
@Component
public class RedisIdWorker {

    /**
     * 起始时间戳（2023-01-01 00:00:00）
     */
    private static final long BEGIN_TIMESTAMP = 1672502400L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成全局唯一 ID
     *
     * @param keyPrefix 业务前缀，例如 item、activity、info，用于隔离自增序列
     * @return 全局唯一 long 型 ID
     */
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2.生成序列号（按天自增，每天重置，防止溢出）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String key = "icr:" + keyPrefix + ":" + date;
        long count = stringRedisTemplate.opsForValue().increment(key);
        // 3.设置 TTL（3 天），避免历史日期 key 无限积累（活跃 key 到期后自然清理）
        stringRedisTemplate.expire(key, Duration.ofDays(3));
        // 4.拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
