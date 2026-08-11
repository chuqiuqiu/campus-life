package com.campus.item.client.dto;

import lombok.Data;

/**
 * 卖家信息 DTO（Feign 从 user-service 获取，仅保留展示字段）
 */
@Data
public class SellerVO {
    /**
     * 用户id
     */
    private Long id;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 学院
     */
    private String college;
}
