package com.campus.activity.controller;

import com.campus.activity.domain.dto.ActivityFormDTO;
import com.campus.activity.domain.query.ActivityPageQuery;
import com.campus.activity.domain.vo.ActivityTopVO;
import com.campus.activity.domain.vo.ActivityVO;
import com.campus.activity.service.IActivityService;
import com.campus.activity.service.ISignupService;
import com.campus.common.domain.PageDTO;
import com.campus.common.domain.R;
import com.campus.common.exception.UnauthorizedException;
import com.campus.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园活动接口（黑马点评核心场景落地）
 */
@Api(tags = "校园活动相关接口")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final IActivityService activityService;
    private final ISignupService signupService;

    /**
     * 发布活动
     */
    @ApiOperation("发布活动")
    @PostMapping
    public R<Long> addActivity(@RequestBody @Validated ActivityFormDTO activityForm) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        return R.ok(activityService.addActivity(activityForm, userId));
    }

    /**
     * 分页查询活动
     */
    @ApiOperation("分页查询活动")
    @GetMapping("/page")
    public R<PageDTO<ActivityVO>> queryActivityPage(ActivityPageQuery query) {
        return R.ok(activityService.queryActivityPage(query));
    }

    /**
     * 活动详情（含点赞/报名状态）
     */
    @ApiOperation("活动详情")
    @GetMapping("/{id}")
    public R<ActivityVO> queryActivityById(@PathVariable("id") Long id) {
        return R.ok(activityService.queryActivityById(id, UserContext.getUser()));
    }

    /**
     * 报名活动（Lua 防超卖 + Redis 全局唯一 ID 凭证）
     */
    @ApiOperation("报名活动")
    @PostMapping("/{id}/signup")
    public R<Map<String, String>> signup(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        String signNo = signupService.signup(id, userId);
        Map<String, String> data = new HashMap<>();
        data.put("signNo", signNo);
        return R.ok(data);
    }

    /**
     * 取消报名（名额回滚 + 热度回退）
     */
    @ApiOperation("取消报名")
    @DeleteMapping("/{id}/signup")
    public R<Void> cancelSignup(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        signupService.cancelSignup(id, userId);
        return R.ok();
    }

    /**
     * 删除活动（组织者或管理员）
     */
    @ApiOperation("删除活动")
    @DeleteMapping("/{id}")
    public R<Void> deleteActivity(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        activityService.deleteActivity(id, userId);
        return R.ok();
    }

    /**
     * 活动打卡
     */
    @ApiOperation("活动打卡")
    @PostMapping("/{id}/checkin")
    public R<Void> checkin(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        signupService.checkin(id, userId);
        return R.ok();
    }

    /**
     * 点赞/取消点赞（Redis Set，幂等切换）
     */
    @ApiOperation("点赞/取消点赞")
    @PostMapping("/{id}/like")
    public R<Map<String, Object>> like(@PathVariable("id") Long id) {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        Long likeCount = activityService.likeActivity(id, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", likeCount);
        return R.ok(data);
    }

    /**
     * 热度排行榜（Redis ZSet）
     */
    @ApiOperation("热度排行榜")
    @GetMapping("/top")
    public R<List<ActivityTopVO>> top(@RequestParam(value = "n", defaultValue = "10") Integer n) {
        return R.ok(activityService.topActivities(n));
    }

    /**
     * 我报名的活动
     */
    @ApiOperation("我报名的活动")
    @GetMapping("/my")
    public R<List<ActivityVO>> myActivities() {
        Long userId = UserContext.getUser();
        requireLogin(userId);
        return R.ok(signupService.queryMyActivities(userId));
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
