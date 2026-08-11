package com.campus.info.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.domain.PageDTO;
import com.campus.common.exception.BadRequestException;
import com.campus.common.utils.UserContext;
import com.campus.info.domain.dto.InfoFormDTO;
import com.campus.info.domain.po.Info;
import com.campus.info.domain.query.InfoPageQuery;
import com.campus.info.domain.vo.InfoVO;
import com.campus.info.mapper.InfoMapper;
import com.campus.info.service.IInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 资讯服务实现：热门列表缓存（热点数据缓存场景）+ 浏览量 Redis 计数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfoServiceImpl extends ServiceImpl<InfoMapper, Info> implements IInfoService {

    /**
     * 资讯详情缓存 key
     */
    private static final String CACHE_INFO_KEY = "cache:info:";
    /**
     * 热门资讯列表缓存 key
     */
    private static final String CACHE_INFO_LIST_KEY = "cache:info:list:";
    /**
     * 资讯浏览量 Redis 计数 key
     */
    private static final String VIEW_INFO_KEY = "view:info:";
    /**
     * 资讯点赞用户集合 key：like:info:set:{id}（Set，一用户一赞）
     * 注意：前缀带 set 后缀，避开旧版本用同前缀存 String 计数导致的 WRONGTYPE 冲突
     */
    private static final String LIKE_INFO_KEY = "like:info:set:";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration CACHE_NULL_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Long addInfo(InfoFormDTO dto, Long userId) {
        Info info = BeanUtil.copyProperties(dto, Info.class);
        info.setAuthorId(userId);
        info.setViewCount(0);
        info.setLikeCount(0);
        info.setStatus(1);
        save(info);
        // 清理列表缓存
        evictListCache();
        return info.getId();
    }

    @Override
    public PageDTO<InfoVO> queryInfoPage(InfoPageQuery query) {
        // 热门资讯（首页无筛选）走 Redis 缓存；空字符串视为无筛选（前端 el-select 清空后传 "" 而非 null）
        boolean categoryBlank = StrUtil.isBlank(query.getCategory());
        boolean keywordBlank = StrUtil.isBlank(query.getKeyword());
        boolean cacheable = categoryBlank && keywordBlank && query.getPageNo() == 1;
        String cacheKey = CACHE_INFO_LIST_KEY + query.getPageSize();
        if (cacheable) {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                // 必须用 TypeReference 反序列化，否则 PageDTO 泛型擦除导致 list 元素变 Map、前端渲染不出数据
                PageDTO<InfoVO> result = JSONUtil.toBean(json, new TypeReference<PageDTO<InfoVO>>() {
                }, false);
                // 叠加 Redis 浏览量增量（实时展示浏览数）
                fillViewCount(result);
                return result;
            }
        }
        LambdaQueryWrapper<Info> wrapper = new LambdaQueryWrapper<Info>()
                .eq(Info::getStatus, 1)
                .eq(!categoryBlank, Info::getCategory, query.getCategory())
                .like(!keywordBlank, Info::getTitle, query.getKeyword())
                .orderByDesc(Info::getCreateTime);
        Page<Info> p = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        PageDTO<InfoVO> result = PageDTO.of(p, info -> BeanUtil.copyProperties(info, InfoVO.class));
        // 先写缓存：只存 DB 原始值，避免把叠加后的浏览量一起缓存导致下次命中重复叠加
        if (cacheable) {
            long ttl = CACHE_TTL.getSeconds() + ThreadLocalRandom.current().nextLong(300);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), ttl, TimeUnit.SECONDS);
        }
        // 返回前叠加 Redis 浏览量增量（与详情接口口径一致：DB 存量 + Redis 增量）
        fillViewCount(result);
        return result;
    }

    @Override
    public InfoVO queryInfoById(Long id) {
        if (id == null) {
            throw new BadRequestException("资讯id不能为空");
        }
        // 浏览量 Redis 计数 +1
        stringRedisTemplate.opsForValue().increment(VIEW_INFO_KEY + id);
        // 详情缓存（空值防穿透）
        Info info;
        String json = stringRedisTemplate.opsForValue().get(CACHE_INFO_KEY + id);
        if (json != null) {
            if (json.isEmpty()) {
                return null;
            }
            info = JSONUtil.toBean(json, Info.class);
        } else {
            info = getById(id);
            if (info == null) {
                stringRedisTemplate.opsForValue().set(CACHE_INFO_KEY + id, "", CACHE_NULL_TTL.toMillis(), TimeUnit.MILLISECONDS);
                return null;
            }
            stringRedisTemplate.opsForValue().set(CACHE_INFO_KEY + id, JSONUtil.toJsonStr(info), CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        }
        InfoVO vo = BeanUtil.copyProperties(info, InfoVO.class);
        // 浏览量 = DB 存量 + Redis 增量
        String incrStr = stringRedisTemplate.opsForValue().get(VIEW_INFO_KEY + id);
        int incr = incrStr == null ? 0 : Integer.parseInt(incrStr);
        vo.setViewCount((info.getViewCount() == null ? 0 : info.getViewCount()) + incr);
        // 当前用户是否已点赞（登录用户由网关透传 user-info）
        Long userId = UserContext.getUser();
        if (userId != null) {
            vo.setLiked(Boolean.TRUE.equals(
                    stringRedisTemplate.opsForSet().isMember(LIKE_INFO_KEY + id, userId.toString())));
        }
        return vo;
    }

    @Override
    public Long likeInfo(Long id, Long userId) {
        String key = LIKE_INFO_KEY + id;
        // Redis Set 原子去重：首次 SADD 成功=点赞，已存在则取消（一用户一赞，对标黑马点评）
        Long added = stringRedisTemplate.opsForSet().add(key, userId.toString());
        if (added != null && added > 0) {
            // 点赞：DB 同步 +1
            update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Info>()
                    .eq(Info::getId, id)
                    .setSql("like_count = like_count + 1"));
        } else {
            // 取消点赞：DB 同步 -1
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
            update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Info>()
                    .eq(Info::getId, id)
                    .gt(Info::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        }
        stringRedisTemplate.delete(CACHE_INFO_KEY + id);
        // 点赞后清理列表缓存，保证列表页实时
        evictListCache();
        // 返回真实点赞人数（Redis Set 大小）
        Long count = stringRedisTemplate.opsForSet().size(key);
        return count == null ? 0 : count;
    }

    /**
     * 列表浏览量叠加 Redis 增量，保证实时展示
     */
    private void fillViewCount(PageDTO<InfoVO> result) {
        if (result.getList() == null || result.getList().isEmpty()) {
            return;
        }
        result.getList().forEach(vo -> {
            String incrStr = stringRedisTemplate.opsForValue().get(VIEW_INFO_KEY + vo.getId());
            if (incrStr != null) {
                vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + Integer.parseInt(incrStr));
            }
        });
    }

    private void evictListCache() {
        java.util.Set<String> keys = stringRedisTemplate.keys(CACHE_INFO_LIST_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
