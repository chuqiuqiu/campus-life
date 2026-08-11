package com.campus.info.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.common.domain.PageDTO;
import com.campus.info.domain.dto.InfoFormDTO;
import com.campus.info.domain.po.Info;
import com.campus.info.domain.query.InfoPageQuery;
import com.campus.info.domain.vo.InfoVO;

/**
 * 资讯服务接口
 */
public interface IInfoService extends IService<Info> {

    /**
     * 发布资讯
     */
    Long addInfo(InfoFormDTO dto, Long userId);

    /**
     * 分页查询资讯（无筛选条件时走热门列表缓存）
     */
    PageDTO<InfoVO> queryInfoPage(InfoPageQuery query);

    /**
     * 资讯详情（浏览量 Redis 计数 + 缓存）
     */
    InfoVO queryInfoById(Long id);

    /**
     * 点赞/取消点赞（Redis Set 去重，一用户一赞）
     *
     * @param id     资讯id
     * @param userId 用户id
     * @return 真实点赞数
     */
    Long likeInfo(Long id, Long userId);
}
