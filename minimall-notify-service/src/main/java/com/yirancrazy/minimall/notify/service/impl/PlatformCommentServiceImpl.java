package com.yirancrazy.minimall.notify.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台评价管理服务实现，封装审核（通过/隐藏）与平台回复业务
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Service
public class PlatformCommentServiceImpl implements PlatformCommentService {

    private final CommentManager commentManager;

    public PlatformCommentServiceImpl(CommentManager commentManager) {
        this.commentManager = commentManager;
    }

    /**
     * 分页查询评价，按 ID 降序返回，支持状态/订单号过滤；游标解码后按 id < lastId 取下一页。
     * @param dto 分页入参
     * @return 评价游标分页结果
     */
    @Override
    public CursorPageVO<CommentPO> page(CommentPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<CommentPO> wrapper = Wrappers.lambdaQuery(CommentPO.class);
        wrapper.lt(lastId != null, CommentPO::getId, lastId);
        if (dto.getStatus() != null) {
            wrapper.eq(CommentPO::getStatus, dto.getStatus());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.like(CommentPO::getOrderNo, dto.getOrderNo());
        }
        wrapper.orderByDesc(CommentPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        return CursorPageVO.of(commentManager.list(wrapper), limit, CommentPO::getId);
    }

    /**
     * 通过评价：将状态置为 NORMAL。已 NORMAL 的评价为幂等操作。
     * @param id 评价ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        CommentPO po = mustGet(id);
        if (po.getStatus() != null && po.getStatus() == CommentStatusEnum.NORMAL.intCode()) {
            return;
        }
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        commentManager.updateById(po);
    }

    /**
     * 隐藏评价：将状态置为 HIDDEN。允许从 NORMAL 隐藏回 HIDDEN。
     * @param id 评价ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hide(Long id) {
        CommentPO po = mustGet(id);
        po.setStatus(CommentStatusEnum.HIDDEN.intCode());
        commentManager.updateById(po);
    }

    /**
     * 平台回复评价：写入回复内容与当前时间；不影响商家后续再次回复。
     * @param id 评价ID
     * @param reply 回复内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String reply) {
        CommentPO po = mustGet(id);
        po.setMerchantReply(reply);
        po.setMerchantReplyTime(java.time.LocalDateTime.now());
        commentManager.updateById(po);
    }

    private CommentPO mustGet(Long id) {
        CommentPO po = commentManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMMENT_NOT_FOUND);
        }
        return po;
    }
}
