package com.yirancrazy.minimall.order.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 OutboxPo 的持久化访问接口，用于订单发件箱事件。
 *   业务边界：继承 IService 提供基础 CRUD 和条件更新，不包含远程调用或跨聚合业务编排。
 *   使用场景：由订单服务及补偿任务调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OutboxManager extends IService<OutboxPo> {

    /**
     * 查询当前可发布的发件箱事件。
     *
     * <p>条件为 PENDING、payload 非空、nextRetryTime 已到期，并按 ID 升序限制批次。
     *
     * @param now 当前时间，用于判断候选是否到期
     * @param limit 单批最大处理数量
     * @return 可抢占的发件箱记录。
     */
    List<OutboxPo> listDispatchCandidates(LocalDateTime now, int limit);

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
    boolean claimForDispatch(Long id, LocalDateTime now, LocalDateTime leaseUntil);

    /**
     * 将待发送发件箱事件标记为已发送。
     *
     * <p>仅更新仍为 PENDING 的记录，写入发送时间和清空最近错误。
     *
     * @param id 发件箱记录主键
     * @param sentTime 发送成功时间
     */
    void markSent(Long id, LocalDateTime sentTime);

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
    void markRetry(Long id, int retryCount, LocalDateTime nextRetryTime, String lastError);

    /**
     * 将发件箱事件推进为死信。
     *
     * <p>仅处理仍为 PENDING 的记录，写入最终重试次数和错误信息，后续扫描不再选取。
     *
     * @param id 发件箱记录主键
     * @param retryCount 失败后的累计重试次数
     * @param lastError 最近一次失败摘要，持久化前会截断
     */
    void markDead(Long id, int retryCount, String lastError);
}
