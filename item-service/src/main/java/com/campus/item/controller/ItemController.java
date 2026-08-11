package com.campus.item.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.campus.common.domain.PageDTO;
import com.campus.common.domain.R;
import com.campus.common.exception.UnauthorizedException;
import com.campus.common.utils.UserContext;
import com.campus.item.domain.dto.ItemFormDTO;
import com.campus.item.domain.query.ItemPageQuery;
import com.campus.item.domain.vo.ItemVO;
import com.campus.item.service.IFavoriteService;
import com.campus.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 校园闲置接口
 */
@Api(tags = "校园闲置相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;
    private final IFavoriteService favoriteService;

    /**
     * 发布闲置（Seata 分布式事务 + 跨服务积分）
     */
    @ApiOperation("发布闲置")
    @PostMapping
    public R<Long> addItem(@RequestBody @Validated ItemFormDTO itemForm) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        return R.ok(itemService.addItem(itemForm, userId));
    }

    /**
     * 分页查询闲置
     */
    @ApiOperation("分页查询闲置")
    @GetMapping("/page")
    public R<PageDTO<ItemVO>> queryItemPage(ItemPageQuery query) {
        return R.ok(itemService.queryItemPage(query));
    }

    /**
     * 查询闲置详情（三级缓存；Sentinel 熔断降级示例）
     */
    @ApiOperation("查询闲置详情")
    @GetMapping("/{id}")
    @SentinelResource(value = "queryItemById", blockHandler = "queryItemByIdBlock")
    public R<ItemVO> queryItemById(@PathVariable("id") Long id) {
        return R.ok(itemService.queryItemById(id));
    }

    /**
     * Sentinel 流控/熔断降级处理
     */
    public R<ItemVO> queryItemByIdBlock(Long id, BlockException e) {
        return R.error(429, "系统繁忙，请稍后再试");
    }

    /**
     * 更新闲置状态（出售/下架）
     */
    @ApiOperation("更新闲置状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        itemService.updateStatus(id, status, UserContext.getUser());
        return R.ok();
    }

    /**
     * 收藏闲置（幂等）
     */
    @ApiOperation("收藏闲置")
    @PostMapping("/{id}/favorite")
    public R<Map<String, Object>> favorite(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        Boolean isNew = favoriteService.favorite(id, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("isNew", isNew);
        data.put("favorited", true);
        return R.ok(data);
    }

    /**
     * 取消收藏
     */
    @ApiOperation("取消收藏")
    @DeleteMapping("/{id}/favorite")
    public R<Void> unfavorite(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        favoriteService.unfavorite(id, userId);
        return R.ok();
    }

    /**
     * 是否已收藏
     */
    @ApiOperation("是否已收藏")
    @GetMapping("/{id}/isFavorite")
    public R<Boolean> isFavorite(@PathVariable("id") Long id) {
        return R.ok(favoriteService.isFavorite(id, UserContext.getUser()));
    }

    /**
     * 我的收藏
     */
    @ApiOperation("我的收藏")
    @GetMapping("/favorites")
    public R<PageDTO<ItemVO>> myFavorites(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                                          @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        return R.ok(favoriteService.queryMyFavorites(userId, pageNo, pageSize));
    }

    /**
     * 校验登录，未登录抛 401
     */
    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
    }
}
