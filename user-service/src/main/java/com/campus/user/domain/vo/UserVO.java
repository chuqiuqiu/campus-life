package com.campus.user.domain.vo;

import lombok.Data;

/**
 * 用户信息展示 VO
 */
@Data
public class UserVO {
    /**
     * 用户id
     */
    private Long id;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别：0未知 1男 2女
     */
    private Integer gender;
    /**
     * 所在学院
     */
    private String college;
    /**
     * 校园积分
     */
    private Integer credit;
}
