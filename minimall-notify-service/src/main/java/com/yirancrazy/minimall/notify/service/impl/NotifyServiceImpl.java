package com.yirancrazy.minimall.notify.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotifyManager notifyManager;
    private final NotifyPushGateway notifyPushGateway;

    /**
     * 构造 NotifyServiceImpl 实例。
     * @param notifyManager notifyManager 参数
     * @param notifyPushGateway notifyPushGateway 参数
     */
    public NotifyServiceImpl(NotifyManager notifyManager, NotifyPushGateway notifyPushGateway) {
        this.notifyManager = notifyManager;
        this.notifyPushGateway = notifyPushGateway;
    }


    /**
     * 执行 push 方法。
     * @param userId userId 参数
     * @param title title 参数
     * @param content content 参数
     * @return 是否处理成功
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
            notifyPushGateway.send(userId, title);
        }
        return saved;
    }


    /**
     * 查询列表数据。
     * @param dto dto 参数
     * @return 结果列表
     */
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
     * 查询列表数据。
     * @param dto dto 参数
     * @return 结果列表
     */
    @Override
    public List<NotifyMessageVO> listByUserVO(NotifyListDTO dto) {
        return listByUser(dto).stream().map(NotifyMessageVO::from).toList();
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<NotifyMessagePO> page(NotifyListDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<NotifyMessagePO> wrapper = Wrappers.lambdaQuery(NotifyMessagePO.class);
        if (dto.getRecipientType() != null) {
            // recipientType 过滤并入 or 分支：Controller 固定传 USER/MERCHANT/PLATFORM，若独立 AND 会把公告(ALL)挡掉
            wrapper.and(w -> w.eq(NotifyMessagePO::getUserId, dto.getUserId())
                .eq(NotifyMessagePO::getRecipientType, dto.getRecipientType())
                .or(q -> q.eq(NotifyMessagePO::getUserId, 0L)
                    .eq(NotifyMessagePO::getRecipientType, RecipientTypeEnum.ALL.intCode())));
        }
        else {
            wrapper.and(w -> w.eq(NotifyMessagePO::getUserId, dto.getUserId())
                .or(q -> q.eq(NotifyMessagePO::getUserId, 0L)
                    .eq(NotifyMessagePO::getRecipientType, RecipientTypeEnum.ALL.intCode())));
        }
        wrapper.lt(lastId != null, NotifyMessagePO::getId, lastId);
        if (dto.getMessageType() != null) {
            wrapper.eq(NotifyMessagePO::getMessageType, dto.getMessageType());
        }
        if (dto.getReadFlag() != null) {
            wrapper.eq(NotifyMessagePO::getReadFlag, dto.getReadFlag());
        }
        wrapper.orderByDesc(NotifyMessagePO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<NotifyMessagePO> records = notifyManager.list(wrapper);
        return CursorPageVO.of(records, limit, NotifyMessagePO::getId);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<NotifyMessageVO> pageVO(NotifyListDTO dto) {
        return page(dto).map(NotifyMessageVO::from);
    }


    /**
     * 执行 unreadCount 方法。
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     * @return 业务数值
     */
    @Override
    public long unreadCount(Integer recipientType, Long userId) {
        return notifyManager.count(Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, recipientType)
            .eq(NotifyMessagePO::getUserId, userId)
            .eq(NotifyMessagePO::getReadFlag, 0));
    }


    /**
     * 执行 markRead 方法。
     * @param id id 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    public void markRead(Long id, Integer recipientType, Long userId) {
        NotifyMessagePO m = getOwnedMessage(id, recipientType, userId);
        if (Integer.valueOf(1).equals(m.getReadFlag())) {
            return;
        }
        m.setReadFlag(1);
        notifyManager.updateById(m);
    }


    /**
     * 执行 markAllRead 方法。
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    public void markAllRead(Integer recipientType, Long userId) {
        notifyManager.update(Wrappers.lambdaUpdate(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, recipientType)
            .eq(NotifyMessagePO::getUserId, userId)
            .eq(NotifyMessagePO::getReadFlag, 0)
            .set(NotifyMessagePO::getReadFlag, 1));
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    public void delete(Long id, Integer recipientType, Long userId) {
        getOwnedMessage(id, recipientType, userId);
        notifyManager.removeById(id);
    }


    /**
     * 执行 batchDelete 方法。
     * @param ids ids 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids, Integer recipientType, Long userId) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(NotifyCodeEnum.NOTIFY_BATCH_IDS_EMPTY);
        }
        if (ids.size() > 100) {
            throw new BizException(NotifyCodeEnum.NOTIFY_BATCH_TOO_MANY);
        }
        // 按 ID 查询全部消息
        List<NotifyMessagePO> messages = notifyManager.listByIds(ids);
        // 校验全部消息均属于当前用户
        for (NotifyMessagePO m : messages) {
            if (!recipientType.equals(m.getRecipientType()) || !userId.equals(m.getUserId())) {
                throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
            }
        }
        // 检查是否找到全部 ID
        if (messages.size() != ids.size()) {
            throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
        }
        // 批量软删除
        notifyManager.removeByIds(ids);
        log.info("消息已批量删除，count={}，userId={}", ids.size(), userId);
    }


    /**
     * 执行 broadcast 方法。
     * @param dto dto 参数
     */
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
            notifyPushGateway.send(dto.getTargetId(), dto.getTitle());
        }
        else {
            // 全端广播：落库一条 userId=0 的占位记录，按需扩展批量下发
            m.setUserId(0L);
            notifyManager.save(m);
        }
        log.info("广播通知已发送，recipientType={}，targetId={}",
            dto.getRecipientType(), dto.getTargetId());
    }


    /**
     * 执行 marketingPush 方法。
     * @param dto dto 参数
     */
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
            List<NotifyMessagePO> messages = new ArrayList<>(dto.getUserIds().size());
            for (Long uid : dto.getUserIds()) {
                NotifyMessagePO m = new NotifyMessagePO();
                m.setUserId(uid);
                m.setRecipientType(RecipientTypeEnum.USER.intCode());
                m.setMessageType(NotifyMessageTypeEnum.PROMOTION.intCode());
                m.setTitle(dto.getTitle());
                m.setContent(dto.getContent());
                m.setReadFlag(0);
                messages.add(m);
            }
            notifyManager.saveBatch(messages);
            for (Long uid : dto.getUserIds()) {
                notifyPushGateway.send(uid, dto.getTitle());
            }
        }
        log.info("营销消息已推送，userIds={}", dto.getUserIds());
    }


    /**
     * 执行 violationWarning 方法。
     * @param dto dto 参数
     */
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
        notifyPushGateway.send(dto.getMerchantId(), dto.getTitle());
        log.info("违规警告已发送，merchantId={}，type={}",
            dto.getMerchantId(), dto.getViolationType());
    }


    /**
     * 执行 systemAlert 方法。
     * @param dto dto 参数
     */
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
        log.info("系统告警已发送，title={}，level={}", dto.getTitle(), dto.getAlertLevel());
    }


    /**
     * 执行 alertPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<NotifyMessagePO> alertPage(SystemAlertPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<NotifyMessagePO> wrapper = Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getRecipientType, RecipientTypeEnum.PLATFORM.intCode())
            .eq(NotifyMessagePO::getMessageType, NotifyMessageTypeEnum.SYSTEM.intCode())
            .lt(lastId != null, NotifyMessagePO::getId, lastId)
            .orderByDesc(NotifyMessagePO::getId)
            .last("LIMIT " + (limit + 1));
        List<NotifyMessagePO> records = notifyManager.list(wrapper);
        return CursorPageVO.of(records, limit, NotifyMessagePO::getId);
    }


    /**
     * 执行 alertPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<NotifyMessageVO> alertPageVO(SystemAlertPageDTO dto) {
        return alertPage(dto).map(NotifyMessageVO::from);
    }


    /**
     * 执行 publishAnnouncement 方法。
     * @param dto dto 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(AnnouncementCreateDTO dto) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(0L);
        m.setRecipientType(RecipientTypeEnum.ALL.intCode());
        m.setMessageType(NotifyMessageTypeEnum.ANNOUNCEMENT.intCode());
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setReadFlag(0);
        notifyManager.save(m);
        log.info("公告已发布，title={}", dto.getTitle());
    }

    /**
     * 获取Owned消息。
     * @param id ID
     * @param recipientType recipientType
     * @param userId 用户 ID
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
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
