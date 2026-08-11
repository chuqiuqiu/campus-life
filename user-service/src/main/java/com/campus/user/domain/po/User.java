package com.campus.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对标黑马商城 User）
 */
@Data
@TableName("tb_user")
public class User {
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 手机号（登录账号）
     */
    private String phone;
    /**
     * 密码（BCrypt加密）
     */
    private String password;
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
    /**
     * 状态：1正常 0禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
