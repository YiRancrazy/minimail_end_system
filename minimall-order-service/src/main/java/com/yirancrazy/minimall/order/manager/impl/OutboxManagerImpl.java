package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.mapper.OutboxMapper;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现 OutboxManager，通过 MyBatis-Plus 访问OutboxPo。
 *   业务边界：作为 @Manager Bean 只处理持久化细节，不承载跨服务事务和业务状态机。
 *   使用场景：由订单服务或补偿任务依赖 base 接口调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class OutboxManagerImpl extends ServiceImpl<OutboxMapper, OutboxPo>
        implements OutboxManager {

    /**
     * 查询当前可发布的发件箱事件。
     *
     * <p>条件为 PENDING、payload 非空、nextRetryTime 已到期，并按 ID 升序限制批次。
     *
     * @param now 当前时间，用于判断候选是否到期
     * @param limit 单批最大处理数量
     * @return 可抢占的发件箱记录。
     */
    @Override
    public List<OutboxPo> listDispatchCandidates(LocalDateTime now, int limit) {
        return list(
                Wrappers.lambdaQuery(OutboxPo.class)
                        .eq(OutboxPo::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
                        .isNotNull(OutboxPo::getPayload)
                        .le(OutboxPo::getNextRetryTime, now)
                        .orderByAsc(OutboxPo::getId)
                        .last("LIMIT " + limit));
    }

    /**
     * 以数据库条件更新抢占一条发件箱事件。
     *
     * <p>仅当记录仍为 PENDING、payload 存在且已到重试时间时，把 nextRetryTime 更新为租约到期时间。
     *
     * @param id 发件箱记录主键
     * @param now 当前时间，用于判断候选是否到期
     * @param leaseUntil 本次发送租约到期时间，防止并发重复抢占
     * @return 更新恰好一行时返回 true，否则表示其他实例已抢占。
     */
    @Override
    public boolean claimForDispatch(Long id, LocalDateTime now, LocalDateTime leaseUntil) {
        int updated =
                baseMapper.update(
                        null,
                        Wrappers.lambdaUpdate(OutboxPo.class)
                                .eq(OutboxPo::getId, id)
                                .eq(OutboxPo::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
                                .isNotNull(OutboxPo::getPayload)
                                .le(OutboxPo::getNextRetryTime, now)
                                .set(OutboxPo::getNextRetryTime, leaseUntil)
                                .set(OutboxPo::getUpdateTime, now));
        return updated == 1;
    }

    /**
     * 将待发送发件箱事件标记为已发送。
     *
     * <p>仅更新仍为 PENDING 的记录，写入发送时间和清空最近错误。
     *
     * @param id 发件箱记录主键
     * @param sentTime 发送成功时间
     */
    @Override
    public void markSent(Long id, LocalDateTime sentTime) {
        baseMapper.update(
                null,
                Wrappers.lambdaUpdate(OutboxPo.class)
                        .eq(OutboxPo::getId, id)
                        .eq(OutboxPo::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
                        .set(OutboxPo::getSendStatus, OutboxSendStatusEnum.SENT.intCode())
                        .set(OutboxPo::getSentTime, sentTime)
                        .set(OutboxPo::getLastError, null)
                        .set(OutboxPo::getUpdateTime, sentTime));
    }

    /**
     * 记录发件箱事件的失败信息并安排下次重试。
     *
     * <p>保持 PENDING，更新重试次数、nextRetryTime、lastError 和更新时间。
     *
     * @param id 发件箱记录主键
     * @param retryCount 失败后的累计重试次数
     * @param nextRetryTime 下次允许重试的时间
     * @param lastError 最近一次失败摘要，持久化前会截断
     */
    @Override
    public void markRetry(Long id, int retryCount, LocalDateTime nextRetryTime, String lastError) {
        baseMapper.update(
                null,
                Wrappers.lambdaUpdate(OutboxPo.class)
                        .eq(OutboxPo::getId, id)
                        .eq(OutboxPo::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
                        .set(OutboxPo::getRetryCount, retryCount)
                        .set(OutboxPo::getNextRetryTime, nextRetryTime)
                        .set(OutboxPo::getLastError, lastError)
                        .set(OutboxPo::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 将发件箱事件推进为死信。
     *
     * <p>仅处理仍为 PENDING 的记录，写入最终重试次数和错误信息，后续扫描不再选取。
     *
     * @param id 发件箱记录主键
     * @param retryCount 失败后的累计重试次数
     * @param lastError 最近一次失败摘要，持久化前会截断
     */
    @Override
    public void markDead(Long id, int retryCount, String lastError) {
        LocalDateTime now = LocalDateTime.now();
        baseMapper.update(
                null,
                Wrappers.lambdaUpdate(OutboxPo.class)
                        .eq(OutboxPo::getId, id)
                        .eq(OutboxPo::getSendStatus, OutboxSendStatusEnum.PENDING.intCode())
                        .set(OutboxPo::getSendStatus, OutboxSendStatusEnum.DEAD.intCode())
                        .set(OutboxPo::getRetryCount, retryCount)
                        .set(OutboxPo::getLastError, lastError)
                        .set(OutboxPo::getUpdateTime, now));
    }
}
