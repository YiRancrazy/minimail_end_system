package com.yirancrazy.minimall.notify.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价领域服务实现，实现评价提交、分页、商家回复、统计业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentManager commentManager;

    public CommentServiceImpl(CommentManager commentManager) {
        this.commentManager = commentManager;
    }

    /**
     * 用户提交评价：使用 userId 覆盖 DTO 中的评价人；merchantId 缺失时置 0（避免 null 列）；
     * 同一订单同一商品仅允许一条评价，通过 orderNo+spuId+userId 唯一约束保证。
     * @param userId 评价用户ID
     * @param dto 创建入参
     * @return 新评价ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, CommentCreateDTO dto) {
        CommentPO existing = findExisting(userId, dto.getOrderNo(), dto.getSpuId());
        if (existing != null) {
            throw new BizException(NotifyCodeEnum.COMMENT_NO_PERMISSION);
        }
        CommentPO po = new CommentPO();
        po.setOrderNo(dto.getOrderNo());
        po.setSpuId(dto.getSpuId());
        po.setSkuId(dto.getSkuId());
        po.setUserId(userId);
        po.setMerchantId(dto.getMerchantId() == null ? 0L : dto.getMerchantId());
        po.setRating(dto.getRating());
        po.setContent(dto.getContent());
        po.setImages(dto.getImages());
        po.setAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        commentManager.save(po);
        log.info("comment created, id={}, userId={}, spuId={}, orderNo={}",
                po.getId(), userId, dto.getSpuId(), dto.getOrderNo());
        return po.getId();
    }

    /**
     * 分页查询评价，支持按 SPU/用户/商家/订单号过滤；按 ID 降序返回。
     * @param dto 分页入参
     * @return 评价分页结果
     */
    @Override
    public CursorPageVO<CommentPO> page(CommentPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<CommentPO> wrapper = Wrappers.lambdaQuery(CommentPO.class);
        wrapper.lt(lastId != null, CommentPO::getId, lastId);
        if (dto.getSpuId() != null) {
            wrapper.eq(CommentPO::getSpuId, dto.getSpuId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(CommentPO::getUserId, dto.getUserId());
        }
        if (dto.getMerchantId() != null) {
            wrapper.eq(CommentPO::getMerchantId, dto.getMerchantId());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.eq(CommentPO::getOrderNo, dto.getOrderNo());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(CommentPO::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(CommentPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<CommentPO> records = commentManager.list(wrapper);
        return CursorPageVO.of(records, limit, CommentPO::getId);
    }

    /**
     * 商家回复评价：仅允许同 merchantId 的商家回复；已存在回复时直接覆盖（前端支持修改）。
     * @param id 评价ID
     * @param merchantId 商家ID
     * @param reply 回复内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, Long merchantId, String reply) {
        CommentPO po = commentManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMMENT_NOT_FOUND);
        }
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(NotifyCodeEnum.COMMENT_NO_PERMISSION);
        }
        po.setMerchantReply(reply);
        po.setMerchantReplyTime(LocalDateTime.now());
        commentManager.updateById(po);
        log.info("comment replied, id={}, merchantId={}", id, merchantId);
    }

    /**
     * 统计 SPU 的评价总数 / 平均分 / 各评分数量。
     * @param spuId 商品SPU ID
     * @return 评价统计视图
     */
    @Override
    public CommentStatsVO stats(Long spuId) {
        LambdaQueryWrapper<CommentPO> wrapper = Wrappers.lambdaQuery(CommentPO.class);
        wrapper.eq(CommentPO::getSpuId, spuId);
        wrapper.eq(CommentPO::getStatus, CommentStatusEnum.NORMAL.intCode());
        List<CommentPO> all = commentManager.list(wrapper);
        long total = all.size();
        long sum = 0;
        long r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0;
        for (CommentPO po : all) {
            int rating = po.getRating() == null ? 0 : po.getRating();
            sum += rating;
            if (rating == 1) r1++;
            else if (rating == 2) r2++;
            else if (rating == 3) r3++;
            else if (rating == 4) r4++;
            else if (rating == 5) r5++;
        }
        double avg = total == 0 ? 0.0
                : BigDecimal.valueOf(sum)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                        .doubleValue();
        return new CommentStatsVO(total, avg, r5, r4, r3, r2, r1);
    }

    private CommentPO findExisting(Long userId, String orderNo, Long spuId) {
        LambdaQueryWrapper<CommentPO> wrapper = Wrappers.lambdaQuery(CommentPO.class);
        wrapper.eq(CommentPO::getUserId, userId);
        wrapper.eq(CommentPO::getOrderNo, orderNo);
        wrapper.eq(CommentPO::getSpuId, spuId);
        return commentManager.getOne(wrapper);
    }
}
