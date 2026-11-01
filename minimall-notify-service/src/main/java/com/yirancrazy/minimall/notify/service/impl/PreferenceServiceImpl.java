package com.yirancrazy.minimall.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCategoryEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDto;
import com.yirancrazy.minimall.notify.entity.PreferencePo;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.service.PreferenceService;
import com.yirancrazy.minimall.notify.vo.PreferenceVo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 负责用户通知偏好的默认值补齐、批量更新和发送前渠道开关判断；未保存配置默认启用，非法类别或渠道拒绝写入.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class PreferenceServiceImpl implements PreferenceService {

    private static final int DEFAULT_ENABLED = 1;

    private final PreferenceManager preferenceManager;

    /**
     * 注入用户通知偏好的数据访问入口.
     *
     * @param preferenceManager 偏好记录持久化入口
     */
    public PreferenceServiceImpl(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    /**
     * 查询用户已保存偏好，并按全部通知类别补齐缺失项；缺失项三个渠道均默认启用.
     *
     * @param userId 用户编号
     * @return 按枚举顺序排列的偏好视图列表
     */
    @Override
    public List<PreferenceVo> listByUser(Long userId) {
        LambdaQueryWrapper<PreferencePo> wrapper = Wrappers.lambdaQuery(PreferencePo.class);
        wrapper.eq(PreferencePo::getUserId, userId);
        List<PreferencePo> stored = preferenceManager.list(wrapper);
        Map<String, PreferencePo> map =
                stored.stream().collect(Collectors.toMap(PreferencePo::getCategoryCode, p -> p));
        List<PreferenceVo> result = new ArrayList<>();
        for (NotifyCategoryEnum cat : NotifyCategoryEnum.values()) {
            PreferencePo po = map.get(cat.getCode());
            if (po == null) {
                result.add(buildDefault(cat.getCode()));
            } else {
                result.add(PreferenceVo.from(po));
            }
        }
        return result;
    }

    /**
     * 在事务内逐项校验通知类别并更新指定渠道；不存在的类别配置会新建，任意非零开关值归一化为启用.
     *
     * @param userId 用户编号
     * @param items 待更新的类别、渠道和开关值
     * @throws BizException 类别或渠道非法时抛出，事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(Long userId, List<PreferenceUpdateDto> items) {
        for (PreferenceUpdateDto item : items) {
            validateCategory(item.getCategoryCode());
            int enabled = item.getEnabled() == null ? 0 : item.getEnabled();
            int normalized = enabled == 0 ? 0 : 1;
            PreferencePo existing = findExisting(userId, item.getCategoryCode());
            if (existing == null) {
                PreferencePo po = new PreferencePo();
                po.setUserId(userId);
                po.setCategoryCode(item.getCategoryCode());
                applyChannel(po, item.getChannel(), normalized);
                preferenceManager.save(po);
            } else {
                applyChannel(existing, item.getChannel(), normalized);
                preferenceManager.updateById(existing);
            }
        }
        log.info("通知偏好批量更新完成，userId={}，count={}", userId, items.size());
    }

    /**
     * 校验通知类别和渠道并查询用户配置；配置不存在或指定渠道字段为空时按启用处理.
     *
     * @param userId 用户编号
     * @param categoryCode NotifyCategoryEnum 中的类别编码
     * @param channel 发送渠道，支持 SITE、SMS、EMAIL
     * @return true 表示允许发送
     * @throws BizException 类别或渠道非法时抛出
     */
    @Override
    public boolean isChannelEnabled(Long userId, String categoryCode, String channel) {
        validateCategory(categoryCode);
        if (channel == null
                || (!"SITE".equals(channel) && !"SMS".equals(channel) && !"EMAIL".equals(channel))) {
            throw new BizException(NotifyCodeEnum.PREFERENCE_CHANNEL_INVALID);
        }
        PreferencePo po = findExisting(userId, categoryCode);
        if (po == null) {
            return true;
        }
        return switch (channel) {
            case "SITE" -> po.getSiteEnabled() == null || po.getSiteEnabled() == 1;
            case "SMS" -> po.getSmsEnabled() == null || po.getSmsEnabled() == 1;
            case "EMAIL" -> po.getEmailEnabled() == null || po.getEmailEnabled() == 1;
            default -> true;
        };
    }

    /**
     * 构造缺失类别配置的三渠道默认启用视图.
     *
     * @param categoryCode 通知类别编码
     * @return 默认启用的偏好视图
     */
    private PreferenceVo buildDefault(String categoryCode) {
        PreferenceVo vo = new PreferenceVo();
        vo.setCategoryCode(categoryCode);
        vo.setSiteEnabled(DEFAULT_ENABLED);
        vo.setSmsEnabled(DEFAULT_ENABLED);
        vo.setEmailEnabled(DEFAULT_ENABLED);
        return vo;
    }

    /**
     * 按用户和通知类别查询已保存的偏好记录.
     *
     * @param userId 用户编号
     * @param categoryCode 通知类别编码
     * @return 已保存配置，未找到时返回 null
     */
    private PreferencePo findExisting(Long userId, String categoryCode) {
        LambdaQueryWrapper<PreferencePo> wrapper = Wrappers.lambdaQuery(PreferencePo.class);
        wrapper.eq(PreferencePo::getUserId, userId);
        wrapper.eq(PreferencePo::getCategoryCode, categoryCode);
        return preferenceManager.getOne(wrapper);
    }

    /**
     * 将开关值写入指定渠道，并把其余未初始化渠道补齐为默认启用.
     *
     * @param po 待写入的偏好实体
     * @param channel 目标渠道，支持 SITE、SMS、EMAIL
     * @param enabled 已归一化的开关值，0 为关闭，1 为启用
     * @throws BizException 渠道非法时抛出
     */
    private void applyChannel(PreferencePo po, String channel, int enabled) {
        switch (channel) {
            case "SITE" -> po.setSiteEnabled(enabled);
            case "SMS" -> po.setSmsEnabled(enabled);
            case "EMAIL" -> po.setEmailEnabled(enabled);
            default -> throw new BizException(NotifyCodeEnum.PREFERENCE_CHANNEL_INVALID);
        }
        if (po.getSiteEnabled() == null) {
            po.setSiteEnabled(DEFAULT_ENABLED);
        }
        if (po.getSmsEnabled() == null) {
            po.setSmsEnabled(DEFAULT_ENABLED);
        }
        if (po.getEmailEnabled() == null) {
            po.setEmailEnabled(DEFAULT_ENABLED);
        }
    }

    /**
     * 校验类别编码是否属于 NotifyCategoryEnum.
     *
     * @param code 通知类别编码
     * @throws BizException 类别不在枚举范围内时抛出
     */
    private void validateCategory(String code) {
        boolean valid =
                Arrays.stream(NotifyCategoryEnum.values()).anyMatch(c -> c.getCode().equals(code));
        if (!valid) {
            throw new BizException(NotifyCodeEnum.PREFERENCE_CATEGORY_INVALID);
        }
    }
}
