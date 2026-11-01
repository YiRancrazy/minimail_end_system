package com.yirancrazy.minimall.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDto;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVo;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 负责评价创建资格校验、并发重复防护、评价游标查询、商家回复和评分统计；依赖订单服务确认购买关系，依赖 Minio 生成图片访问地址.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentManager commentManager;
    private final CommentRemoteGateway commentRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 注入评价数据访问、订单资格校验和对象存储地址转换依赖.
     *
     * @param commentManager 评价记录持久化入口
     * @param commentRemoteGateway 订单服务评价资格查询入口
     * @param minioUtil 评价图片对象键转公开 URL 的工具
     */
    public CommentServiceImpl(
            CommentManager commentManager,
            CommentRemoteGateway commentRemoteGateway,
            MinioUtil minioUtil) {
        this.commentManager = commentManager;
        this.commentRemoteGateway = commentRemoteGateway;
        this.minioUtil = minioUtil;
    }

    /**
     * 校验订单商品可评价性和重复记录后创建正常状态评价；商家编号以订单服务的资格结果为准.
     *
     * @param userId 发起评价的用户编号
     * @param dto 评价订单、SPU、SKU、评分、内容、图片和匿名标记
     * @return 新评价主键
     * @throws BizException 订单不可评价、订单服务不可用、预查重复或数据库唯一键拒绝重复时抛出
     */
    @Override
    public Long create(Long userId, CommentCreateDto dto) {
        OrderCommentEligibilityDto eligibility = loadEligibility(userId, dto);
        if (!eligibility.isEligible()) {
            log.warn(
                    "评价已拒绝，订单不符合条件，userId={}，orderNo={}，spuId={}", userId, dto.getOrderNo(), dto.getSpuId());
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_NOT_PURCHASED);
        }
        CommentPo existing = findExisting(userId, dto.getOrderNo(), dto.getSpuId());
        if (existing != null) {
            throw new BizException(NotifyCodeEnum.COMMENT_ALREADY_EXISTS);
        }
        CommentPo po = new CommentPo();
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
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发重复提交时仅一条落库，其余转为明确业务错误
            log.warn(
                    "唯一索引拒绝重复评价，userId={}，orderNo={}，spuId={}", userId, dto.getOrderNo(), dto.getSpuId());
            throw new BizException(NotifyCodeEnum.COMMENT_ALREADY_EXISTS);
        }
        log.info(
                "评价已创建，id={}，userId={}，spuId={}，orderNo={}",
                po.getId(),
                userId,
                dto.getSpuId(),
                dto.getOrderNo());
        return po.getId();
    }

    /**
     * 调用订单服务加载评价资格，并把远程异常、空响应或非成功响应统一转换为订单服务不可用错误.
     *
     * @param userId 发起评价的用户编号
     * @param dto 包含订单号和 SPU 的评价请求
     * @return 订单服务返回的评价资格数据
     * @throws BizException 远程调用异常或响应不满足成功条件时抛出
     */
    private OrderCommentEligibilityDto loadEligibility(Long userId, CommentCreateDto dto) {
        Result<OrderCommentEligibilityDto> result;
        try {
            result = commentRemoteGateway.commentEligibility(dto.getOrderNo(), userId, dto.getSpuId());
        } catch (Exception e) {
            log.error("订单评价资格查询失败，orderNo={}，userId={}", dto.getOrderNo(), userId, e);
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE);
        }
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            log.warn("订单评价资格服务不可用，orderNo={}，result={}", dto.getOrderNo(), result);
            throw new BizException(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE);
        }
        return result.getData();
    }

    /**
     * 使用主键游标倒序查询评价实体，并组合 SPU、用户、商家、订单号和状态过滤条件.
     *
     * @param dto 游标及评价筛选条件
     * @return 当前页评价实体及下一页游标
     */
    public CursorPageVo<CommentPo> page(CommentPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<CommentPo> wrapper = Wrappers.lambdaQuery(CommentPo.class);
        wrapper.lt(lastId != null, CommentPo::getId, lastId);
        if (dto.getSpuId() != null) {
            wrapper.eq(CommentPo::getSpuId, dto.getSpuId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(CommentPo::getUserId, dto.getUserId());
        }
        if (dto.getMerchantId() != null) {
            wrapper.eq(CommentPo::getMerchantId, dto.getMerchantId());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.eq(CommentPo::getOrderNo, dto.getOrderNo());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(CommentPo::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(CommentPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<CommentPo> records = commentManager.list(wrapper);
        return CursorPageVo.of(records, limit, CommentPo::getId);
    }

    /**
     * 将游标查询得到的评价实体转换为视图，并把逗号分隔的图片对象键解析为公开地址.
     *
     * @param dto 游标及评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    @Override
    public CursorPageVo<CommentVo> pageVo(CommentPageDto dto) {
        return page(dto).map(po -> CommentVo.from(po, minioUtil));
    }

    /**
     * 在事务内校验评价存在且属于当前商家，再写入回复正文和回复时间.
     *
     * @param id 评价主键
     * @param merchantId 当前商家编号
     * @param reply 商家回复正文
     * @throws BizException 评价不存在或商家不匹配时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, Long merchantId, String reply) {
        CommentPo po = commentManager.getById(id);
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
     * 统计指定 SPU 正常评价的总数、平均分和 1 至 5 分数量；平均分按两位小数四舍五入.
     *
     * @param spuId 商品 SPU 编号
     * @return 评分统计视图；无正常评价时各字段为零
     */
    @Override
    public CommentStatsVo stats(Long spuId) {
        List<CommentRatingCountDto> rows =
                commentManager.countGroupByRating(spuId, CommentStatusEnum.NORMAL.intCode());
        long total = 0;
        long sum = 0;
        long r1 = 0;
        long r2 = 0;
        long r3 = 0;
        long r4 = 0;
        long r5 = 0;
        for (CommentRatingCountDto row : rows) {
            int rating = row.getRating();
            long cnt = row.getCnt();
            total += cnt;
            sum += (long) rating * cnt;
            if (rating == 1) {
                r1 = cnt;
            } else if (rating == 2) {
                r2 = cnt;
            } else if (rating == 3) {
                r3 = cnt;
            } else if (rating == 4) {
                r4 = cnt;
            } else if (rating == 5) {
                r5 = cnt;
            }
        }
        double avg =
                total == 0
                        ? 0.0
                        : BigDecimal.valueOf(sum)
                                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                                .doubleValue();
        return new CommentStatsVo(total, avg, r5, r4, r3, r2, r1);
    }

    /**
     * 按用户、订单号和 SPU 查询已存在的评价，用于在写入前返回明确重复错误.
     *
     * @param userId 用户编号
     * @param orderNo 订单编号
     * @param spuId 商品 SPU 编号
     * @return 已存在评价，未找到时返回 null
     */
    private CommentPo findExisting(Long userId, String orderNo, Long spuId) {
        LambdaQueryWrapper<CommentPo> wrapper = Wrappers.lambdaQuery(CommentPo.class);
        wrapper.eq(CommentPo::getUserId, userId);
        wrapper.eq(CommentPo::getOrderNo, orderNo);
        wrapper.eq(CommentPo::getSpuId, spuId);
        return commentManager.getOne(wrapper);
    }
}
