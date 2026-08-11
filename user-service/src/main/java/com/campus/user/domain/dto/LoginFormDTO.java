package com.campus.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 登录表单（对标黑马商城 LoginFormDTO）
 */
@Data
public class LoginFormDTO {
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
