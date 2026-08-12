package com.campus.item.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.domain.PageDTO;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.BizIllegalException;
import com.campus.common.exception.ForbiddenException;
import com.campus.common.domain.R;
import com.campus.common.utils.RedisUtils;
import com.campus.item.client.UserClient;
import com.campus.item.client.dto.SellerVO;
import com.campus.item.domain.dto.ItemFormDTO;
import com.campus.item.domain.po.Item;
import com.campus.item.domain.query.ItemPageQuery;
import com.campus.item.domain.vo.ItemVO;
import com.campus.item.mapper.ItemMapper;
import com.campus.item.service.IItemService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 闲置服务实现（核心：三级缓存方案解决缓存三大问题 + Seata 分布式事务）
 * <p>
 * 缓存架构：
 * <pre>
 *   一级缓存 Caffeine（本地，5分钟）   --&gt; 抗高并发
 *   二级缓存 Redis（30分钟+随机TTL）   --&gt; 集群共享
 *   三级缓存 MySQL（兜底）
 *   穿透：查库为空时缓存空值（短TTL）
 *   击穿：Redis SETNX 互斥锁，只允许一个线程回源
 *   雪崩：TTL 加随机偏移，避免同时过期
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    /**
     * 闲置详情 Redis 缓存 key 前缀
     */
    private static final String CACHE_ITEM_KEY = "cache:item:";
    /**
     * 首页列表缓存 key 前缀
     */
    private static final String CACHE_ITEM_LIST_KEY = "cache:item:list:";
    /**
     * 互斥锁 key 前缀
     */
    private static final String LOCK_ITEM_KEY = "lock:item:";
    /**
     * 浏览量 Redis 增量 key 前缀
     */
    private static final String VIEW_ITEM_KEY = "view:item:";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    /**
     * 空值缓存 TTL（防穿透）
     */
    private static final Duration CACHE_NULL_TTL = Duration.ofMinutes(2);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    /**
     * 发布闲置奖励积分
     */
    private static final int PUBLISH_CREDIT = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final UserClient userClient;

    /**
     * 一级缓存：Caffeine 本地缓存（过期 5 分钟，容量 1000）
     */
    private final Cache<Long, Item> caffeineCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 发布闲置：Seata 全局事务，保证【插入闲置 + 跨服务加积分】的分布式一致性
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Long addItem(ItemFormDTO dto, Long userId) {
        // 1.保存闲置
        Item item = BeanUtil.copyProperties(dto, Item.class);
        item.setSellerId(userId);
        item.setStatus(0);
        item.setViewCount(0);
        item.setFavoriteCount(0);
        save(item);
        log.info("闲置发布成功：id = {}，开始跨服务增加积分（Seata 全局事务）", item.getId());
        // 发布后清理列表缓存，保证新发布立即可见
        evictCache(item.getId());
        // 2.跨服务调用 user-service 增加积分（Seata AT 参与方，失败则整体回滚）
        R<Void> r = userClient.addCredit(userId, PUBLISH_CREDIT);
        if (r == null || !r.success()) {
            throw new BizIllegalException("积分服务调用失败，事务回滚");
        }
        return item.getId();
    }

    /**
     * 分页查询：无筛选条件时走首页列表缓存（热点数据缓存场景）
     */
    @Override
    public PageDTO<ItemVO> queryItemPage(ItemPageQuery query) {
        // 空字符串视为无筛选条件（前端 el-select 清空后传 "" 而非 null，会导致查不到数据）
        boolean categoryBlank = StrUtil.isBlank(query.getCategory());
        boolean campusBlank = StrUtil.isBlank(query.getCampusArea());
        boolean keywordBlank = StrUtil.isBlank(query.getKeyword());
        // 首页列表（无任何筛选条件，且非"我的发布"查询）走缓存
        boolean cacheable = categoryBlank && campusBlank && keywordBlank && query.getUserId() == null && query.getPageNo() == 1;
        String cacheKey = CACHE_ITEM_LIST_KEY + query.getPageNo() + ":" + query.getPageSize();
        if (cacheable) {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                // 必须用 TypeReference 反序列化，否则 PageDTO 泛型擦除导致 list 元素变 Map、前端渲染不出数据
                PageDTO<ItemVO> result = JSONUtil.toBean(json, new TypeReference<PageDTO<ItemVO>>() {
                }, false);
                // 命中缓存后叠加 Redis 浏览量增量（与详情口径一致）
                fillViewCount(result);
                return result;
            }
        }
        // 构建查询条件："我的发布"查全部状态，公共列表仅查在售（status=0）
        boolean isMyPublish = query.getUserId() != null;
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(!isMyPublish, Item::getStatus, 0)
                .eq(isMyPublish, Item::getSellerId, query.getUserId())
                .eq(!categoryBlank, Item::getCategory, query.getCategory())
                .eq(!campusBlank, Item::getCampusArea, query.getCampusArea())
                .like(!keywordBlank, Item::getTitle, query.getKeyword())
                .orderByDesc(Item::getCreateTime);
        Page<Item> p = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        PageDTO<ItemVO> result = PageDTO.of(p, item -> BeanUtil.copyProperties(item, ItemVO.class));
        // 先写缓存：只存 DB 原始值，避免把叠加后的浏览量一起缓存导致下次命中重复叠加（同资讯模块修复）
        if (cacheable) {
            long ttl = 300 + ThreadLocalRandom.current().nextLong(300);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), ttl, TimeUnit.SECONDS);
        }
        // 返回前叠加 Redis 浏览量增量（与详情接口口径一致：DB 存量 + Redis 增量）
        fillViewCount(result);
        return result;
    }

    /**
     * 列表浏览量叠加 Redis 增量，保证与详情口径一致（DB 存量 + Redis 增量）
     */
    private void fillViewCount(PageDTO<ItemVO> result) {
        if (result.getList() == null || result.getList().isEmpty()) {
            return;
        }
        result.getList().forEach(vo -> {
            String incrStr = stringRedisTemplate.opsForValue().get(VIEW_ITEM_KEY + vo.getId());
            if (incrStr != null) {
                vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + Integer.parseInt(incrStr));
            }
        });
    }

    /**
     * 详情查询：三级缓存 + 防穿透/击穿/雪崩
     */
    @Override
    public ItemVO queryItemById(Long id) {
        if (id == null) {
            throw new BadRequestException("闲置id不能为空");
        }
        // 浏览量 +1（Redis 计数，定时合并回数据库）
        stringRedisTemplate.opsForValue().increment(VIEW_ITEM_KEY + id);
        Item item = queryWithCache(id);
        if (item == null) {
            return null;
        }
        ItemVO vo = BeanUtil.copyProperties(item, ItemVO.class);
        // 浏览量 = DB 存量 + Redis 增量
        String incrStr = stringRedisTemplate.opsForValue().get(VIEW_ITEM_KEY + id);
        int incr = incrStr == null ? 0 : Integer.parseInt(incrStr);
        vo.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + incr);
        // Feign 填充卖家昵称
        fillSellerName(vo);
        return vo;
    }

    @Override
    public void updateStatus(Long id, Integer status, Long userId) {
        Item item = getById(id);
        if (item == null) {
            throw new BadRequestException("闲置不存在");
        }
        // 仅发布者可操作
        if (!item.getSellerId().equals(userId)) {
            throw new ForbiddenException("无权操作该闲置");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BadRequestException("状态不合法");
        }
        item.setStatus(status);
        updateById(item);
        // 清理缓存，保证一致性
        evictCache(id);
    }

    /**
     * 定时任务：每分钟将 Redis 浏览量增量合并回数据库（模拟异步落库）
     */
    @Override
    @Scheduled(fixedDelay = 60000)
    public void syncViewCount() {
        // 使用 SCAN 代替 KEYS（避免 O(N) 阻塞 Redis）
        Set<String> keys = RedisUtils.scanKeys(stringRedisTemplate, VIEW_ITEM_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String id = key.substring(VIEW_ITEM_KEY.length());
            String value = stringRedisTemplate.opsForValue().getAndDelete(key);
            if (value == null || Long.parseLong(value) == 0) {
                continue;
            }
            // 合并到 DB（原子自增），并清理缓存
            update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, id)
                    .setSql("view_count = view_count + " + value));
            evictCache(Long.valueOf(id));
        }
        log.info("浏览量合并完成，共处理 {} 个闲置", keys.size());
    }

    /**
     * 三级缓存查询：Caffeine -&gt; Redis -&gt; MySQL（互斥锁防击穿）
     */
    private Item queryWithCache(Long id) {
        // 1.一级缓存 Caffeine
        Item item = caffeineCache.getIfPresent(id);
        if (item != null) {
            return item;
        }
        // 2.二级缓存 Redis（空值标记：防穿透）
        String json = stringRedisTemplate.opsForValue().get(CACHE_ITEM_KEY + id);
        if (json != null) {
            if (json.isEmpty()) {
                return null;
            }
            item = JSONUtil.toBean(json, Item.class);
            caffeineCache.put(id, item);
            return item;
        }
        // 3.两级都 miss：互斥锁回源数据库（防击穿）
        return queryWithMutex(id);
    }

    /**
     * 互斥锁方案：SETNX 加锁，未抢到锁则短暂休眠后循环重试（最多 20 次 ≈ 累计等待 1s）；
     * 抢到锁后二次检查缓存再回源数据库
     */
    private Item queryWithMutex(Long id) {
        final int maxRetry = 20;
        for (int i = 0; i < maxRetry; i++) {
            String lockKey = LOCK_ITEM_KEY + id;
            // 尝试加锁
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL.toMillis(), TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 二次检查 Redis（double check）
                    String json = stringRedisTemplate.opsForValue().get(CACHE_ITEM_KEY + id);
                    if (json != null) {
                        return json.isEmpty() ? null : JSONUtil.toBean(json, Item.class);
                    }
                    // 查询数据库
                    Item item = getById(id);
                    if (item == null) {
                        // 缓存空值（防穿透），短 TTL
                        stringRedisTemplate.opsForValue().set(CACHE_ITEM_KEY + id, "", CACHE_NULL_TTL.toMillis(), TimeUnit.MILLISECONDS);
                        return null;
                    }
                    // 回填缓存：随机 TTL 防雪崩
                    long ttl = CACHE_TTL.getSeconds() + ThreadLocalRandom.current().nextLong(300);
                    stringRedisTemplate.opsForValue().set(CACHE_ITEM_KEY + id, JSONUtil.toJsonStr(item), ttl, TimeUnit.SECONDS);
                    caffeineCache.put(id, item);
                    return item;
                } finally {
                    // 释放锁
                    stringRedisTemplate.delete(lockKey);
                }
            }
            // 未抢到锁：休眠 50ms 后重试（等待持锁线程回填缓存）；超限抛异常，避免无限递归
            ThreadUtil.sleep(50);
        }
        throw new BizIllegalException("系统繁忙，请稍后再试");
    }

    /**
     * 清理两级缓存（发布/收藏/状态变更后调用，保证一致性）
     */
    @Override
    public void evictCache(Long id) {
        caffeineCache.invalidate(id);
        stringRedisTemplate.delete(CACHE_ITEM_KEY + id);
        // 简单清理：首页列表缓存整体失效
        // 使用 SCAN 代替 KEYS（避免 O(N) 阻塞 Redis）
        Set<String> keys = RedisUtils.scanKeys(stringRedisTemplate, CACHE_ITEM_LIST_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 删除闲置（仅发布者本人或管理员可操作），删除后清理缓存与浏览量记录
     */
    @Override
    public void deleteItem(Long id, Long userId) {
        Item item = getById(id);
        if (item == null) {
            throw new BadRequestException("闲置不存在");
        }
        // 权限校验：仅发布者本人可删除（管理员接口可后续扩展）
        if (!item.getSellerId().equals(userId)) {
            throw new ForbiddenException("无权删除他人的闲置");
        }
        removeById(id);
        // 清理缓存
        evictCache(id);
        // 清理 Redis 浏览量计数
        stringRedisTemplate.delete("view:item:" + id);
    }

    /**
     * Feign 填充卖家昵称
     */
    private void fillSellerName(ItemVO vo) {
        try {
            R<SellerVO> r = userClient.queryUserById(vo.getSellerId());
            if (r != null && r.getData() != null) {
                vo.setSellerName(r.getData().getNickName());
            }
        } catch (Exception e) {
            log.warn("填充卖家昵称失败：sellerId = {}", vo.getSellerId(), e);
        }
    }
}
