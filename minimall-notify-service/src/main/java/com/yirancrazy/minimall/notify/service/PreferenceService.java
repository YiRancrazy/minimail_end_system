package com.yirancrazy.minimall.notify.service;

import java.util.List;
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDTO;
import com.yirancrazy.minimall.notify.vo.PreferenceVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好领域服务接口，定义偏好查询、批量更新、默认初始化等业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
public interface PreferenceService {

    /**
     * 查询用户全部通知偏好，缺失类别使用默认值（全开）。
     * @param userId 用户ID
     * @return 偏好视图列表，按类别顺序返回
     */
    List<PreferenceVO> listByUser(Long userId);

    /**
     * 批量更新用户通知偏好，不存在的类别自动创建。
     * @param userId 用户ID
     * @param items 更新项列表
     */
    void updateBatch(Long userId, List<PreferenceUpdateDTO> items);

    /**
     * 判断用户是否启用了指定类别+渠道。
     * 用于通知发送前的开关校验。
     * @param userId 用户ID
     * @param categoryCode 类别编码
     * @param channel 渠道：SITE/SMS/EMAIL
     * @return true 表示已启用
     */
    boolean isChannelEnabled(Long userId, String categoryCode, String channel);
}
