package com.campus.info.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校园资讯实体
 */
@Data
@TableName("tb_info")
public class Info {
    /**
     * 资讯id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 资讯标题
     */
    private String title;
    /**
     * 资讯内容
     */
    private String content;
    /**
     * 封面图
     */
    private String cover;
    /**
     * 分类：校园资讯/通知公告/讲座活动
     */
    private String category;
    /**
     * 作者id
     */
    private Long authorId;
    /**
     * 浏览量
     */
    private Integer viewCount;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 状态：0草稿 1已发布
     */
    private Integer status;
    /**
     * 发布时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
