package com.yirancrazy.minimall.notify.service;

import java.util.List;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface NotifyService {


    /**
     * 执行 push 方法。
     * @param userId userId 参数
     * @param title title 参数
     * @param content content 参数
     * @return 是否处理成功
     */
    boolean push(Long userId, String title, String content);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<NotifyMessagePO> page(NotifyListDTO dto);
    /**
     * 查询列表数据。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<NotifyMessageVO> listByUserVO(NotifyListDTO dto);



    /**
     * 执行 alertPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<NotifyMessagePO> alertPage(SystemAlertPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<NotifyMessageVO> pageVO(NotifyListDTO dto);


    /**
     * 执行 unreadCount 方法。
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     * @return 业务数值
     */
    long unreadCount(Integer recipientType, Long userId);


    /**
     * 执行 markRead 方法。
     * @param id id 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    void markRead(Long id, Integer recipientType, Long userId);


    /**
     * 执行 markAllRead 方法。
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    void markAllRead(Integer recipientType, Long userId);


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    void delete(Long id, Integer recipientType, Long userId);


    /**
     * 执行 batchDelete 方法。
     * @param ids ids 参数
     * @param recipientType recipientType 参数
     * @param userId userId 参数
     */
    void batchDelete(List<Long> ids, Integer recipientType, Long userId);


    /**
     * 执行 broadcast 方法。
     * @param dto dto 参数
     */
    void broadcast(NotifyBroadcastDTO dto);


    /**
     * 执行 marketingPush 方法。
     * @param dto dto 参数
     */
    void marketingPush(NotifyMarketingPushDTO dto);


    /**
     * 执行 violationWarning 方法。
     * @param dto dto 参数
     */
    void violationWarning(NotifyViolationWarningDTO dto);


    /**
     * 执行 systemAlert 方法。
     * @param dto dto 参数
     */
    void systemAlert(NotifySystemAlertDTO dto);



    /**
     * 执行 alertPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<NotifyMessageVO> alertPageVO(SystemAlertPageDTO dto);


    /**
     * 执行 publishAnnouncement 方法。
     * @param dto dto 参数
     */
    void publishAnnouncement(AnnouncementCreateDTO dto);

    /**
     * 查询列表数据。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<NotifyMessagePO> listByUser(NotifyListDTO dto);
}
