package com.campus.item.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.domain.PageDTO;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.BizIllegalException;
import com.campus.item.domain.po.Favorite;
import com.campus.item.domain.po.Item;
import com.campus.item.domain.vo.ItemVO;
import com.campus.item.mapper.FavoriteMapper;
import com.campus.item.mapper.ItemMapper;
import com.campus.item.service.IFavoriteService;
import com.campus.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 收藏服务实现：Redis Set 原子防重复收藏 + 计数统计（黑马点评点赞场景的收藏版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    /**
     * 收藏用户集合 key：favorite:item:{itemId}，value 为 userId 的 Set
     */
    private static final String FAVORITE_ITEM_KEY = "favorite:item:";
    /**
     * 收藏数 key：favorite:count:{itemId}
     */
    private static final String FAVORITE_COUNT_KEY = "favorite:count:";
    /**
     * 用户收藏的闲置集合 key：favorite:user:{userId}
     */
    private static final String FAVORITE_USER_KEY = "favorite:user:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ItemMapper itemMapper;
    private final IItemService itemService;

    @Override
    public Boolean favorite(Long itemId, Long userId) {
        // 1.校验闲置存在且在售
        Item item = getItemById(itemId);
        if (item == null || item.getStatus() != 0) {
            throw new BadRequestException("闲置不存在或已下架");
        }
        // 2.Redis SADD 原子判断：返回 1 表示首次收藏，0 表示已收藏（幂等）
        Long added = stringRedisTemplate.opsForSet().add(FAVORITE_ITEM_KEY + itemId, userId.toString());
        if (added == null || added == 0) {
            return false;
        }
        // 3.记录用户收藏集合（供我的收藏查询）
        stringRedisTemplate.opsForSet().add(FAVORITE_USER_KEY + userId, itemId.toString());
        // 4.计数 +1（Redis 与 DB 同步维护）
        stringRedisTemplate.opsForValue().increment(FAVORITE_COUNT_KEY + itemId);
        itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .setSql("favorite_count = favorite_count + 1"));
        // 5.落库冗余一条收藏记录（失败时回滚，避免 Redis/计数与记录不一致）
        Favorite favorite = new Favorite();
        favorite.setItemId(itemId);
        favorite.setUserId(userId);
        try {
            save(favorite);
        } catch (Exception e) {
            // 回滚：移除 Redis Set 成员 + 计数回退 + DB favorite_count -1
            stringRedisTemplate.opsForSet().remove(FAVORITE_ITEM_KEY + itemId, userId.toString());
            stringRedisTemplate.opsForSet().remove(FAVORITE_USER_KEY + userId, itemId.toString());
            stringRedisTemplate.opsForValue().decrement(FAVORITE_COUNT_KEY + itemId);
            itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .gt(Item::getFavoriteCount, 0)
                    .setSql("favorite_count = favorite_count - 1"));
            log.warn("收藏落库失败，已回滚：itemId = {}, userId = {}", itemId, userId);
            throw new BizIllegalException("收藏失败，请稍后重试");
        }
        // 6.清理闲置详情/列表缓存，保证收藏数实时（否则命中旧缓存数字不变）
        itemService.evictCache(itemId);
        return true;
    }

    @Override
    public void unfavorite(Long itemId, Long userId) {
        Long removed = stringRedisTemplate.opsForSet().remove(FAVORITE_ITEM_KEY + itemId, userId.toString());
        if (removed == null || removed == 0) {
            throw new BizIllegalException("尚未收藏该闲置");
        }
        stringRedisTemplate.opsForSet().remove(FAVORITE_USER_KEY + userId, itemId.toString());
        // 计数 -1（不低于 0）
        Long count = stringRedisTemplate.opsForValue().decrement(FAVORITE_COUNT_KEY + itemId);
        if (count != null && count < 0) {
            stringRedisTemplate.opsForValue().set(FAVORITE_COUNT_KEY + itemId, "0");
        }
        itemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .gt(Item::getFavoriteCount, 0)
                .setSql("favorite_count = favorite_count - 1"));
        // 删除冗余记录
        remove(new LambdaQueryWrapper<Favorite>().eq(Favorite::getItemId, itemId).eq(Favorite::getUserId, userId));
        // 清理闲置详情/列表缓存，保证收藏数实时
        itemService.evictCache(itemId);
    }

    @Override
    public Boolean isFavorite(Long itemId, Long userId) {
        // 未登录视为未收藏（浏览公开，收藏状态需登录才有意义）
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(FAVORITE_ITEM_KEY + itemId, userId.toString()));
    }

    @Override
    public PageDTO<ItemVO> queryMyFavorites(Long userId, Integer pageNo, Integer pageSize) {
        // 分页查询收藏记录（按收藏时间倒序）
        Page<Favorite> p = page(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        // 批量查询闲置信息
        List<Long> itemIds = p.getRecords().stream().map(Favorite::getItemId).collect(java.util.stream.Collectors.toList());
        if (itemIds.isEmpty()) {
            return PageDTO.of(p.getTotal(), java.util.Collections.emptyList());
        }
        List<Item> items = itemMapper.selectBatchIds(itemIds);
        List<ItemVO> vos = BeanUtil.copyToList(items, ItemVO.class);
        // 填充 Redis 收藏数（与 DB 计数保持一致，此处直接展示 DB 值）
        return PageDTO.of(p.getTotal(), vos);
    }

    /**
     * 查询闲置（复用缓存查询逻辑，保持计数一致）
     */
    private Item getItemById(Long id) {
        return itemMapper.selectById(id);
    }
}
