package com.campus.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.user.domain.dto.LoginFormDTO;
import com.campus.user.domain.dto.UserRegisterDTO;
import com.campus.user.domain.dto.UserUpdateDTO;
import com.campus.user.domain.po.User;
import com.campus.user.domain.vo.UserLoginVO;
import com.campus.user.domain.vo.UserPublicVO;
import com.campus.user.domain.vo.UserVO;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {

    /**
     * 登录：校验手机号密码，签发 token，缓存会话
     *
     * @param loginForm 登录表单
     * @return 登录结果（token + 用户信息）
     */
    UserLoginVO login(LoginFormDTO loginForm);

    /**
     * 获取当前登录用户信息（Redis 缓存优先，miss 回源数据库）
     *
     * @param userId 用户id
     * @return 用户信息
     */
    UserLoginVO register(UserRegisterDTO registerForm);

    /**
     * 退出登录：清理服务端会话缓存
     */
    void logout(Long userId);

    UserVO me(Long userId);

    /**
     * 查询用户公开信息（不暴露手机号/积分等敏感字段）
     */
    UserPublicVO publicUser(Long id);

    /**
     * 更新当前用户信息（仅允许修改白名单字段）
     *
     * @param userId 当前用户id
     * @param dto    可修改字段
     */
    void updateUser(Long userId, UserUpdateDTO dto);

    /**
     * 给用户增加积分（供 item-service 等通过 Feign 调用，参与 Seata 全局事务）
     *
     * @param userId 用户id
     * @param point  增加积分数
     */
    void addCredit(Long userId, Integer point);
}
