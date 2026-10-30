package com.yirancrazy.minimall.order.manager;

import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.entity.OutboxPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OutboxManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OutboxManager extends IService<OutboxPO> {

    /**
     * 查询待发布事件。
     *
     * @param now 当前时间
     * @param limit 单批数量
     * @return 待发布事件
     */
    List<OutboxPO> listDispatchCandidates(LocalDateTime now, int limit);

    /**
     * 抢占待发布事件。
     *
     * @param id 事件 ID
     * @param now 当前时间
     * @param leaseUntil 发送租约到期时间
     * @return 是否抢占成功
     */
    boolean claimForDispatch(Long id, LocalDateTime now, LocalDateTime leaseUntil);

    /**
     * 标记事件发送成功。
     *
     * @param id 事件 ID
     * @param sentTime 发送成功时间
     */
    void markSent(Long id, LocalDateTime sentTime);

    /**
     * 标记事件等待重试。
     *
     * @param id 事件 ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @param lastError 最近一次错误
     */
    void markRetry(Long id, int retryCount, LocalDateTime nextRetryTime, String lastError);

    /**
     * 标记事件为死信。
     *
     * @param id 事件 ID
     * @param retryCount 重试次数
     * @param lastError 最近一次错误
     */
    void markDead(Long id, int retryCount, String lastError);
}
