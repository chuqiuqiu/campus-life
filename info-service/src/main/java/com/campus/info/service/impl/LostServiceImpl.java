package com.campus.info.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.domain.PageDTO;
import com.campus.common.exception.BadRequestException;
import com.campus.common.exception.ForbiddenException;
import com.campus.common.utils.RedisIdWorker;
import com.campus.info.domain.dto.LostFormDTO;
import com.campus.info.domain.po.Lost;
import com.campus.info.domain.query.LostPageQuery;
import com.campus.info.domain.vo.LostVO;
import com.campus.info.mapper.LostMapper;
import com.campus.info.service.ILostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 失物招领服务实现：Redis 全局唯一 ID 生成失物编号（黑马点评 RedisIdWorker 场景）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LostServiceImpl extends ServiceImpl<LostMapper, Lost> implements ILostService {

    private final RedisIdWorker redisIdWorker;

    @Override
    public Long addLost(LostFormDTO dto, Long userId) {
        Lost lost = BeanUtil.copyProperties(dto, Lost.class);
        // 失物编号：Redis 全局唯一 ID（时间戳 + 序列号）
        lost.setLostNo("L" + redisIdWorker.nextId("lost"));
        lost.setUserId(userId);
        lost.setStatus(0);
        save(lost);
        log.info("失物发布成功：id = {}，编号 = {}", lost.getId(), lost.getLostNo());
        return lost.getId();
    }

    @Override
    public PageDTO<LostVO> queryLostPage(LostPageQuery query) {
        // 空字符串视为无筛选（前端 el-select 清空后传 "" 而非 null）
        boolean itemTypeBlank = cn.hutool.core.util.StrUtil.isBlank(query.getItemType());
        boolean keywordBlank = cn.hutool.core.util.StrUtil.isBlank(query.getKeyword());
        LambdaQueryWrapper<Lost> wrapper = new LambdaQueryWrapper<Lost>()
                .eq(query.getType() != null, Lost::getType, query.getType())
                .eq(!itemTypeBlank, Lost::getItemType, query.getItemType())
                .like(!keywordBlank, Lost::getTitle, query.getKeyword())
                .orderByDesc(Lost::getCreateTime);
        Page<Lost> p = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return PageDTO.of(p, lost -> BeanUtil.copyProperties(lost, LostVO.class));
    }

    @Override
    public LostVO queryLostById(Long id) {
        Lost lost = getById(id);
        if (lost == null) {
            throw new BadRequestException("记录不存在");
        }
        return BeanUtil.copyProperties(lost, LostVO.class);
    }

    @Override
    public void updateStatus(Long id, Integer status, Long userId) {
        Lost lost = getById(id);
        if (lost == null) {
            throw new BadRequestException("记录不存在");
        }
        if (!lost.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该记录");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BadRequestException("状态不合法");
        }
        lost.setStatus(status);
        updateById(lost);
    }
}
