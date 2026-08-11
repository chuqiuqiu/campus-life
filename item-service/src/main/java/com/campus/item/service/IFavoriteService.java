package com.campus.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.common.domain.PageDTO;
import com.campus.item.domain.po.Favorite;
import com.campus.item.domain.vo.ItemVO;

/**
 * 收藏服务接口（Redis Set 记录收藏状态，防重复收藏）
 */
public interface IFavoriteService extends IService<Favorite> {

    /**
     * 收藏闲置（幂等：已收藏返回 false）
     *
     * @param itemId 闲置id
     * @param userId 用户id
     * @return true 本次新增收藏，false 已收藏
     */
    Boolean favorite(Long itemId, Long userId);

    /**
     * 取消收藏
     */
    void unfavorite(Long itemId, Long userId);

    /**
     * 是否已收藏
     */
    Boolean isFavorite(Long itemId, Long userId);

    /**
     * 我的收藏分页列表（查库 + Redis 计数）
     */
    PageDTO<ItemVO> queryMyFavorites(Long userId, Integer pageNo, Integer pageSize);
}
