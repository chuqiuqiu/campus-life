package com.campus.user.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.campus.common.exception.UnauthorizedException;
import com.campus.user.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具（对标黑马商城 JwtTool，使用 HS256 对称密钥，便于网关共用）
 * <p>
 * 注意：hutool 的 HMacJWTSigner 非线程安全（内部共享 HMac 引擎），并发 sign/verify 会抛
 * ArrayIndexOutOfBoundsException。因此每次签发/解析都创建独立的 signer。
 */
@Component
public class JwtTool {
    private final JwtProperties properties;

    public JwtTool(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 access-token
     *
     * @param userId 用户id
     * @return access-token
     */
    public String createToken(Long userId) {
        Duration ttl = properties.getTokenTTL();
        return JWT.create()
                .setPayload("user", userId)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(JWTSignerUtil.hs256(properties.getSecret().getBytes()))
                .sign();
    }

    /**
     * 解析 token，返回用户id
     *
     * @param token token
     * @return 用户id
     */
    public Long parseToken(String token) {
        // 1.校验 token 是否为空
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }
        // 2.每次创建独立 signer（线程安全），校验并解析 jwt
        JWTSigner jwtSigner = JWTSignerUtil.hs256(properties.getSecret().getBytes());
        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            throw new UnauthorizedException("无效的token", e);
        }
        // 3.校验 jwt 签名
        if (!jwt.verify()) {
            throw new UnauthorizedException("无效的token");
        }
        // 4.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }
        // 5.数据格式校验
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            throw new UnauthorizedException("无效的token");
        }
        // 6.数据解析
        try {
            return Long.valueOf(userPayload.toString());
        } catch (RuntimeException e) {
            throw new UnauthorizedException("无效的token");
        }
    }
}
