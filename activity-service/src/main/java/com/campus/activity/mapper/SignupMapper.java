package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.domain.po.Signup;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报名 Mapper
 */
public interface SignupMapper extends BaseMapper<Signup> {

    /**
     * 按活动统计报名数（名额对账任务使用，DB 为最终权威）
     */
    @Select("SELECT activity_id AS activityId, COUNT(*) AS cnt FROM tb_activity_signup GROUP BY activity_id")
    List<Map<String, Object>> countByActivity();
}
