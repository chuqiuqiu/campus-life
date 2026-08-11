package com.campus.item.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发布闲置表单
 */
@Data
public class ItemFormDTO {
    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题过长")
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
    @NotNull(message = "价格不能为空")
    private Integer price;
    /**
     * 分类
     */
    @NotBlank(message = "分类不能为空")
    private String category;
    /**
     * 所在校区
     */
    private String campusArea;
}
