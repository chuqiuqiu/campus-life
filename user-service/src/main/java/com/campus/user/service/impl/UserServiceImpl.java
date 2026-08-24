package com.campus.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.BizIllegalException;
import com.campus.common.exception.NotFoundException;
import com.campus.common.utils.RedisRateLimiter;
import com.campus.common.utils.WebUtils;
import com.campus.user.domain.dto.LoginFormDTO;
import com.campus.user.domain.dto.UserRegisterDTO;
import com.campus.user.domain.dto.UserUpdateDTO;
import com.campus.user.domain.po.User;
import com.campus.user.domain.vo.UserLoginVO;
import com.campus.user.domain.vo.UserPublicVO;
import com.campus.user.domain.vo.UserVO;
import com.campus.user.mapper.UserMapper;
import com.campus.user.service.IUserService;
import com.campus.user.utils.JwtTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
    public UserLoginVO register(UserRegisterDTO registerForm) {
        // 1.限流防刷：同一 IP 每分钟最多 5 次注册尝试
        String ip = getClientIp();
        boolean pass = redisRateLimiter.tryAcquire("limit:register:ip:" + ip, 5, 60);
        if (!pass) {
            throw new BizIllegalException("操作过于频繁，请稍后再试");
        }
        // 2.手机号唯一性预校验（并发场景由唯一索引兜底）
        Long exists = lambdaQuery().eq(User::getPhone, registerForm.getPhone()).count();
        if (exists != null && exists > 0) {
            throw new BizIllegalException("该手机号已注册");
        }
        // 3.落库（BCrypt 加密）
        User user = new User();
        user.setPhone(registerForm.getPhone());
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        user.setNickName(StrUtil.isBlank(registerForm.getNickName())
                ? "校园用户" + registerForm.getPhone().substring(7)
                : registerForm.getNickName());
        user.setStatus(1);
        try {
            save(user);
        } catch (Exception e) {
            throw new BizIllegalException("该手机号已注册");
        }
        // 4.注册即登录：签发 token + 缓存会话
        String token = jwtTool.createToken(user.getId());
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        cacheUser(userVO);
        stringRedisTemplate.opsForValue().set(
                LOGIN_SESSION_KEY + user.getId(), token, CACHE_USER_TTL.toMillis(), TimeUnit.MILLISECONDS);
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickName(user.getNickName());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public void logout(Long userId) {
        if (userId == null) {
            return;
        }
        // 清理服务端会话缓存（JWT 本身无状态，到期前仍可解析；此处保持"会话"语义一致）
        stringRedisTemplate.delete(LOGIN_SESSION_KEY + userId);
        log.info("用户 {} 退出登录", userId);
    }

    @Override
    public UserPublicVO publicUser(Long id) {
        // 复用 me() 的缓存与存在性校验，仅暴露公开字段
        return BeanUtil.copyProperties(me(id), UserPublicVO.class);
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
            throw new NotFoundException("用户不存在");
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        // 3.回填缓存
        cacheUser(userVO);
        return userVO;
    }

    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        // 只更新白名单字段，杜绝批量赋值（credit/status/password 等敏感字段不可由用户直接修改）
        User user = new User();
        user.setId(userId);
        user.setNickName(dto.getNickName());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender());
        user.setCollege(dto.getCollege());
        updateById(user);
        // 删除缓存，保证一致性
        stringRedisTemplate.delete(CACHE_USER_KEY + userId);
    }

    @Override
    public void addCredit(Long userId, Integer point) {
        if (userId == null || point == null || point <= 0) {
            throw new BadRequestException("参数不合法");
        }
        // 使用 setSql 参数化原子自增，避免并发覆盖（Seata 全局事务参与方）
        boolean updated = update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("credit = credit + {0}", point));
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
     * 获取客户端 IP（防伪造）：
     * 1. 优先取反向代理覆写的 X-Real-IP（nginx/网关可信头）
     * 2. 其次取 X-Forwarded-For 的最后一个值（代理链追加，真实 IP 在后；直接取首个可被客户端伪造绕过限流）
     * 3. 最后兜底 remoteAddr
     */
    private String getClientIp() {
        HttpServletRequest request = WebUtils.getRequest();
        // 1.可信的 X-Real-IP（由 nginx 用 $remote_addr 覆写）
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        // 2.X-Forwarded-For 取最后一个（后端代理追加，伪造值在前）
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String p = parts[i].trim();
                if (!p.isEmpty() && !"unknown".equalsIgnoreCase(p)) {
                    return p;
                }
            }
        }
        // 3.兜底
        return request.getRemoteAddr();
    }
}
