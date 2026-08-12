package com.campus.info.controller;

import com.campus.common.domain.PageDTO;
import com.campus.common.domain.R;
import com.campus.common.exception.UnauthorizedException;
import com.campus.common.utils.UserContext;
import com.campus.info.domain.dto.InfoFormDTO;import com.campus.info.domain.query.InfoPageQuery;
import com.campus.info.domain.vo.InfoVO;
import com.campus.info.service.IInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 校园资讯接口
 */
@Api(tags = "校园资讯相关接口")
@RestController
@RequestMapping("/infos")
@RequiredArgsConstructor
public class InfoController {

    private final IInfoService infoService;

    /**
     * 发布资讯
     */
    @ApiOperation("发布资讯")
    @PostMapping
    public R<Long> addInfo(@RequestBody @Validated InfoFormDTO infoForm) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return R.ok(infoService.addInfo(infoForm, userId));
    }

    /**
     * 分页查询资讯（热门列表走缓存）
     */
    @ApiOperation("分页查询资讯")
    @GetMapping("/page")
    public R<PageDTO<InfoVO>> queryInfoPage(InfoPageQuery query) {
        return R.ok(infoService.queryInfoPage(query));
    }

    /**
     * 资讯详情（浏览量 Redis 计数）
     */
    @ApiOperation("资讯详情")
    @GetMapping("/{id}")
    public R<InfoVO> queryInfoById(@PathVariable("id") Long id) {
        return R.ok(infoService.queryInfoById(id));
    }

    /**
     * 删除资讯（仅发布者本人可操作）
     */
    @ApiOperation("删除资讯")
    @DeleteMapping("/{id}")
    public R<Void> deleteInfo(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        infoService.deleteInfo(id, userId);
        return R.ok();
    }

    /**
     * 点赞资讯
     */
    @ApiOperation("点赞资讯")
    @PostMapping("/{id}/like")
    public R<Map<String, Object>> like(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        Long likeCount = infoService.likeInfo(id, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", likeCount);
        return R.ok(data);
    }
}
