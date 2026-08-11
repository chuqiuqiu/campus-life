package com.campus.info.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯展示 VO
 */
@Data
public class InfoVO {
    /**
     * 资讯id
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 封面图
     */
    private String cover;
    /**
     * 分类
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
     * 当前用户是否已点赞
     */
    private Boolean liked;
    /**
     * 发布时间
     */
    private LocalDateTime createTime;
}
