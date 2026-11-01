package com.yirancrazy.minimall.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台侧评价查询、审核恢复、隐藏和回复能力；所有写操作在事务内完成，缺失评价统一转换为业务异常.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Service
public class PlatformCommentServiceImpl implements PlatformCommentService {

    private final CommentManager commentManager;
    private final MinioUtil minioUtil;

    /**
     * 注入评价数据访问和图片地址转换依赖.
     *
     * @param commentManager 评价记录持久化入口
     * @param minioUtil 评价图片对象键转公开 URL 的工具
     */
    public PlatformCommentServiceImpl(CommentManager commentManager, MinioUtil minioUtil) {
        this.commentManager = commentManager;
        this.minioUtil = minioUtil;
    }

    /**
     * 使用主键游标倒序查询平台评价，并支持状态和订单号模糊匹配.
     *
     * @param dto 游标及平台评价筛选条件
     * @return 当前页评价实体及下一页游标
     */
    public CursorPageVo<CommentPo> page(CommentPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<CommentPo> wrapper = Wrappers.lambdaQuery(CommentPo.class);
        wrapper.lt(lastId != null, CommentPo::getId, lastId);
        if (dto.getStatus() != null) {
            wrapper.eq(CommentPo::getStatus, dto.getStatus());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.like(CommentPo::getOrderNo, dto.getOrderNo());
        }
        wrapper.orderByDesc(CommentPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        return CursorPageVo.of(commentManager.list(wrapper), limit, CommentPo::getId);
    }

    /**
     * 将平台评价查询结果转换为视图，并解析图片对象键对应的公开地址.
     *
     * @param dto 游标及平台评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    @Override
    public CursorPageVo<CommentVo> pageVo(CommentPageDto dto) {
        return page(dto).map(po -> CommentVo.from(po, minioUtil));
    }

    /**
     * 在事务内将评价恢复为正常状态；已经是正常状态时不执行更新.
     *
     * @param id 评价主键
     * @throws BizException 评价不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        CommentPo po = mustGet(id);
        if (po.getStatus() != null && po.getStatus() == CommentStatusEnum.NORMAL.intCode()) {
            return;
        }
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        commentManager.updateById(po);
    }

    /**
     * 在事务内将评价状态改为已隐藏.
     *
     * @param id 评价主键
     * @throws BizException 评价不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void hide(Long id) {
        CommentPo po = mustGet(id);
        po.setStatus(CommentStatusEnum.HIDDEN.intCode());
        commentManager.updateById(po);
    }

    /**
     * 在事务内写入平台回复正文和当前回复时间.
     *
     * @param id 评价主键
     * @param reply 平台回复正文
     * @throws BizException 评价不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String reply) {
        CommentPo po = mustGet(id);
        po.setMerchantReply(reply);
        po.setMerchantReplyTime(java.time.LocalDateTime.now());
        commentManager.updateById(po);
    }

    /**
     * 查询评价并在记录不存在时抛出业务异常.
     *
     * @param id 评价主键
     * @return 已存在的评价实体
     * @throws BizException 评价不存在时抛出
     */
    private CommentPo mustGet(Long id) {
        CommentPo po = commentManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMMENT_NOT_FOUND);
        }
        return po;
    }
}
