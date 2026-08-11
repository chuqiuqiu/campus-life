package com.campus.item.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闲置收藏实体（落库冗余，收藏状态与计数以 Redis Set 为准）
 */
@Data
@TableName("tb_idle_favorite")
public class Favorite {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 闲置id
     */
    private Long itemId;
    /**
     * 收藏用户id
     */
    private Long userId;
    /**
     * 收藏时间
     */
    private LocalDateTime createTime;
}
