package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDto;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDto;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDto;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDto;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDto;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义站内通知写入、查询、已读、删除、广播、营销、违规告警、系统告警和公告发布能力，并统一用户与平台接收方语义.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface NotifyService {

    /**
     * 写入一条普通用户系统通知，不提供业务幂等键.
     *
     * @param userId 接收通知的用户编号
     * @param title 通知标题
     * @param content 通知正文
     * @return true 表示通知成功落库；false 表示数据库写入失败
     */
    default boolean push(Long userId, String title, String content) {
        return push(userId, title, content, null);
    }

    /**
     * 使用业务唯一键写入用户系统通知；唯一键冲突视为重复消息已处理，不再触发 SSE 推送.
     *
     * @param userId 接收通知的用户编号
     * @param title 通知标题
     * @param content 通知正文
     * @param bizId 业务幂等键；为空时由数据库唯一约束决定是否可去重
     * @return true 表示新建成功或命中重复键；false 表示其他保存失败
     */
    boolean push(Long userId, String title, String content, String bizId);

    /**
     * 按游标查询当前接收方可见的站内通知，并额外包含 userId 为 0 的全端公告占位消息.
     *
     * @param dto 用户、接收方类型、消息类型和已读状态等查询条件
     * @return 当前页通知实体及下一页游标
     */
    CursorPageVo<NotifyMessagePo> page(NotifyListDto dto);

    /**
     * 查询当前用户或接收方可见的站内通知并转换为展示视图.
     *
     * @param dto 用户、接收方类型、消息类型和已读状态等查询条件
     * @return 按主键倒序排列的通知视图
     */
    List<NotifyMessageVo> listByUserVo(NotifyListDto dto);

    /**
     * 按游标查询平台接收方且消息类型为系统的告警记录.
     *
     * @param dto 告警列表游标分页条件
     * @return 当前页系统告警实体及下一页游标
     */
    CursorPageVo<NotifyMessagePo> alertPage(SystemAlertPageDto dto);

    /**
     * 按游标查询当前接收方可见通知并转换为展示视图.
     *
     * @param dto 用户、接收方类型、消息类型和已读状态等查询条件
     * @return 当前页通知视图及下一页游标
     */
    CursorPageVo<NotifyMessageVo> pageVo(NotifyListDto dto);

    /**
     * 统计指定接收方和用户未读通知数量；不把全端公告占位消息计入未读数.
     *
     * @param recipientType 接收方类型编码
     * @param userId 接收方编号
     * @return 未读消息数量
     */
    long unreadCount(Integer recipientType, Long userId);

    /**
     * 将当前接收方拥有的单条通知标记为已读；消息已读时直接返回.
     *
     * @param id 通知主键
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws com.yirancrazy.minimall.common.exception.BizException 通知不存在或不属于当前接收方时抛出
     */
    void markRead(Long id, Integer recipientType, Long userId);

    /**
     * 将指定接收方当前全部未读通知批量标记为已读.
     *
     * @param recipientType 接收方类型编码
     * @param userId 接收方编号
     */
    void markAllRead(Integer recipientType, Long userId);

    /**
     * 删除当前接收方拥有的单条通知；归属不匹配时按通知不存在处理.
     *
     * @param id 通知主键
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws com.yirancrazy.minimall.common.exception.BizException 通知不存在或不属于当前接收方时抛出
     */
    void delete(Long id, Integer recipientType, Long userId);

    /**
     * 批量删除当前接收方的通知，要求编号数量为 1 至 100 且全部属于当前接收方.
     *
     * @param ids 待删除通知主键集合
     * @param recipientType 当前接收方类型编码
     * @param userId 当前接收方编号
     * @throws com.yirancrazy.minimall.common.exception.BizException 编号为空、超过上限、记录缺失或归属不匹配时抛出
     */
    void batchDelete(List<Long> ids, Integer recipientType, Long userId);

    /**
     * 广播通知；targetId 存在时定向落库并推送 SSE，否则写入 userId 为 0 的全端占位记录.
     *
     * @param dto 广播接收方、消息类型、标题、正文及定向目标
     * @throws com.yirancrazy.minimall.common.exception.BizException 接收方类型超出枚举范围时抛出
     */
    void broadcast(NotifyBroadcastDto dto);

    /**
     * 向指定用户批量写入营销通知并逐人推送；用户列表为空时只写入全端营销占位消息.
     *
     * @param dto 营销通知标题、正文及可选目标用户列表
     */
    void marketingPush(NotifyMarketingPushDto dto);

    /**
     * 向指定商家写入违规警告通知并实时推送 SSE.
     *
     * @param dto 商家编号、警告标题、正文和违规类型
     */
    void violationWarning(NotifyViolationWarningDto dto);

    /**
     * 写入一条 userId 为 0、接收方为平台、类型为系统的告警消息.
     *
     * @param dto 告警标题、正文和级别
     */
    void systemAlert(NotifySystemAlertDto dto);

    /**
     * 查询平台系统告警并按游标分页转换为展示视图.
     *
     * @param dto 告警列表游标分页条件
     * @return 当前页告警视图及下一页游标
     */
    CursorPageVo<NotifyMessageVo> alertPageVo(SystemAlertPageDto dto);

    /**
     * 发布全端公告占位消息，接收方类型为 ALL、用户编号为 0.
     *
     * @param dto 公告标题和正文
     */
    void publishAnnouncement(AnnouncementCreateDto dto);

    /**
     * 查询当前用户或接收方可见的站内通知实体.
     *
     * @param dto 用户、接收方类型、消息类型和已读状态等查询条件
     * @return 按主键倒序排列的通知实体
     */
    List<NotifyMessagePo> listByUser(NotifyListDto dto);
}
