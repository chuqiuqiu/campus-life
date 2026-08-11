package com.campus.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.domain.po.Signup;
import com.campus.activity.domain.vo.ActivityVO;

import java.util.List;

/**
 * 报名服务接口
 */
public interface ISignupService extends IService<Signup> {

    /**
     * 报名活动：Lua 脚本防超卖 + Redis 全局唯一 ID 生成凭证编号
     *
     * @param activityId 活动id
     * @param userId     用户id
     * @return 报名凭证编号
     */
    String signup(Long activityId, Long userId);

    /**
     * 取消报名：删除报名记录 + 名额回滚 + 热度回退
     *
     * @param activityId 活动id
     * @param userId     用户id
     */
    void cancelSignup(Long activityId, Long userId);

    /**
     * 活动打卡（报名后方可打卡）
     */
    void checkin(Long activityId, Long userId);

    /**
     * 我报名的活动列表
     */
    List<ActivityVO> queryMyActivities(Long userId);
}
