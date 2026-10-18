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
 * @Description: PreferenceServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class PreferenceServiceImpl implements PreferenceService {


    private static final int DEFAULT_ENABLED = 1;

    private final PreferenceManager preferenceManager;

    /**
     * 构造 PreferenceServiceImpl 实例。
     * @param preferenceManager preferenceManager 参数
     */
    public PreferenceServiceImpl(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
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
     * 更新业务数据。
     * @param userId userId 参数
     * @param items items 参数
     */
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
        log.info("通知偏好批量更新完成，userId={}，count={}", userId, items.size());
    }


    /**
     * 执行 isChannelEnabled 方法。
     * @param userId userId 参数
     * @param categoryCode categoryCode 参数
     * @param channel channel 参数
     * @return 是否处理成功
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

    /**
     * 构建或生成Default。
     * @param categoryCode category编码
     * @return 视图对象
     */
    private PreferenceVO buildDefault(String categoryCode) {
        PreferenceVO vo = new PreferenceVO();
        vo.setCategoryCode(categoryCode);
        vo.setSiteEnabled(DEFAULT_ENABLED);
        vo.setSmsEnabled(DEFAULT_ENABLED);
        vo.setEmailEnabled(DEFAULT_ENABLED);
        return vo;
    }

    /**
     * 加载Existing。
     * @param userId 用户 ID
     * @param categoryCode category编码
     * @return 查询结果
     */
    private PreferencePO findExisting(Long userId, String categoryCode) {
        LambdaQueryWrapper<PreferencePO> wrapper = Wrappers.lambdaQuery(PreferencePO.class);
        wrapper.eq(PreferencePO::getUserId, userId);
        wrapper.eq(PreferencePO::getCategoryCode, categoryCode);
        return preferenceManager.getOne(wrapper);
    }


    /**
     * 执行Channel逻辑。
     * @param po 持久化对象
     * @param channel channel
     * @param enabled enabled
     * @throws BizException 业务校验失败时抛出
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

    /**
     * 校验Category。
     * @param code 编码
     * @throws BizException 业务校验失败时抛出
     */
    private void validateCategory(String code) {
        boolean valid = Arrays.stream(NotifyCategoryEnum.values())
                .anyMatch(c -> c.getCode().equals(code));
        if (!valid) {
            throw new BizException(NotifyCodeEnum.PREFERENCE_CATEGORY_INVALID);
        }
    }
}
