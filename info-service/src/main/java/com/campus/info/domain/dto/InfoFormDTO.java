package com.campus.info.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发布资讯表单
 */
@Data
public class InfoFormDTO {
    /**
     * 资讯标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题过长")
    private String title;
    /**
     * 资讯内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;
    /**
     * 封面图
     */
    private String cover;
    /**
     * 分类
     */
    private String category;
}
