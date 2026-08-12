package com.campus.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.common.domain.PageDTO;
import com.campus.item.domain.dto.ItemFormDTO;
import com.campus.item.domain.po.Item;
import com.campus.item.domain.query.ItemPageQuery;
import com.campus.item.domain.vo.ItemVO;

/**
 * 闲置服务接口
 */
public interface IItemService extends IService<Item> {

    /**
     * 发布闲置（Seata 全局事务：创建闲置 + 跨服务增加积分）
     *
     * @param dto    发布表单
     * @param userId 发布者id
     * @return 新闲置id
     */
    Long addItem(ItemFormDTO dto, Long userId);

    /**
     * 分页查询闲置（无筛选条件时走首页列表缓存）
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageDTO<ItemVO> queryItemPage(ItemPageQuery query);

    /**
     * 查询闲置详情（Caffeine + Redis + DB 三级缓存，防穿透/击穿/雪崩）
     *
     * @param id 闲置id
     * @return 详情
     */
    ItemVO queryItemById(Long id);

    /**
     * 更新闲置状态（出售/下架），仅发布者可操作
     *
     * @param id     闲置id
     * @param status 目标状态
     * @param userId 当前用户id
     */
    void updateStatus(Long id, Integer status, Long userId);

    /**
     * 将 Redis 中的浏览量增量合并回数据库（定时任务调用）
     */
    void syncViewCount();

    /**
     * 清理闲置两级缓存（发布/收藏/状态变更后调用，保证列表与详情实时一致）
     */
    void evictCache(Long id);

    /**
     * 删除闲置（仅发布者本人或管理员可操作），删除后清理缓存
     *
     * @param id     闲置id
     * @param userId 当前用户id
     */
    void deleteItem(Long id, Long userId);
}
