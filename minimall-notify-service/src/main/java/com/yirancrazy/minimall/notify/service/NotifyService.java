package com.yirancrazy.minimall.notify.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知领域服务接口，定义站内信相关业务契约
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
public interface NotifyService {

    /**
     * 构造通知实体并写入数据库，已读标记默认为未读（向后兼容，默认 USER + SYSTEM）。
     *
     * @param userId  接收用户主键
     * @param title   通知标题
     * @param content 通知正文
     * @return 入库是否成功
     */
    boolean push(Long userId, String title, String content);

    /**
     * 查询指定用户全部通知消息，按持久化顺序返回列表。
     *
     * @param dto 查询条件
     * @return 通知消息列表
     */
    List<NotifyMessagePO> listByUser(NotifyListDTO dto);

    /**
     * 分页查询站内信，支持按接收方/消息类型/已读状态过滤。
     * @param dto 分页查询入参
     * @return 站内信分页结果
     */
    IPage<NotifyMessagePO> page(NotifyListDTO dto);

    /**
     * 统计指定接收方的未读消息数量。
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @return 未读消息数
     */
    long unreadCount(Integer recipientType, Long userId);

    /**
     * 标记单条消息为已读，校验归属权限。
     * @param id 消息ID
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 消息不存在或无权操作时
     */
    void markRead(Long id, Integer recipientType, Long userId);

    /**
     * 将指定接收方的全部未读消息标记为已读。
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     */
    void markAllRead(Integer recipientType, Long userId);

    /**
     * 删除单条消息（软删除），校验归属权限。
     * @param id 消息ID
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 消息不存在或无权操作时
     */
    void delete(Long id, Integer recipientType, Long userId);

    /**
     * 批量删除消息（软删除），校验所有消息归属当前用户。
     * @param ids 消息ID列表，最多100条
     * @param recipientType 接收方类型
     * @param userId 接收者ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 当ID列表为空、超限或存在非本人消息时
     */
    void batchDelete(List<Long> ids, Integer recipientType, Long userId);

    /**
     * 平台广播消息，落库并通过 SSE 实时推送。
     * @param dto 广播入参
     */
    void broadcast(NotifyBroadcastDTO dto);

    /**
     * 营销推送，向指定用户列表发送营销站内信；userIds 为 null 时全量广播。
     * @param dto 营销推送入参
     */
    void marketingPush(NotifyMarketingPushDTO dto);

    /**
     * 违规警告通知，向指定商家发送违规警告站内信。
     * @param dto 违规警告入参
     */
    void violationWarning(NotifyViolationWarningDTO dto);

    /**
     * 系统告警通知，向平台管理员发送系统异常/故障预警站内信。
     * @param dto 系统告警入参
     */
    void systemAlert(NotifySystemAlertDTO dto);

    /**
     * 平台系统告警分页查询，固定过滤 recipient_type=PLATFORM, message_type=SYSTEM。
     * @param dto 分页入参
     * @return 系统告警分页结果
     */
    IPage<NotifyMessagePO> alertPage(SystemAlertPageDTO dto);
}
