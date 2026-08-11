package com.campus.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名实体
 */
@Data
@TableName("tb_activity_signup")
public class Signup {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 报名凭证编号（Redis 全局唯一 ID 生成）
     */
    private String signNo;
    /**
     * 活动id
     */
    private Long activityId;
    /**
     * 报名用户id
     */
    private Long userId;
    /**
     * 报名时间
     */
    private LocalDateTime signTime;
    /**
     * 打卡时间
     */
    private LocalDateTime checkinTime;
    /**
     * 打卡状态：0未打卡 1已打卡
     */
    private Integer checkinStatus;
}
