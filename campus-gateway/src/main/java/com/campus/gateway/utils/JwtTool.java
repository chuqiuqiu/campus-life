package com.campus.gateway.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.campus.common.exception.UnauthorizedException;
import com.campus.gateway.config.JwtProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 工具（与 user-service 的 JwtTool 逻辑一致，HS256 对称密钥）
 * <p>
 * 注意：hutool 的 HMacJWTSigner 非线程安全（内部共享 HMac 引擎），并发 verify 会抛
 * ArrayIndexOutOfBoundsException。因此每次解析/签发都创建独立的 signer，杜绝并发问题。
 */
@Component
public class JwtTool {
    private final JwtProperties properties;

    public JwtTool(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 解析 token，返回用户id；非法/过期抛出 UnauthorizedException
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
        // 3.校验签名
        if (!jwt.verify()) {
            throw new UnauthorizedException("无效的token");
        }
        // 4.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }
        // 5.数据校验与解析
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            throw new UnauthorizedException("无效的token");
        }
        try {
            return Long.valueOf(userPayload.toString());
        } catch (RuntimeException e) {
            throw new UnauthorizedException("无效的token");
        }
    }
}
