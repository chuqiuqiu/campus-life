package com.campus.user.controller;

import com.campus.common.domain.R;
import com.campus.common.utils.UserContext;
import com.campus.user.domain.dto.LoginFormDTO;
import com.campus.user.domain.po.User;
import com.campus.user.domain.vo.UserLoginVO;
import com.campus.user.domain.vo.UserVO;
import com.campus.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务接口（对标黑马商城 UserController）
 */
@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 登录（限流防刷：IP 维度）
     */
    @ApiOperation("手机号密码登录")
    @PostMapping("/login")
    public R<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginForm) {
        return R.ok(userService.login(loginForm));
    }

    /**
     * 获取当前登录用户信息（Redis 会话缓存）
     */
    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/me")
    public R<UserVO> me() {
        Long userId = UserContext.getUser();
        return R.ok(userService.me(userId));
    }

    /**
     * 更新当前用户信息
     */
    @ApiOperation("更新当前用户信息")
    @PutMapping("/me")
    public R<Void> updateMe(@RequestBody User user) {
        Long userId = UserContext.getUser();
        user.setId(userId);
        userService.updateUser(user);
        return R.ok();
    }

    /**
     * 根据 id 查询用户信息（公开，供前端展示发布者）
     */
    @ApiOperation("根据id查询用户")
    @GetMapping("/{id}")
    public R<UserVO> queryUserById(@PathVariable("id") Long id) {
        return R.ok(userService.me(id));
    }

    /**
     * 增加积分（内部接口：item-service 发布闲置时 Feign 调用，参与 Seata 全局事务）
     */
    @ApiOperation("增加积分（内部接口）")
    @PostMapping("/credit/add")
    public R<Void> addCredit(@RequestParam("userId") Long userId, @RequestParam("point") Integer point) {
        userService.addCredit(userId, point);
        return R.ok();
    }
}
