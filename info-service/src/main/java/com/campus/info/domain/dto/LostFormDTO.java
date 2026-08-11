package com.campus.info.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 发布失物/招领表单
 */
@Data
public class LostFormDTO {
    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
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
     * 物品类型
     */
    private String itemType;
    /**
     * 丢失/拾到地点
     */
    private String location;
    /**
     * 联系方式
     */
    @NotBlank(message = "联系方式不能为空")
    private String contact;
    /**
     * 类型：0寻物 1招领
     */
    private Integer type;
}
