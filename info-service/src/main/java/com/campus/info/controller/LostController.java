package com.campus.info.controller;

import com.campus.common.domain.PageDTO;
import com.campus.common.domain.R;
import com.campus.common.exception.UnauthorizedException;
import com.campus.common.utils.UserContext;
import com.campus.info.domain.dto.LostFormDTO;
import com.campus.info.domain.query.LostPageQuery;
import com.campus.info.domain.vo.LostVO;
import com.campus.info.service.ILostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 失物招领接口
 */
@Api(tags = "失物招领相关接口")
@RestController
@RequestMapping("/losts")
@RequiredArgsConstructor
public class LostController {

    private final ILostService lostService;

    /**
     * 发布失物/招领（Redis 全局唯一 ID 编号）
     */
    @ApiOperation("发布失物/招领")
    @PostMapping
    public R<Long> addLost(@RequestBody @Validated LostFormDTO lostForm) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return R.ok(lostService.addLost(lostForm, userId));
    }

    /**
     * 分页查询失物招领
     */
    @ApiOperation("分页查询失物招领")
    @GetMapping("/page")
    public R<PageDTO<LostVO>> queryLostPage(LostPageQuery query) {
        return R.ok(lostService.queryLostPage(query));
    }

    /**
     * 失物详情
     */
    @ApiOperation("失物详情")
    @GetMapping("/{id}")
    public R<LostVO> queryLostById(@PathVariable("id") Long id) {
        return R.ok(lostService.queryLostById(id));
    }

    /**
     * 更新状态（已找到/已归还）
     */
    @ApiOperation("更新状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        lostService.updateStatus(id, status, userId);
        return R.ok();
    }
}
