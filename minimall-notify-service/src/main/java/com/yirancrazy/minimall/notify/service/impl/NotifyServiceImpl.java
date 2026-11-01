package com.yirancrazy.minimall.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDto;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDto;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDto;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDto;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDto;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 负责站内通知写入、游标查询、已读与删除、广播、营销、违规告警、系统告警和公告发布；写库后按目标触发 SSE 推送，并处理业务唯一键冲突.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotifyManager notifyManager;
    private final NotifyPushGateway notifyPushGateway;

    /**
     * 注入通知数据访问和实时推送依赖.
     *
     * @param notifyManager 站内通知持久化入口
     * @param notifyPushGateway 在线通知的 SSE 推送入口
     */
    public NotifyServiceImpl(NotifyManager notifyManager, NotifyPushGateway notifyPushGateway) {
        this.notifyManager = notifyManager;
        this.notifyPushGateway = notifyPushGateway;
    }

    /**
     * 写入普通用户系统通知；数据库唯一键冲突视为重复消息并返回成功，首次落库成功后推送标题.
     *
     * @param userId 接收通知的用户编号
     * @param title 通知标题，同时作为 SSE 推送内容
     * @param content 落库的通知正文
     * @param bizId 业务幂等键；为空时依赖数据库唯一约束的实际行为
     * @return true 表示新建成功或命中重复键，false 表示保存未成功
     */
    @Override
    public boolean push(Long userId, String title, String content, String bizId) {
        NotifyMessagePo m = new NotifyMessagePo();
        m.setUserId(userId);
        m.setRecipientType(RecipientTypeEnum.USER.intCode());
        m.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
        m.setBizId(bizId);
        m.setTitle(title);
        m.setContent(content);
        m.setReadFlag(0);
        boolean saved;
        try {
            saved = notifyManager.save(m);
        } catch (DuplicateKeyException e) {
            log.info("通知已存在，bizId={}", bizId);
            return true;
        }
        if (saved) {
            notifyPushGateway.send(userId, title);
        }
        return saved;
    }

    /**
     * 查询用户或指定接收方的通知实体，只按 userId 和可选接收方类型过滤，不包含全端公告占位消息.
     *
     * @param dto 包含用户、接收方类型、消息类型和已读状态的查询条件
     * @return 按主键倒序排列的通知实体
     */
    public List<NotifyMessagePo> listByUser(NotifyListDto dto) {
        LambdaQueryWrapper<NotifyMessagePo> wrapper =
                Wrappers.lambdaQuery(NotifyMessagePo.class).eq(NotifyMessagePo::getUserId, dto.getUserId());
        if (dto.getRecipientType() != null) {
            wrapper.eq(NotifyMessagePo::getRecipientType, dto.getRecipientType());
        }
        wrapper.orderByDesc(NotifyMessagePo::getId);
        return notifyManager.list(wrapper);
    }

    /**
     * 将当前用户的站内通知实体转换为展示视图.
     *
     * @param dto 包含用户、接收方类型、消息类型和已读状态的查询条件
     * @return 通知视图列表
     */
    @Override
    public List<NotifyMessageVo> listByUserVo(NotifyListDto dto) {
        return listByUser(dto).stream().map(NotifyMessageVo::from).toList();
    }

    /**
     * 使用主键游标分页查询通知，并把当前接收方的消息与 userId 为 0 的全端公告占位消息合并过滤.
     *
     * @param dto 游标、接收方、消息类型和已读状态筛选条件
     * @return 当前页通知实体及下一页游标
     */
    public CursorPageVo<NotifyMessagePo> page(NotifyListDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<NotifyMessagePo> wrapper = Wrappers.lambdaQuery(NotifyMessagePo.class);
        if (dto.getRecipientType() != null) {
            // recipientType 过滤并入 or 分支：Controller 固定传 USER/MERCHANT/PLATFORM，若独立 AND 会把公告(ALL)挡掉
            wrapper.and(
                    w ->
                            w.eq(NotifyMessagePo::getUserId, dto.getUserId())
                                    .eq(NotifyMessagePo::getRecipientType, dto.getRecipientType())
                                    .or(
                                            q ->
                                                    q.eq(NotifyMessagePo::getUserId, 0L)
                                                            .eq(
                                                                    NotifyMessagePo::getRecipientType,
                                                                    RecipientTypeEnum.ALL.intCode())));
        } else {
            wrapper.and(
                    w ->
                            w.eq(NotifyMessagePo::getUserId, dto.getUserId())
                                    .or(
                                            q ->
                                                    q.eq(NotifyMessagePo::getUserId, 0L)
                                                            .eq(
                                                                    NotifyMessagePo::getRecipientType,
                                                                    RecipientTypeEnum.ALL.intCode())));
        }
        wrapper.lt(lastId != null, NotifyMessagePo::getId, lastId);
        if (dto.getMessageType() != null) {
            wrapper.eq(NotifyMessagePo::getMessageType, dto.getMessageType());
        }
        if (dto.getReadFlag() != null) {
            wrapper.eq(NotifyMessagePo::getReadFlag, dto.getReadFlag());
        }
        wrapper.orderByDesc(NotifyMessagePo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<NotifyMessagePo> records = notifyManager.list(wrapper);
        return CursorPageVo.of(records, limit, NotifyMessagePo::getId);
    }

    /**
     * 将游标分页得到的通知实体转换为展示视图，并转换消息类型和已读状态别名.
     *
     * @param dto 游标、接收方、消息类型和已读状态筛选条件
     * @return 当前页通知视图及下一页游标
     */
    @Override
    public CursorPageVo<NotifyMessageVo> pageVo(NotifyListDto dto) {
        return page(dto).map(NotifyMessageVo::from);
    }

    /**
     * 统计指定接收方和用户下 readFlag 为 0 的通知数量，不包含全端公告占位消息.
     *
     * @param recipientType 接收方类型编码
     * @param userId 接收方编号
     * @return 未读通知数量
     */
    @Override
    public long unreadCount(Integer recipientType, Long userId) {
        return notifyManager.count(
                Wrappers.lambdaQuery(NotifyMessagePo.class)
                        .eq(NotifyMessagePo::getRecipientType, recipientType)
                        .eq(NotifyMessagePo::getUserId, userId)
                        .eq(NotifyMessagePo::getReadFlag, 0));
    }

    /**
     * 校验通知归属后将单条通知标记为已读；已经已读时不写库.
     *
     * @param id 通知主键
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws BizException 通知不存在或归属不匹配时抛出
     */
    public void markRead(Long id, Integer recipientType, Long userId) {
        NotifyMessagePo m = getOwnedMessage(id, recipientType, userId);
        if (Integer.valueOf(1).equals(m.getReadFlag())) {
            return;
        }
        m.setReadFlag(1);
        notifyManager.updateById(m);
    }

    /**
     * 将指定接收方和用户下所有未读通知批量更新为已读.
     *
     * @param recipientType 接收方类型编码
     * @param userId 接收方编号
     */
    public void markAllRead(Integer recipientType, Long userId) {
        notifyManager.update(
                Wrappers.lambdaUpdate(NotifyMessagePo.class)
                        .eq(NotifyMessagePo::getRecipientType, recipientType)
                        .eq(NotifyMessagePo::getUserId, userId)
                        .eq(NotifyMessagePo::getReadFlag, 0)
                        .set(NotifyMessagePo::getReadFlag, 1));
    }

    /**
     * 校验通知归属后按主键软删除单条通知.
     *
     * @param id 通知主键
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws BizException 通知不存在或归属不匹配时抛出
     */
    public void delete(Long id, Integer recipientType, Long userId) {
        getOwnedMessage(id, recipientType, userId);
        notifyManager.removeById(id);
    }

    /**
     * 在事务内校验编号数量、记录完整性和全部归属后批量软删除通知.
     *
     * @param ids 待删除通知主键集合，允许 1 至 100 条
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws BizException 编号为空、超过 100 条、记录缺失或任一记录归属不匹配时抛出
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
        List<NotifyMessagePo> messages = notifyManager.listByIds(ids);
        // 校验全部消息均属于当前用户
        for (NotifyMessagePo m : messages) {
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
     * 校验接收方类型后发送广播；指定 targetId 时写入单条通知并推送 SSE，否则写入 userId 为 0 的全端占位记录.
     *
     * @param dto 广播接收方、消息类型、标题、正文、发送方、幂等键和可选目标编号
     * @throws BizException 接收方类型为空或超出 RecipientTypeEnum 范围时抛出
     */
    public void broadcast(NotifyBroadcastDto dto) {
        if (dto.getRecipientType() == null
                || dto.getRecipientType() < 1
                || dto.getRecipientType() > RecipientTypeEnum.values().length) {
            throw new BizException(NotifyCodeEnum.BROADCAST_RECIPIENT_INVALID);
        }
        NotifyMessagePo m = new NotifyMessagePo();
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
        } else {
            // 全端广播：落库一条 userId=0 的占位记录，按需扩展批量下发
            m.setUserId(0L);
            notifyManager.save(m);
        }
        log.info("广播通知已发送，recipientType={}，targetId={}", dto.getRecipientType(), dto.getTargetId());
    }

    /**
     * 在事务内向指定用户批量写入营销通知并逐人推送 SSE；目标用户为空时写入一条全端营销占位消息.
     *
     * @param dto 营销标题、正文和可选用户编号列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void marketingPush(NotifyMarketingPushDto dto) {
        if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
            NotifyMessagePo m = new NotifyMessagePo();
            m.setUserId(0L);
            m.setRecipientType(RecipientTypeEnum.USER.intCode());
            m.setMessageType(NotifyMessageTypeEnum.PROMOTION.intCode());
            m.setTitle(dto.getTitle());
            m.setContent(dto.getContent());
            m.setReadFlag(0);
            notifyManager.save(m);
        } else {
            List<NotifyMessagePo> messages = new ArrayList<>(dto.getUserIds().size());
            for (Long uid : dto.getUserIds()) {
                NotifyMessagePo m = new NotifyMessagePo();
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
     * 在事务内向指定商家写入违规类型通知并同步推送 SSE.
     *
     * @param dto 商家编号、警告标题、正文和违规类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void violationWarning(NotifyViolationWarningDto dto) {
        NotifyMessagePo m = new NotifyMessagePo();
        m.setUserId(dto.getMerchantId());
        m.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
        m.setMessageType(NotifyMessageTypeEnum.VIOLATION.intCode());
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setReadFlag(0);
        notifyManager.save(m);
        notifyPushGateway.send(dto.getMerchantId(), dto.getTitle());
        log.info("违规警告已发送，merchantId={}，type={}", dto.getMerchantId(), dto.getViolationType());
    }

    /**
     * 在事务内写入面向平台、类型为系统的告警消息，用户编号固定为 0.
     *
     * @param dto 告警标题、正文和级别
     */
    @Transactional(rollbackFor = Exception.class)
    public void systemAlert(NotifySystemAlertDto dto) {
        NotifyMessagePo m = new NotifyMessagePo();
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
     * 使用主键游标倒序查询平台接收方且消息类型为系统的告警记录.
     *
     * @param dto 告警列表游标分页条件
     * @return 当前页系统告警实体及下一页游标
     */
    public CursorPageVo<NotifyMessagePo> alertPage(SystemAlertPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<NotifyMessagePo> wrapper =
                Wrappers.lambdaQuery(NotifyMessagePo.class)
                        .eq(NotifyMessagePo::getRecipientType, RecipientTypeEnum.PLATFORM.intCode())
                        .eq(NotifyMessagePo::getMessageType, NotifyMessageTypeEnum.SYSTEM.intCode())
                        .lt(lastId != null, NotifyMessagePo::getId, lastId)
                        .orderByDesc(NotifyMessagePo::getId)
                        .last("LIMIT " + (limit + 1));
        List<NotifyMessagePo> records = notifyManager.list(wrapper);
        return CursorPageVo.of(records, limit, NotifyMessagePo::getId);
    }

    /**
     * 将平台系统告警查询结果转换为通知展示视图.
     *
     * @param dto 告警列表游标分页条件
     * @return 当前页系统告警视图及下一页游标
     */
    @Override
    public CursorPageVo<NotifyMessageVo> alertPageVo(SystemAlertPageDto dto) {
        return alertPage(dto).map(NotifyMessageVo::from);
    }

    /**
     * 在事务内发布接收方为 ALL、用户编号为 0 的全端公告占位消息.
     *
     * @param dto 公告标题和正文
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(AnnouncementCreateDto dto) {
        NotifyMessagePo m = new NotifyMessagePo();
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
     * 查询通知并校验接收方类型与用户编号均匹配，否则按通知不存在处理.
     *
     * @param id 通知主键
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @return 当前接收方拥有的通知实体
     * @throws BizException 通知不存在或归属不匹配时抛出
     */
    private NotifyMessagePo getOwnedMessage(Long id, Integer recipientType, Long userId) {
        NotifyMessagePo m = notifyManager.getById(id);
        if (m == null || !recipientType.equals(m.getRecipientType()) || !userId.equals(m.getUserId())) {
            throw new BizException(NotifyCodeEnum.NOTIFY_NOT_FOUND);
        }
        return m;
    }
}
