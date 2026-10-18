package com.yirancrazy.minimall.notify.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformCommentServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Service
public class PlatformCommentServiceImpl implements PlatformCommentService {

    private final CommentManager commentManager;
    private final MinioUtil minioUtil;

    /**
     * 构造 PlatformCommentServiceImpl 实例。
     * @param commentManager commentManager 参数
     * @param minioUtil minioUtil 参数
     */
    public PlatformCommentServiceImpl(CommentManager commentManager, MinioUtil minioUtil) {
        this.commentManager = commentManager;
        this.minioUtil = minioUtil;
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
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<CommentVO> pageVO(CommentPageDTO dto) {
        return page(dto).map(po -> CommentVO.from(po, minioUtil));
    }


    /**
     * 执行 approve 方法。
     * @param id id 参数
     */
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
     * 执行 hide 方法。
     * @param id id 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void hide(Long id) {
        CommentPO po = mustGet(id);
        po.setStatus(CommentStatusEnum.HIDDEN.intCode());
        commentManager.updateById(po);
    }


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param reply reply 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String reply) {
        CommentPO po = mustGet(id);
        po.setMerchantReply(reply);
        po.setMerchantReplyTime(java.time.LocalDateTime.now());
        commentManager.updateById(po);
    }

    /**
     * 处理must获取。
     * @param id ID
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private CommentPO mustGet(Long id) {
        CommentPO po = commentManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMMENT_NOT_FOUND);
        }
        return po;
    }
}
