package com.yirancrazy.minimall.order.manager.impl;

import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.mapper.OutboxMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OutboxManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class OutboxManagerImpl extends ServiceImpl<OutboxMapper, OutboxPO>
    implements OutboxManager {

    /**
     * 查询待发布事件。
     *
     * @param now 当前时间
     * @param limit 单批数量
     * @return 待发布事件
     */
    @Override
    public List<OutboxPO> listDispatchCandidates(LocalDateTime now, int limit) {
        return list(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
            .isNotNull(OutboxPO::getPayload)
            .le(OutboxPO::getNextRetryTime, now)
            .orderByAsc(OutboxPO::getId)
            .last("LIMIT " + limit));
    }

    /**
     * 抢占待发布事件。
     *
     * @param id 事件 ID
     * @param now 当前时间
     * @param leaseUntil 发送租约到期时间
     * @return 是否抢占成功
     */
    @Override
    public boolean claimForDispatch(Long id, LocalDateTime now, LocalDateTime leaseUntil) {
        int updated = baseMapper.update(null, Wrappers.lambdaUpdate(OutboxPO.class)
            .eq(OutboxPO::getId, id)
            .eq(OutboxPO::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
            .isNotNull(OutboxPO::getPayload)
            .le(OutboxPO::getNextRetryTime, now)
            .set(OutboxPO::getNextRetryTime, leaseUntil)
            .set(OutboxPO::getUpdateTime, now));
        return updated == 1;
    }

    /**
     * 标记事件发送成功。
     *
     * @param id 事件 ID
     * @param sentTime 发送成功时间
     */
    @Override
    public void markSent(Long id, LocalDateTime sentTime) {
        baseMapper.update(null, Wrappers.lambdaUpdate(OutboxPO.class)
            .eq(OutboxPO::getId, id)
            .eq(OutboxPO::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
            .set(OutboxPO::getSendStatus, OutboxSendStatusEnum.SENT.intCode())
            .set(OutboxPO::getSentTime, sentTime)
            .set(OutboxPO::getLastError, null)
            .set(OutboxPO::getUpdateTime, sentTime));
    }

    /**
     * 标记事件等待重试。
     *
     * @param id 事件 ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @param lastError 最近一次错误
     */
    @Override
    public void markRetry(Long id, int retryCount, LocalDateTime nextRetryTime, String lastError) {
        baseMapper.update(null, Wrappers.lambdaUpdate(OutboxPO.class)
            .eq(OutboxPO::getId, id)
            .eq(OutboxPO::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
            .set(OutboxPO::getRetryCount, retryCount)
            .set(OutboxPO::getNextRetryTime, nextRetryTime)
            .set(OutboxPO::getLastError, lastError)
            .set(OutboxPO::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 标记事件为死信。
     *
     * @param id 事件 ID
     * @param retryCount 重试次数
     * @param lastError 最近一次错误
     */
    @Override
    public void markDead(Long id, int retryCount, String lastError) {
        LocalDateTime now = LocalDateTime.now();
        baseMapper.update(null, Wrappers.lambdaUpdate(OutboxPO.class)
            .eq(OutboxPO::getId, id)
            .eq(OutboxPO::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
            .set(OutboxPO::getSendStatus, OutboxSendStatusEnum.DEAD.intCode())
            .set(OutboxPO::getRetryCount, retryCount)
            .set(OutboxPO::getLastError, lastError)
            .set(OutboxPO::getUpdateTime, now));
    }
}
