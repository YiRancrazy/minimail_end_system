package com.yirancrazy.minimall.notify.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentManager commentManager;
    private final CommentRemoteGateway commentRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 构造 CommentServiceImpl 实例。
     * @param commentManager commentManager 参数
     * @param commentRemoteGateway commentRemoteGateway 参数
     * @param minioUtil minioUtil 参数
     */
    public CommentServiceImpl(CommentManager commentManager, CommentRemoteGateway commentRemoteGateway,
                              MinioUtil minioUtil) {
        this.commentManager = commentManager;
        this.commentRemoteGateway = commentRemoteGateway;
        this.minioUtil = minioUtil;
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long create(Long userId, CommentCreateDTO dto) {
        OrderCommentEligibilityDTO eligibility = loadEligibility(userId, dto);
        if (!eligibility.isEligible()) {
            log.warn("评价已拒绝，订单不符合条件，userId={}，orderNo={}，spuId={}",
                userId, dto.getOrderNo(), dto.getSpuId());
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_NOT_PURCHASED);
        }
        CommentPO existing = findExisting(userId, dto.getOrderNo(), dto.getSpuId());
        if (existing != null) {
            throw new BizException(NotifyCodeEnum.COMMENT_ALREADY_EXISTS);
        }
        CommentPO po = new CommentPO();
        po.setOrderNo(dto.getOrderNo());
        po.setSpuId(dto.getSpuId());
        po.setSkuId(dto.getSkuId());
        po.setUserId(userId);
        // 商家归属以订单快照为准，忽略前端传入的 merchantId，避免评价挂到其他商家
        po.setMerchantId(eligibility.getMerchantId() == null ? 0L : eligibility.getMerchantId());
        po.setRating(dto.getRating());
        po.setContent(dto.getContent());
        po.setImages(dto.getImages());
        po.setAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        try {
            commentManager.save(po);
        }
        catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发重复提交时仅一条落库，其余转为明确业务错误
            log.warn("唯一索引拒绝重复评价，userId={}，orderNo={}，spuId={}",
                userId, dto.getOrderNo(), dto.getSpuId());
            throw new BizException(NotifyCodeEnum.COMMENT_ALREADY_EXISTS);
        }
        log.info("评价已创建，id={}，userId={}，spuId={}，orderNo={}",
                po.getId(), userId, dto.getSpuId(), dto.getOrderNo());
        return po.getId();
    }


    /**
     * 加载Eligibility。
     * @param userId 用户 ID
     * @param dto 请求参数
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
    private OrderCommentEligibilityDTO loadEligibility(Long userId, CommentCreateDTO dto) {
        Result<OrderCommentEligibilityDTO> result;
        try {
            result = commentRemoteGateway.commentEligibility(dto.getOrderNo(), userId, dto.getSpuId());
        }
        catch (Exception e) {
            log.error("订单评价资格查询失败，orderNo={}，userId={}",
                dto.getOrderNo(), userId, e);
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE);
        }
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            log.warn("订单评价资格服务不可用，orderNo={}，result={}", dto.getOrderNo(), result);
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE);
        }
        return result.getData();
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
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
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<CommentVO> pageVO(CommentPageDTO dto) {
        return page(dto).map(po -> CommentVO.from(po, minioUtil));
    }


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param reply reply 参数
     */
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
        log.info("评价已回复，id={}，merchantId={}", id, merchantId);
    }


    /**
     * 执行 stats 方法。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    @Override
    public CommentStatsVO stats(Long spuId) {
        List<CommentRatingCountDTO> rows = commentManager.countGroupByRating(
            spuId, CommentStatusEnum.NORMAL.intCode());
        long total = 0;
        long sum = 0;
        long r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0;
        for (CommentRatingCountDTO row : rows) {
            int rating = row.getRating();
            long cnt = row.getCnt();
            total += cnt;
            sum += (long) rating * cnt;
            if (rating == 1) {
                r1 = cnt;
            }
            else if (rating == 2) {
                r2 = cnt;
            }
            else if (rating == 3) {
                r3 = cnt;
            }
            else if (rating == 4) {
                r4 = cnt;
            }
            else if (rating == 5) {
                r5 = cnt;
            }
        }
        double avg = total == 0 ? 0.0
                : BigDecimal.valueOf(sum)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                        .doubleValue();
        return new CommentStatsVO(total, avg, r5, r4, r3, r2, r1);
    }

    /**
     * 加载Existing。
     * @param userId 用户 ID
     * @param orderNo 订单编号
     * @param spuId SPU ID
     * @return 查询结果
     */
    private CommentPO findExisting(Long userId, String orderNo, Long spuId) {
        LambdaQueryWrapper<CommentPO> wrapper = Wrappers.lambdaQuery(CommentPO.class);
        wrapper.eq(CommentPO::getUserId, userId);
        wrapper.eq(CommentPO::getOrderNo, orderNo);
        wrapper.eq(CommentPO::getSpuId, spuId);
        return commentManager.getOne(wrapper);
    }
}
