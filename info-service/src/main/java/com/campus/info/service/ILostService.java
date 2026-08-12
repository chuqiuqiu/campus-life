package com.campus.info.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.common.domain.PageDTO;
import com.campus.info.domain.dto.LostFormDTO;
import com.campus.info.domain.po.Lost;
import com.campus.info.domain.query.LostPageQuery;
import com.campus.info.domain.vo.LostVO;

/**
 * 失物招领服务接口
 */
public interface ILostService extends IService<Lost> {

    /**
     * 发布失物/招领（Redis 全局唯一 ID 生成编号）
     */
    Long addLost(LostFormDTO dto, Long userId);

    /**
     * 分页查询
     */
    PageDTO<LostVO> queryLostPage(LostPageQuery query);

    /**
     * 详情
     */
    LostVO queryLostById(Long id);

    /**
     * 更新状态（已找到/已归还），仅发布者可操作
     */
    void updateStatus(Long id, Integer status, Long userId);

    /**
     * 删除失物记录（仅发布者本人可操作）
     *
     * @param id     失物id
     * @param userId 当前用户id
     */
    void deleteLost(Long id, Long userId);
}
