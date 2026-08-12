package com.campus.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.client.UserClient;
import com.campus.activity.domain.dto.ActivityFormDTO;
import com.campus.activity.domain.po.Activity;
import com.campus.activity.domain.po.Signup;
import com.campus.activity.domain.query.ActivityPageQuery;
import com.campus.activity.domain.vo.ActivityTopVO;
import com.campus.activity.domain.vo.ActivityVO;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.SignupMapper;
import com.campus.activity.service.IActivityService;
import com.campus.common.domain.PageDTO;
import com.campus.common.domain.R;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.ForbiddenException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 活动服务实现（黑马点评核心场景：点赞 Set、热度排行榜 ZSet、缓存防穿透）
 * <p>
 * 热度计算规则（Redis ZSet 实时维护，每 5 分钟落库 DB 冗余）：
 * <pre>
 *   发布活动 +50、报名 +10、点赞 +2、浏览详情 +1
 * </pre>
 * 活动状态按时间动态计算，不依赖数据库静态字段：
 * <pre>
 *   未开始 → 0 报名中；进行中 → 1 进行中；已结束 → 2 已结束
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements IActivityService {

    /**
     * 活动详情缓存 key
     */
    private static final String CACHE_ACTIVITY_KEY = "cache:activity:";
    /**
     * 点赞用户集合 key：like:activity:{id}
     */
    private static final String LIKE_ACTIVITY_KEY = "like:activity:";
    /**
     * 热度排行榜 ZSet key
     */
    private static final String HEAT_RANK_KEY = "activity:heat:rank";

    /**
     * 热度权重：发布 +50、报名 +10、点赞 +2、浏览 +1
     */
    public static final double HEAT_PUBLISH = 50;
    public static final double HEAT_SIGNUP = 10;
    public static final double HEAT_LIKE = 2;
    public static final double HEAT_VIEW = 1;

    /**
     * 预置管理员用户 id（SQL 中 13800000003/管理员）。如需扩展角色，可改为角色表校验
     */
    public static final Long ADMIN_USER_ID = 1002L;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration CACHE_NULL_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate stringRedisTemplate;
    private final UserClient userClient;
    private final SignupMapper signupMapper;

    /**
     * 一级缓存：Caffeine 本地缓存
     */
    private final Cache<Long, Activity> caffeineCache = Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 初始化热度 ZSet：把数据库已有的活动（含 SQL 预置数据）按 DB heat 加入排行榜，
     * 否则预置活动不在 ZSet 里，热度/排行会缺失
     */
    @PostConstruct
    public void initHeatRanks() {
        try {
            List<Activity> all = list();
            for (Activity a : all) {
                Double score = stringRedisTemplate.opsForZSet().score(HEAT_RANK_KEY, String.valueOf(a.getId()));
                if (score == null) {
                    double heat = a.getHeat() == null ? 0 : a.getHeat();
                    stringRedisTemplate.opsForZSet().add(HEAT_RANK_KEY, String.valueOf(a.getId()), heat);
                }
            }
            log.info("活动热度 ZSet 初始化完成，共 {} 个活动", all.size());
        } catch (Exception e) {
            log.warn("活动热度初始化失败（Redis 可能未就绪，不影响启动）：{}", e.getMessage());
        }
    }

    @Override
    public Long addActivity(ActivityFormDTO dto, Long userId) {
        // 1.校验时间
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new BadRequestException("开始时间不能晚于结束时间");
        }
        // 2.落库
        Activity activity = BeanUtil.copyProperties(dto, Activity.class);
        activity.setOrganizerId(userId);
        activity.setSignedCount(0);
        activity.setLikeCount(0);
        activity.setStatus(0);
        activity.setHeat(HEAT_PUBLISH);
        save(activity);
        // 3.初始化 Redis 报名名额（防超卖）
        stringRedisTemplate.opsForValue().set("activity:signup:max:" + activity.getId(), String.valueOf(activity.getMaxPeople()));
        stringRedisTemplate.opsForValue().set("activity:signup:count:" + activity.getId(), "0");
        // 4.热度 ZSet 初始热度（发布 +50）
        stringRedisTemplate.opsForZSet().add(HEAT_RANK_KEY, String.valueOf(activity.getId()), HEAT_PUBLISH);
        return activity.getId();
    }

    @Override
    public PageDTO<ActivityVO> queryActivityPage(ActivityPageQuery query) {
        LocalDateTime now = LocalDateTime.now();
        // 空字符串视为无筛选（前端 el-select 清空后传 "" 而非 null）
        boolean keywordBlank = cn.hutool.core.util.StrUtil.isBlank(query.getKeyword());
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(query.getUserId() != null, Activity::getOrganizerId, query.getUserId())
                .like(!keywordBlank, Activity::getTitle, query.getKeyword())
                .orderByDesc(Activity::getCreateTime);
        // 状态筛选：状态是动态计算的，不能用 DB 静态字段，改用时间条件模拟
        if (query.getStatus() != null) {
            switch (query.getStatus()) {
                case 0: // 报名中：未开始（与 calcStatus 的 status=0 语义一致）
                    wrapper.gt(Activity::getStartTime, now);
                    break;
                case 1: // 进行中：已开始且未结束
                    wrapper.le(Activity::getStartTime, now).gt(Activity::getEndTime, now);
                    break;
                case 2: // 已结束
                    wrapper.lt(Activity::getEndTime, now);
                    break;
                default:
                    break;
            }
        }
        Page<Activity> p = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return PageDTO.of(p, activity -> toVO(activity, null));
    }

    /**
     * 详情查询：Caffeine + Redis 两级缓存 + 空值防穿透 + 浏览热度 +1
     */
    @Override
    public ActivityVO queryActivityById(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("活动id不能为空");
        }
        // 1.一级缓存 Caffeine
        Activity activity = caffeineCache.getIfPresent(id);
        if (activity == null) {
            // 2.二级缓存 Redis（空值标记防穿透）
            String json = stringRedisTemplate.opsForValue().get(CACHE_ACTIVITY_KEY + id);
            if (json != null) {
                if (json.isEmpty()) {
                    return null;
                }
                activity = JSONUtil.toBean(json, Activity.class);
                caffeineCache.put(id, activity);
            } else {
                // 3.回源数据库
                activity = getById(id);
                if (activity == null) {
                    // 缓存空值（防穿透）
                    stringRedisTemplate.opsForValue().set(CACHE_ACTIVITY_KEY + id, "", CACHE_NULL_TTL.toMillis(), TimeUnit.MILLISECONDS);
                    return null;
                }
                stringRedisTemplate.opsForValue().set(CACHE_ACTIVITY_KEY + id, JSONUtil.toJsonStr(activity), CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
                caffeineCache.put(id, activity);
            }
        }
        // 4.浏览活动详情热度 +1（点击即浏览）
        stringRedisTemplate.opsForZSet().incrementScore(HEAT_RANK_KEY, String.valueOf(id), HEAT_VIEW);
        ActivityVO vo = toVO(activity, userId);
        // 5.填充组织者昵称
        try {
            R<Map<String, Object>> r = userClient.queryUserById(activity.getOrganizerId());
            if (r != null && r.getData() != null) {
                vo.setOrganizerName(String.valueOf(r.getData().get("nickName")));
            }
        } catch (Exception e) {
            log.warn("填充组织者昵称失败：organizerId = {}", activity.getOrganizerId());
        }
        return vo;
    }

    /**
     * 点赞/取消点赞：Redis Set 原子操作（黑马点评核心）
     * SADD 成功 = 点赞，SREM = 取消点赞；SCARD 统计点赞总数
     */
    @Override
    public Long likeActivity(Long id, Long userId) {
        String key = LIKE_ACTIVITY_KEY + id;
        // 点赞（已点赞则返回 false）
        Long added = stringRedisTemplate.opsForSet().add(key, userId.toString());
        if (added != null && added > 0) {
            // 点赞数 +1（DB 同步维护）
            update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, id)
                    .setSql("like_count = like_count + 1"));
            // 热度 ZSet +2
            stringRedisTemplate.opsForZSet().incrementScore(HEAT_RANK_KEY, String.valueOf(id), HEAT_LIKE);
        } else {
            // 取消点赞
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
            update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, id)
                    .gt(Activity::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
            stringRedisTemplate.opsForZSet().incrementScore(HEAT_RANK_KEY, String.valueOf(id), -HEAT_LIKE);
        }
        // 清理详情缓存
        evictCache(id);
        // 返回最新点赞数
        Long count = stringRedisTemplate.opsForSet().size(key);
        return count == null ? 0 : count;
    }

    /**
     * 热度排行榜：Redis ZSet 按 score 倒序取前 n 名（批量查询，避免 N+1）
     */
    @Override
    public List<ActivityTopVO> topActivities(Integer n) {
        // ZREVRANGE WITHSCORES 返回有序集合，直接按返回顺序遍历
        // 注意：绝不能转成 HashSet，否则顺序丢失、排行榜乱序
        java.util.Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HEAT_RANK_KEY, 0, n - 1);
        List<ActivityTopVO> list = new ArrayList<>();
        if (tuples == null || tuples.isEmpty()) {
            return list;
        }
        // 1.收集活动 id 与热度分数
        List<Long> ids = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            Long id = Long.valueOf(tuple.getValue());
            ids.add(id);
            scoreMap.put(id, tuple.getScore());
        }
        if (ids.isEmpty()) {
            return list;
        }
        // 2.批量查询活动，避免逐个 getById 的 N+1 问题
        Map<Long, Activity> activityMap = listByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Activity::getId, a -> a));
        // 3.按 ZSet 顺序组装（保持热度降序）
        int rank = 1;
        for (Long id : ids) {
            Activity activity = activityMap.get(id);
            if (activity == null) {
                continue;
            }
            ActivityTopVO vo = new ActivityTopVO();
            vo.setId(activity.getId());
            vo.setTitle(activity.getTitle());
            vo.setImage(activity.getImage());
            vo.setHeat(scoreMap.get(id));
            vo.setRank(rank++);
            list.add(vo);
        }
        return list;
    }

    /**
     * 定时任务：每 5 分钟将 ZSet 热度同步回数据库 + 已结束活动落库状态（异步落库）
     */
    @Override
    @Scheduled(fixedDelay = 300000)
    public void syncHeat() {
        LocalDateTime now = LocalDateTime.now();
        // 1.热度批量同步回 DB（MP 批处理，减少网络往返）
        java.util.Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(HEAT_RANK_KEY, 0, -1);
        int count = 0;
        if (tuples != null && !tuples.isEmpty()) {
            List<Activity> updates = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }
                Activity a = new Activity();
                a.setId(Long.valueOf(tuple.getValue()));
                a.setHeat(tuple.getScore());
                updates.add(a);
            }
            if (!updates.isEmpty()) {
                updateBatchById(updates);
                count = updates.size();
            }
        }
        // 2.已结束活动落库 status=2
        boolean marked = update(new LambdaUpdateWrapper<Activity>()
                .lt(Activity::getEndTime, now)
                .set(Activity::getStatus, 2));
        log.info("活动热度同步完成：{} 个活动，已结束标记执行={}", count, marked);
    }

    /**
     * 删除活动（仅组织者或管理员）：
     * 删除活动 + 删除该活动全部报名记录 + 清理 Redis 热度/点赞/名额 + 清理缓存
     */
    @Override
    public void deleteActivity(Long id, Long userId) {
        // 1.校验活动存在
        Activity activity = getById(id);
        if (activity == null) {
            throw new BadRequestException("活动不存在");
        }
        // 2.权限校验：组织者或管理员
        if (!activity.getOrganizerId().equals(userId) && !isAdmin(userId)) {
            throw new ForbiddenException("无权删除该活动");
        }
        // 3.删除活动 + 报名记录
        removeById(id);
        signupMapper.delete(new LambdaQueryWrapper<Signup>().eq(Signup::getActivityId, id));
        // 4.清理 Redis：热度 ZSet 成员、点赞集合、报名名额
        stringRedisTemplate.opsForZSet().remove(HEAT_RANK_KEY, String.valueOf(id));
        stringRedisTemplate.delete(LIKE_ACTIVITY_KEY + id);
        stringRedisTemplate.delete("activity:signup:count:" + id);
        stringRedisTemplate.delete("activity:signup:max:" + id);
        // 5.清理缓存
        evictCache(id);
        log.info("活动 {} 已被用户 {} 删除", id, userId);
    }

    /**
     * 转 VO：热度取 Redis ZSet 实时值（未初始化用 DB 值），状态按时间动态计算
     */
    private ActivityVO toVO(Activity activity, Long userId) {
        ActivityVO vo = BeanUtil.copyProperties(activity, ActivityVO.class);
        // 实时热度：Redis ZSet 为准，未初始化成员用 DB 兜底
        Double heat = stringRedisTemplate.opsForZSet().score(HEAT_RANK_KEY, String.valueOf(activity.getId()));
        vo.setHeat(heat == null ? (activity.getHeat() == null ? 0 : activity.getHeat()) : heat);
        // 动态状态：按时间计算，覆盖 DB 静态字段
        vo.setStatus(calcStatus(activity));
        if (userId != null) {
            vo.setLiked(Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                    .isMember(LIKE_ACTIVITY_KEY + activity.getId(), userId.toString())));
            // 是否可管理（组织者或管理员）
            vo.setIsOwner(userId.equals(activity.getOrganizerId()) || isAdmin(userId));
            // 是否已报名（取消报名按钮依赖此状态）
            Long signupCount = signupMapper.selectCount(new LambdaQueryWrapper<Signup>()
                    .eq(Signup::getActivityId, activity.getId())
                    .eq(Signup::getUserId, userId));
            vo.setSignedUp(signupCount != null && signupCount > 0);
        }
        return vo;
    }

    /**
     * 是否为预置管理员
     */
    private boolean isAdmin(Long userId) {
        return ADMIN_USER_ID.equals(userId);
    }

    /**
     * 动态计算活动状态（不依赖数据库静态字段）：
     * 未开始 → 0 报名中；已开始未结束 → 1 进行中；已结束 → 2 已结束
     */
    public static Integer calcStatus(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            return 2;
        }
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            return 0;
        }
        return 1;
    }

    /**
     * 清理两级缓存
     */
    @Override
    public void evictCache(Long id) {
        caffeineCache.invalidate(id);
        stringRedisTemplate.delete(CACHE_ACTIVITY_KEY + id);
    }
}
