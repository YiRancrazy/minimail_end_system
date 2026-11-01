package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDto;
import com.yirancrazy.minimall.notify.vo.PreferenceVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义用户通知偏好的读取、批量更新和发送前渠道开关判断能力；未保存的偏好默认启用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PreferenceService {

    /**
     * 返回全部通知类别；已保存类别使用数据库配置，未保存类别补全为三渠道默认启用.
     *
     * @param userId 用户编号
     * @return 按 NotifyCategoryEnum 顺序排列的通知偏好列表
     */
    List<PreferenceVo> listByUser(Long userId);

    /**
     * 批量更新通知偏好；每个类别必须合法，渠道只能是 SITE、SMS 或 EMAIL，非零开关值归一化为启用.
     *
     * @param userId 用户编号
     * @param items 待更新的通知类别和渠道开关
     * @throws com.yirancrazy.minimall.common.exception.BizException 类别或渠道非法时抛出，整批事务回滚
     */
    void updateBatch(Long userId, List<PreferenceUpdateDto> items);

    /**
     * 判断用户在指定通知类别和渠道上是否启用消息；没有持久化配置时默认启用.
     *
     * @param userId 用户编号
     * @param categoryCode NotifyCategoryEnum 中的类别编码
     * @param channel 发送渠道，支持 SITE、SMS、EMAIL
     * @return true 表示允许发送
     * @throws com.yirancrazy.minimall.common.exception.BizException 类别或渠道非法时抛出
     */
    boolean isChannelEnabled(Long userId, String categoryCode, String channel);
}
