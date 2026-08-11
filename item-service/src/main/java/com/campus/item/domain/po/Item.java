package com.campus.item.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校园闲置实体
 */
@Data
@TableName("tb_idle_item")
public class Item {
    /**
     * 闲置id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String description;
    /**
     * 图片地址，逗号分隔
     */
    private String images;
    /**
     * 价格（单位：分）
     */
    private Integer price;
    /**
     * 分类：教材/数码/生活/运动/其他
     */
    private String category;
    /**
     * 所在校区
     */
    private String campusArea;
    /**
     * 发布者id
     */
    private Long sellerId;
    /**
     * 状态：0在售 1已售出 2已下架
     */
    private Integer status;
    /**
     * 浏览量
     */
    private Integer viewCount;
    /**
     * 收藏数
     */
    private Integer favoriteCount;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
