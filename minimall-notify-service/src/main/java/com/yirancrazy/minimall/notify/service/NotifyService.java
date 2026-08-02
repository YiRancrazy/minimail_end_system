package com.yirancrazy.minimall.notify.service;

import java.util.List;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知领域服务接口，定义Notify相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface NotifyService {

    /**
     * 构造通知实体并写入数据库，已读标记默认为未读。
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
}
