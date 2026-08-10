package com.yirancrazy.minimall.notify.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCategoryEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDTO;
import com.yirancrazy.minimall.notify.entity.PreferencePO;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.service.PreferenceService;
import com.yirancrazy.minimall.notify.vo.PreferenceVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好领域服务实现
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Slf4j
@Service
public class PreferenceServiceImpl implements PreferenceService {

    /** 默认开关值（全开） */
    private static final int DEFAULT_ENABLED = 1;

    private final PreferenceManager preferenceManager;

    public PreferenceServiceImpl(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    /**
     * 查询用户偏好：返回数据库已有的偏好记录 + 缺失类别的默认值，确保返回全量类别。
     * @param userId 用户ID
     * @return 偏好视图列表
     */
    @Override
    public List<PreferenceVO> listByUser(Long userId) {
        LambdaQueryWrapper<PreferencePO> wrapper = Wrappers.lambdaQuery(PreferencePO.class);
        wrapper.eq(PreferencePO::getUserId, userId);
        List<PreferencePO> stored = preferenceManager.list(wrapper);
        Map<String, PreferencePO> map = stored.stream()
                .collect(Collectors.toMap(PreferencePO::getCategoryCode, p -> p));
        List<PreferenceVO> result = new ArrayList<>();
        for (NotifyCategoryEnum cat : NotifyCategoryEnum.values()) {
            PreferencePO po = map.get(cat.getCode());
            if (po == null) {
                result.add(buildDefault(cat.getCode()));
            }
            else {
                result.add(PreferenceVO.from(po));
            }
        }
        return result;
    }

    /**
     * 批量更新偏好：按 (userId, categoryCode) upsert。
     * @param userId 用户ID
     * @param items 更新项列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(Long userId, List<PreferenceUpdateDTO> items) {
        for (PreferenceUpdateDTO item : items) {
            validateCategory(item.getCategoryCode());
            int enabled = item.getEnabled() == null ? 0 : item.getEnabled();
            int normalized = enabled == 0 ? 0 : 1;
            PreferencePO existing = findExisting(userId, item.getCategoryCode());
            if (existing == null) {
                PreferencePO po = new PreferencePO();
                po.setUserId(userId);
                po.setCategoryCode(item.getCategoryCode());
                applyChannel(po, item.getChannel(), normalized);
                preferenceManager.save(po);
            }
            else {
                applyChannel(existing, item.getChannel(), normalized);
                preferenceManager.updateById(existing);
            }
        }
        log.info("preferences batch updated, userId={}, count={}", userId, items.size());
    }

    /**
     * 判断是否启用：未设置偏好时按默认（启用）处理。
     * @param userId 用户ID
     * @param categoryCode 类别编码
     * @param channel 渠道
     * @return true=启用
     */
    @Override
    public boolean isChannelEnabled(Long userId, String categoryCode, String channel) {
        validateCategory(categoryCode);
        if (channel == null
                || (!"SITE".equals(channel) && !"SMS".equals(channel) && !"EMAIL".equals(channel))) {
            throw new BizException(NotifyCodeEnum.PREFERENCE_CHANNEL_INVALID);
        }
        PreferencePO po = findExisting(userId, categoryCode);
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

    private PreferenceVO buildDefault(String categoryCode) {
        PreferenceVO vo = new PreferenceVO();
        vo.setCategoryCode(categoryCode);
        vo.setSiteEnabled(DEFAULT_ENABLED);
        vo.setSmsEnabled(DEFAULT_ENABLED);
        vo.setEmailEnabled(DEFAULT_ENABLED);
        return vo;
    }

    private PreferencePO findExisting(Long userId, String categoryCode) {
        LambdaQueryWrapper<PreferencePO> wrapper = Wrappers.lambdaQuery(PreferencePO.class);
        wrapper.eq(PreferencePO::getUserId, userId);
        wrapper.eq(PreferencePO::getCategoryCode, categoryCode);
        return preferenceManager.getOne(wrapper);
    }

    /**
     * 仅修改指定渠道的开关，其它渠道保留原值；新建记录时其它渠道使用默认值。
     * @param po 偏好记录
     * @param channel 渠道
     * @param enabled 开关值
     */
    private void applyChannel(PreferencePO po, String channel, int enabled) {
        switch (channel) {
            case "SITE" -> po.setSiteEnabled(enabled);
            case "SMS" -> po.setSmsEnabled(enabled);
            case "EMAIL" -> po.setEmailEnabled(enabled);
            default -> throw new BizException(NotifyCodeEnum.PREFERENCE_CHANNEL_INVALID);
        }
        if (po.getSiteEnabled() == null) po.setSiteEnabled(DEFAULT_ENABLED);
        if (po.getSmsEnabled() == null) po.setSmsEnabled(DEFAULT_ENABLED);
        if (po.getEmailEnabled() == null) po.setEmailEnabled(DEFAULT_ENABLED);
    }

    private void validateCategory(String code) {
        boolean valid = Arrays.stream(NotifyCategoryEnum.values())
                .anyMatch(c -> c.getCode().equals(code));
        if (!valid) {
            throw new BizException(NotifyCodeEnum.PREFERENCE_CATEGORY_INVALID);
        }
    }
}
