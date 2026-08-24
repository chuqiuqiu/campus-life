package com.campus.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.domain.po.Activity;
import com.campus.activity.domain.po.Signup;
import com.campus.activity.domain.vo.ActivityVO;
import com.campus.activity.mapper.SignupMapper;
import com.campus.activity.service.IActivityService;
import com.campus.activity.service.ISignupService;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.BizIllegalException;
import com.campus.common.utils.RedisIdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 报名服务实现：Lua 防超卖 + Redis 全局唯一 ID + 打卡（黑马点评核心场景）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl extends ServiceImpl<SignupMapper, Signup> implements ISignupService {

    /**
     * 报名人数 key
     */
    private static final String SIGNUP_COUNT_KEY = "activity:signup:count:";
    /**
     * 报名人数上限 key
     */
    private static final String SIGNUP_MAX_KEY = "activity:signup:max:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisIdWorker redisIdWorker;
    private final IActivityService activityService;

    /**
     * 报名 Lua 脚本（原子判断名额，防超卖）
     */
    private DefaultRedisScript<Long> signupScript;

    @PostConstruct
    public void init() {
        signupScript = new DefaultRedisScript<>();
        signupScript.setLocation(new ClassPathResource("signup.lua"));
        signupScript.setResultType(Long.class);
    }

    @Override
    public String signup(Long activityId, Long userId) {
        // 1.校验活动存在且可报名（状态按时间动态判断，不依赖 DB 静态字段）
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BadRequestException("活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        // 报名窗口：仅未开始（活动开始后停止报名）
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BadRequestException("活动已结束，无法报名");
        }
        if (activity.getStartTime() != null && !now.isBefore(activity.getStartTime())) {
            throw new BadRequestException("活动已开始，报名已截止");
        }
        // 1.5 初始化 Redis 报名名额（幂等：预置活动首次报名时补初始化，不会覆盖已有值）
        stringRedisTemplate.opsForValue().setIfAbsent(
                SIGNUP_MAX_KEY + activityId, String.valueOf(activity.getMaxPeople()));
        stringRedisTemplate.opsForValue().setIfAbsent(
                SIGNUP_COUNT_KEY + activityId, String.valueOf(activity.getSignedCount()));
        // 刷新名额 key TTL（活动结束后 1 天自动过期，防 key 无限积累）
        expireSignupKeys(activityId, activity);
        // 2.执行 Lua 脚本：原子扣减名额（防超卖，对标黑马点评秒杀）
        // 注意：脚本使用 KEYS[1](报名数) 和 KEYS[2](名额上限)，两个 key 都必须放 keys 列表
        Long result = stringRedisTemplate.execute(
                signupScript,
                Arrays.asList(SIGNUP_COUNT_KEY + activityId, SIGNUP_MAX_KEY + activityId)
        );
        if (result == null || result != 1L) {
            throw new BizIllegalException("手慢了，报名人数已满！");
        }
        // 3.生成报名凭证编号（Redis 全局唯一 ID）
        String signNo = "S" + redisIdWorker.nextId("signup");
        // 4.落库报名记录（唯一索引保证同用户不可重复报名）
        Signup signup = new Signup();
        signup.setSignNo(signNo);
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setSignTime(LocalDateTime.now());
        signup.setCheckinStatus(0);
        try {
            save(signup);
        } catch (Exception e) {
            // 重复报名（唯一索引冲突）：回滚 Redis 名额计数
            stringRedisTemplate.opsForValue().decrement(SIGNUP_COUNT_KEY + activityId);
            throw new BizIllegalException("您已经报名过该活动");
        }
        // 5.DB 报名数 +1（条件更新 signed_count < max_people：DB 为最终权威，
        //   防止 Redis 名额与 DB 不一致时超卖）；失败则补偿删除报名记录并回滚 Redis 名额
        boolean incrOk;
        if (activity.getMaxPeople() == null) {
            incrOk = activityService.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, activityId)
                    .setSql("signed_count = signed_count + 1"));
        } else {
            incrOk = activityService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, activityId)
                    .lt(Activity::getSignedCount, activity.getMaxPeople())
                    .setSql("signed_count = signed_count + 1"));
        }
        if (!incrOk) {
            removeById(signup.getId());
            stringRedisTemplate.opsForValue().decrement(SIGNUP_COUNT_KEY + activityId);
            throw new BizIllegalException("手慢了，报名人数已满！");
        }
        // 刷新名额 key TTL
        expireSignupKeys(activityId, activity);
        stringRedisTemplate.opsForZSet().incrementScore("activity:heat:rank", String.valueOf(activityId), ActivityServiceImpl.HEAT_SIGNUP);
        // 6.清理活动详情缓存
        activityService.evictCache(activityId);
        log.info("用户 {} 报名活动 {} 成功，凭证编号 {}", userId, activityId, signNo);
        return signNo;
    }

    /**
     * 取消报名：删除报名记录 + Redis 名额回滚 + DB 报名数 -1 + 热度回退 -10
     */
    @Override
    public void cancelSignup(Long activityId, Long userId) {
        // 1.校验报名记录存在
        Signup signup = lambdaQuery()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getUserId, userId)
                .one();
        if (signup == null) {
            throw new BadRequestException("您未报名该活动");
        }
        // 2.删除报名记录
        removeById(signup.getId());
        // 3.Redis 名额回滚（不低于 0）
        Long count = stringRedisTemplate.opsForValue().decrement(SIGNUP_COUNT_KEY + activityId);
        if (count != null && count < 0) {
            stringRedisTemplate.opsForValue().set(SIGNUP_COUNT_KEY + activityId, "0");
        }
        // 刷新名额 key TTL
        expireSignupKeys(activityId, activityService.getById(activityId));
        // 4.DB 已报名数 -1（不低于 0）
        activityService.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .gt(Activity::getSignedCount, 0)
                .setSql("signed_count = signed_count - 1"));
        // 5.热度回退 -10
        stringRedisTemplate.opsForZSet().incrementScore(
                "activity:heat:rank", String.valueOf(activityId), -ActivityServiceImpl.HEAT_SIGNUP);
        // 6.清理活动缓存
        activityService.evictCache(activityId);
        log.info("用户 {} 取消报名活动 {}", userId, activityId);
    }

    @Override
    public void checkin(Long activityId, Long userId) {
        // 0.打卡窗口：仅进行中（已开始且未结束）
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BadRequestException("活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BadRequestException("活动未开始，暂不能打卡");
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BadRequestException("活动已结束，无法打卡");
        }
        // 1.查询报名记录
        Signup signup = lambdaQuery()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getUserId, userId)
                .one();
        if (signup == null) {
            throw new BadRequestException("未报名该活动，无法打卡");
        }
        if (signup.getCheckinStatus() != null && signup.getCheckinStatus() == 1) {
            throw new BizIllegalException("您已经打过卡了");
        }
        // 2.更新打卡状态
        signup.setCheckinStatus(1);
        signup.setCheckinTime(LocalDateTime.now());
        updateById(signup);
    }

    @Override
    public List<ActivityVO> queryMyActivities(Long userId) {
        // 查询我报名的活动id列表
        List<Long> activityIds = lambdaQuery()
                .eq(Signup::getUserId, userId)
                .list()
                .stream()
                .map(Signup::getActivityId)
                .collect(java.util.stream.Collectors.toList());
        if (activityIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 批量查询活动
        List<Activity> activities = activityService.listByIds(activityIds);
        return BeanUtil.copyToList(activities, ActivityVO.class);
    }

    /**
     * 刷新报名名额 key 的 TTL（活动结束时间 + 1 天，防止 key 无限积累）
     */
    private void expireSignupKeys(Long activityId, Activity activity) {
        if (activity == null || activity.getEndTime() == null) {
            return;
        }
        long ttl = Duration.between(LocalDateTime.now(), activity.getEndTime()).getSeconds() + 86400;
        if (ttl < 3600) {
            ttl = 3600;
        }
        stringRedisTemplate.expire(SIGNUP_COUNT_KEY + activityId, ttl, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SIGNUP_MAX_KEY + activityId, ttl, TimeUnit.SECONDS);
    }

    /**
     * 定时对账：每 5 分钟用 DB 报名记录数校准 Redis 名额计数（DB 为最终权威），
     * 解决进程崩溃/补偿回滚等导致的 Redis 计数漂移；配合 DB 条件更新不会因此超卖
     */
    @Scheduled(fixedDelay = 300000)
    public void reconcileSignupCounts() {
        try {
            List<Activity> activities = activityService.list();
            if (activities.isEmpty()) {
                return;
            }
            Map<Long, Long> dbCounts = baseMapper.countByActivity().stream()
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("activityId")).longValue(),
                            m -> ((Number) m.get("cnt")).longValue()));
            for (Activity a : activities) {
                long cnt = dbCounts.getOrDefault(a.getId(), 0L);
                stringRedisTemplate.opsForValue().set(SIGNUP_COUNT_KEY + a.getId(), String.valueOf(cnt));
                expireSignupKeys(a.getId(), a);
            }
            log.info("报名名额对账完成，共校准 {} 个活动", activities.size());
        } catch (Exception e) {
            log.warn("报名名额对账失败：{}", e.getMessage());
        }
    }
}
