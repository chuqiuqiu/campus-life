package com.campus.user.domain.vo;

import lombok.Data;

/**
 * 登录成功返回（对标黑马商城 UserLoginVO）
 */
@Data
public class UserLoginVO {
    /**
     * token
     */
    private String token;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;
}
