package com.campus.info.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失物招领实体
 */
@Data
@TableName("tb_lost")
public class Lost {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 招领/寻物编号（Redis 全局唯一 ID 生成）
     */
    private String lostNo;
    /**
     * 标题
     */
    private String title;
    /**
     * 详细描述
     */
    private String description;
    /**
     * 图片
     */
    private String image;
    /**
     * 物品类型：证件/数码/钥匙/钱包/其他
     */
    private String itemType;
    /**
     * 丢失/拾到地点
     */
    private String location;
    /**
     * 联系方式
     */
    private String contact;
    /**
     * 类型：0寻物 1招领
     */
    private Integer type;
    /**
     * 状态：0寻找中 1已找到/已归还
     */
    private Integer status;
    /**
     * 发布者id
     */
    private Long userId;
    /**
     * 发布时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
