package com.campus.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.domain.dto.ActivityFormDTO;
import com.campus.activity.domain.po.Activity;
import com.campus.activity.domain.query.ActivityPageQuery;
import com.campus.activity.domain.vo.ActivityTopVO;
import com.campus.activity.domain.vo.ActivityVO;
import com.campus.common.domain.PageDTO;

import java.util.List;

/**
 * 活动服务接口
 */
public interface IActivityService extends IService<Activity> {

    /**
     * 发布活动：落库 + Redis 初始化报名名额 + 热度 ZSet 初始热度
     *
     * @param dto    发布表单
     * @param userId 组织者id
     * @return 活动id
     */
    Long addActivity(ActivityFormDTO dto, Long userId);

    /**
     * 分页查询活动
     */
    PageDTO<ActivityVO> queryActivityPage(ActivityPageQuery query);

    /**
     * 查询活动详情（Redis 缓存 + 空值防穿透 + 点赞/报名状态）
     */
    ActivityVO queryActivityById(Long id, Long userId);

    /**
     * 点赞/取消点赞（Redis Set 原子操作，黑马点评核心场景）
     *
     * @param id     活动id
     * @param userId 用户id
     * @return 最新点赞数
     */
    Long likeActivity(Long id, Long userId);

    /**
     * 热度排行榜（Redis ZSet，按热度倒序取前 n 名）
     */
    List<ActivityTopVO> topActivities(Integer n);

    /**
     * 定时任务：将 Redis 热度 ZSet 同步回数据库
     */
    void syncHeat();

    /**
     * 清理活动两级缓存（报名/点赞后调用，保证一致性）
     */
    void evictCache(Long id);

    /**
     * 删除活动（仅组织者或管理员）：删除活动与报名记录 + 清理 Redis 热度/点赞/名额
     *
     * @param id     活动id
     * @param userId 当前用户id
     */
    void deleteActivity(Long id, Long userId);
}
