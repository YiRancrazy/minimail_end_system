package com.yirancrazy.minimall.notify.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知领域服务实现，实现站内信发送、分页查询、已读标记、删除与广播
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotifyManager notifyManager;
    private final SseHub sseHub;

    public NotifyServiceImpl(NotifyManager notifyManager, SseHub sseHub) {
        this.notifyManager = notifyManager;
        this.sseHub = sseHub;
    }

    /**
     * 构造通知实体并写入数据库，默认接收方为 USER、消息类型为 SYSTEM，落库后通过 SSE 推送。
     *
     * @param userId  接收用户主键
     * @param title   通知标题
     * @param content 通知正文
     * @return 入库是否成功
     */
    @Override
    public boolean push(Long userId, String title, String content) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(userId);
        m.setRecipientType(RecipientTypeEnum.USER.intCode());
        m.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
        m.setTitle(title);
        m.setContent(content);
        m.setReadFlag(0);
        boolean saved = notifyManager.save(m);
        if (saved) {
            sseHub.send(userId, title);
        }
        return saved;
    }

    /**
     * 查询指定用户全部通知消息，按ID降序返回。
     *
     * @param dto 查询条件
     * @return 通知消息列表
     */
    @Override
    public List<NotifyMessagePO> listByUser(NotifyListDTO dto) {
        LambdaQueryWrapper<NotifyMessagePO> wrapper = Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getUserId, dto.getUserId());
        if (dto.getRecipientType() != null) {
            wrapper.eq(NotifyMessagePO::getRecipientType, dto.getRecipientType());
        }
        wrapper.orderByDesc(NotifyMessagePO::getId);
        return notifyManager.list(wrapper);
    }

    /**
     * 分页查询站内信，支持按接收方/消息类型/已读状态过滤，按ID降序返回。
     * @param dto 分页查询入参
     * @return 站内信分页结果
     */
    @Override
    public IPage<NotifyMessagePO> page(NotifyListDTO dto) {
        Page<NotifyMessagePO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<NotifyMessagePO> wrapper = Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getUserId, dto.getUserId());
        if (dto.getRecipientType() != null) {
            wrapper.eq(NotifyMessagePO::getRecipientType, dto.getRecipientType());
        }
        if (dto.getMessageType() != null) {
            wrapper.eq(NotifyMessagePO::getMessageType, dto.getMessageType());
        }
        if (dto.getReadFlag() != null) {
            wrapper.eq(NotifyMessagePO::getReadFlag, dto.getReadFlag());
        }
        wrapper.orderByDesc(NotifyMessagePO::getId);
        return notifyManager.page(page, wrapper);
    }

    /**
     * 统计指定接收方的未读消息数量。
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @return 未读消息数
     */
    @Override
    public long unreadCount(Integer recipientType, Long userId) {
        return notifyManager.count(Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, recipientType)
            .eq(NotifyMessagePO::getUserId, userId)
            .eq(NotifyMessagePO::getReadFlag, 0));
    }

    /**
     * 标记单条消息为已读，校验归属权限；已读则幂等返回。
     * @param id 消息ID
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws BizException 消息不存在或无权操作时
     */
    @Override
    public void markRead(Long id, Integer recipientType, Long userId) {
        NotifyMessagePO m = getOwnedMessage(id, recipientType, userId);
        if (Integer.valueOf(1).equals(m.getReadFlag())) {
            return;
        }
        m.setReadFlag(1);
        notifyManager.updateById(m);
    }

    /**
     * 将指定接收方的全部未读消息标记为已读。
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     */
    @Override
    public void markAllRead(Integer recipientType, Long userId) {
        notifyManager.update(Wrappers.lambdaUpdate(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, recipientType)
            .eq(NotifyMessagePO::getUserId, userId)
            .eq(NotifyMessagePO::getReadFlag, 0)
            .set(NotifyMessagePO::getReadFlag, 1));
    }

    /**
     * 删除单条消息（软删除），校验归属权限。
     * @param id 消息ID
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws BizException 消息不存在或无权操作时
     */
    @Override
    public void delete(Long id, Integer recipientType, Long userId) {
        getOwnedMessage(id, recipientType, userId);
        notifyManager.removeById(id);
    }

    /**
     * 批量删除消息（软删除），校验所有消息归属当前用户。
     * @param ids 消息ID列表，最多100条
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws BizException 当ID列表为空、超限或存在非本人消息时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids, Integer recipientType, Long userId) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(NotifyCodeEnum.NOTIFY_BATCH_IDS_EMPTY);
        }
        if (ids.size() > 100) {
            throw new BizException(NotifyCodeEnum.NOTIFY_BATCH_TOO_MANY);
        }
        // Query all messages by IDs
        List<NotifyMessagePO> messages = notifyManager.listByIds(ids);
        // Validate all messages belong to the user
        for (NotifyMessagePO m : messages) {
            if (!recipientType.equals(m.getRecipientType()) || !userId.equals(m.getUserId())) {
                throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
            }
        }
        // Check if all IDs were found
        if (messages.size() != ids.size()) {
            throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
        }
        // Batch soft delete
        notifyManager.removeByIds(ids);
        log.info("batch deleted messages, count={}, userId={}", ids.size(), userId);
    }

    /**
     * 平台广播消息，落库并通过 SSE 实时推送；指定 targetId 时定向发送，否则全端广播占位。
     * @param dto 广播入参
     * @throws BizException 接收方类型非法时
     */
    @Override
    public void broadcast(NotifyBroadcastDTO dto) {
        if (dto.getRecipientType() == null
            || dto.getRecipientType() < 1
            || dto.getRecipientType() > RecipientTypeEnum.values().length) {
            throw new BizException(NotifyCodeEnum.BROADCAST_RECIPIENT_INVALID);
        }
        NotifyMessagePO m = new NotifyMessagePO();
        m.setRecipientType(dto.getRecipientType());
        m.setMessageType(dto.getMessageType());
        m.setSenderId(dto.getSenderId());
        m.setBizId(dto.getBizId());
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setReadFlag(0);
        if (dto.getTargetId() != null) {
            m.setUserId(dto.getTargetId());
            notifyManager.save(m);
            sseHub.send(dto.getTargetId(), dto.getTitle());
        }
        else {
            // 全端广播：落库一条 userId=0 的占位记录，按需扩展批量下发
            m.setUserId(0L);
            notifyManager.save(m);
        }
        log.info("broadcast notify, recipientType={}, targetId={}",
            dto.getRecipientType(), dto.getTargetId());
    }

    /**
     * 营销推送，向指定用户列表发送营销站内信；userIds 为 null 或空时全量广播（落库 userId=0 占位）。
     * @param dto 营销推送入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void marketingPush(NotifyMarketingPushDTO dto) {
        if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
            NotifyMessagePO m = new NotifyMessagePO();
            m.setUserId(0L);
            m.setRecipientType(RecipientTypeEnum.USER.intCode());
            m.setMessageType(NotifyMessageTypeEnum.PROMOTION.intCode());
            m.setTitle(dto.getTitle());
            m.setContent(dto.getContent());
            m.setReadFlag(0);
            notifyManager.save(m);
        }
        else {
            for (Long uid : dto.getUserIds()) {
                NotifyMessagePO m = new NotifyMessagePO();
                m.setUserId(uid);
                m.setRecipientType(RecipientTypeEnum.USER.intCode());
                m.setMessageType(NotifyMessageTypeEnum.PROMOTION.intCode());
                m.setTitle(dto.getTitle());
                m.setContent(dto.getContent());
                m.setReadFlag(0);
                notifyManager.save(m);
                sseHub.send(uid, dto.getTitle());
            }
        }
        log.info("marketing push, userIds={}", dto.getUserIds());
    }

    /**
     * 违规警告通知，向指定商家发送违规警告站内信并 SSE 推送。
     * @param dto 违规警告入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void violationWarning(NotifyViolationWarningDTO dto) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(dto.getMerchantId());
        m.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
        m.setMessageType(NotifyMessageTypeEnum.VIOLATION.intCode());
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setReadFlag(0);
        notifyManager.save(m);
        sseHub.send(dto.getMerchantId(), dto.getTitle());
        log.info("violation warning, merchantId={}, type={}",
            dto.getMerchantId(), dto.getViolationType());
    }

    /**
     * 系统告警通知，向平台管理员发送系统异常/故障预警站内信。
     * @param dto 系统告警入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void systemAlert(NotifySystemAlertDTO dto) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(0L);
        m.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        m.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setReadFlag(0);
        notifyManager.save(m);
        log.info("system alert, title={}, level={}", dto.getTitle(), dto.getAlertLevel());
    }

    /**
     * 平台系统告警分页查询，固定过滤 recipient_type=PLATFORM, message_type=SYSTEM，按 ID 降序返回。
     * @param dto 分页入参
     * @return 系统告警分页结果
     */
    @Override
    public IPage<NotifyMessagePO> alertPage(SystemAlertPageDTO dto) {
        Page<NotifyMessagePO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<NotifyMessagePO> wrapper = Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, RecipientTypeEnum.PLATFORM.intCode())
            .eq(NotifyMessagePO::getMessageType, NotifyMessageTypeEnum.SYSTEM.intCode())
            .orderByDesc(NotifyMessagePO::getId);
        return notifyManager.page(page, wrapper);
    }

    private NotifyMessagePO getOwnedMessage(Long id, Integer recipientType, Long userId) {
        NotifyMessagePO m = notifyManager.getById(id);
        if (m == null
            || !recipientType.equals(m.getRecipientType())
            || !userId.equals(m.getUserId())) {
            throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
        }
        return m;
    }
}
