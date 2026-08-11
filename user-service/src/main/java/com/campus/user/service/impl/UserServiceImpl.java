package com.campus.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.BizIllegalException;
import com.campus.common.utils.RedisRateLimiter;
import com.campus.common.utils.WebUtils;
import com.campus.user.domain.dto.LoginFormDTO;
import com.campus.user.domain.po.User;
import com.campus.user.domain.vo.UserLoginVO;
import com.campus.user.domain.vo.UserVO;
import com.campus.user.mapper.UserMapper;
import com.campus.user.service.IUserService;
import com.campus.user.utils.JwtTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现：登录、Redis 会话缓存、限流防刷、积分（对标黑马商城 + 黑马点评）
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 用户信息缓存 key 前缀
     */
    private static final String CACHE_USER_KEY = "cache:user:";
    /**
     * 用户会话（token）缓存 key 前缀：用于会话管理
     */
    private static final String LOGIN_SESSION_KEY = "login:session:";
    /**
     * 用户信息缓存有效期
     */
    private static final Duration CACHE_USER_TTL = Duration.ofMinutes(30);

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtTool jwtTool;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisRateLimiter redisRateLimiter;

    @Override
    public UserLoginVO login(LoginFormDTO loginForm) {
        // 1.限流防刷：同一 IP 每分钟最多 10 次登录尝试（Redis Lua 原子限流）
        String ip = getClientIp();
        boolean pass = redisRateLimiter.tryAcquire("limit:login:ip:" + ip, 10, 60);
        if (!pass) {
            log.warn("登录接口限流触发，ip = {}", ip);
            throw new BizIllegalException("操作过于频繁，请稍后再试");
        }
        // 2.查询用户
        User user = lambdaQuery()
                .eq(User::getPhone, loginForm.getPhone())
                .one();
        if (user == null) {
            throw new BadRequestException("手机号或密码错误");
        }
        // 3.密码校验（BCrypt）
        if (!passwordEncoder.matches(loginForm.getPassword(), user.getPassword())) {
            throw new BadRequestException("手机号或密码错误");
        }
        // 4.账号状态校验
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizIllegalException("账号已被禁用");
        }
        // 5.签发 token
        String token = jwtTool.createToken(user.getId());
        // 6.缓存会话：用户信息缓存 + token 会话缓存（滑动续期）
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        cacheUser(userVO);
        stringRedisTemplate.opsForValue().set(
                LOGIN_SESSION_KEY + user.getId(), token, CACHE_USER_TTL.toMillis(), TimeUnit.MILLISECONDS);
        // 7.组装返回
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickName(user.getNickName());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public UserVO me(Long userId) {
        // 1.优先查 Redis 缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_USER_KEY + userId);
        if (json != null) {
            // 命中缓存，滑动续期（模拟会话活跃管理）
            stringRedisTemplate.expire(CACHE_USER_KEY + userId, CACHE_USER_TTL.toMillis(), TimeUnit.MILLISECONDS);
            return JSONUtil.toBean(json, UserVO.class);
        }
        // 2.缓存未命中，查询数据库（缓存穿透防护：查不到不回填）
        User user = getById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        // 3.回填缓存
        cacheUser(userVO);
        return userVO;
    }

    @Override
    public void updateUser(User user) {
        user.setPhone(null); // 手机号不允许修改
        updateById(user);
        // 删除缓存，保证一致性
        stringRedisTemplate.delete(CACHE_USER_KEY + user.getId());
    }

    @Override
    public void addCredit(Long userId, Integer point) {
        if (userId == null || point == null || point <= 0) {
            throw new BadRequestException("参数不合法");
        }
        // 使用 setSql 原子自增，避免并发覆盖（Seata 全局事务参与方）
        boolean updated = update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("credit = credit + " + point));
        if (!updated) {
            throw new BizIllegalException("用户不存在，积分增加失败");
        }
        // 删除用户缓存，保证一致性
        stringRedisTemplate.delete(CACHE_USER_KEY + userId);
        log.info("用户 {} 增加积分 {}，事务参与方执行完成", userId, point);
    }

    /**
     * 缓存用户信息到 Redis
     */
    private void cacheUser(UserVO userVO) {
        stringRedisTemplate.opsForValue().set(
                CACHE_USER_KEY + userVO.getId(),
                JSONUtil.toJsonStr(userVO),
                CACHE_USER_TTL.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取客户端 IP（兼容反向代理透传）
     */
    private String getClientIp() {
        String ip = WebUtils.getRequest().getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = WebUtils.getRequest().getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
