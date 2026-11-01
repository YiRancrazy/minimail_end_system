package com.yirancrazy.minimall.notify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCategoryEnum;
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDto;
import com.yirancrazy.minimall.notify.entity.PreferencePo;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.service.impl.PreferenceServiceImpl;
import com.yirancrazy.minimall.notify.vo.PreferenceVo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证通知偏好的默认值补齐、已有配置合并、批量增改、合法性校验和渠道启用判断.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class PreferenceServiceImplTest {

    private PreferenceManager preferenceManager;
    private PreferenceServiceImpl service;

    /** 初始化偏好表元数据，创建管理器 Mock 并构造被测偏好服务。 */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.PreferenceMapper");
        TableInfoHelper.initTableInfo(assistant, PreferencePo.class);

        preferenceManager = mock(PreferenceManager.class);
        lenient()
                .doAnswer(
                        inv -> {
                            PreferencePo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(preferenceManager)
                .save(any(PreferencePo.class));
        lenient().when(preferenceManager.updateById(any(PreferencePo.class))).thenReturn(true);
        lenient()
                .when(preferenceManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());
        lenient()
                .when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);
        service = new PreferenceServiceImpl(preferenceManager);
    }

    /**
     * 测试目标：验证用户没有保存任何偏好时返回全部类别且三渠道默认启用。
     *
     * 前置：PreferenceManager.list 返回空集合。
     * 预期：返回数量等于 NotifyCategoryEnum 数量，所有渠道开关均为 1。
     */
    public void listReturnsDefaultsWhenEmpty() {
        List<PreferenceVo> list = service.listByUser(1L);

        assertEquals(NotifyCategoryEnum.values().length, list.size());
        for (PreferenceVo vo : list) {
            assertEquals(1, vo.getSiteEnabled());
            assertEquals(1, vo.getSmsEnabled());
            assertEquals(1, vo.getEmailEnabled());
        }
    }

    /**
     * 测试目标：验证已保存类别使用数据库值，未保存类别继续使用默认值。
     *
     * 前置：仅保存 ORDER 类别且 SMS 关闭。
     * 预期：ORDER 的 SMS 为 0；PAYMENT 等未保存类别仍为默认启用。
     */
    public void listMergesStoredAndDefault() {
        PreferencePo orderPref = buildPreference(1L, "ORDER", 1, 0, 1);
        when(preferenceManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.singletonList(orderPref));

        List<PreferenceVo> list = service.listByUser(1L);

        assertEquals(NotifyCategoryEnum.values().length, list.size());
        PreferenceVo orderVo =
                list.stream().filter(v -> "ORDER".equals(v.getCategoryCode())).findFirst().orElseThrow();
        assertEquals(0, orderVo.getSmsEnabled());
        PreferenceVo payVo =
                list.stream().filter(v -> "PAYMENT".equals(v.getCategoryCode())).findFirst().orElseThrow();
        assertEquals(1, payVo.getSiteEnabled());
    }

    /**
     * 测试目标：验证不存在的偏好类别首次更新时新建记录。
     *
     * 前置：getOne 返回 null，更新 ORDER/SMS 为关闭。
     * 预期：调用 save 新建偏好。
     */
    public void updateCreatesNewWhenNotExists() {
        PreferenceUpdateDto dto = new PreferenceUpdateDto();
        dto.setCategoryCode("ORDER");
        dto.setChannel("SMS");
        dto.setEnabled(0);

        service.updateBatch(1L, Collections.singletonList(dto));

        verify(preferenceManager).save(any(PreferencePo.class));
    }

    /**
     * 测试目标：验证已有偏好只修改指定渠道并保留其他渠道值。
     *
     * 前置：ORDER 配置三个渠道均启用，本次关闭 SITE。
     * 预期：SITE 变为 0，SMS 仍为 1，并调用 updateById。
     */
    public void updateModifiesExisting() {
        PreferencePo existing = buildPreference(1L, "ORDER", 1, 1, 1);
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        PreferenceUpdateDto dto = new PreferenceUpdateDto();
        dto.setCategoryCode("ORDER");
        dto.setChannel("SITE");
        dto.setEnabled(0);

        service.updateBatch(1L, Collections.singletonList(dto));

        assertEquals(0, existing.getSiteEnabled());
        assertEquals(1, existing.getSmsEnabled());
        verify(preferenceManager).updateById(existing);
    }

    /**
     * 测试目标：验证非法通知类别在写入前被拒绝。
     *
     * 前置：categoryCode 为 UNKNOWN。
     * 预期：抛出 BizException，且不调用 save。
     */
    public void updateThrowsOnInvalidCategory() {
        PreferenceUpdateDto dto = new PreferenceUpdateDto();
        dto.setCategoryCode("UNKNOWN");
        dto.setChannel("SITE");
        dto.setEnabled(1);

        assertThrows(BizException.class, () -> service.updateBatch(1L, Collections.singletonList(dto)));
        verify(preferenceManager, never()).save(any(PreferencePo.class));
    }

    /**
     * 测试目标：验证非零开关值归一化为启用并允许创建记录。
     *
     * 前置：PAYMENT/EMAIL 的 enabled 为 5。
     * 预期：完成校验并调用 save。
     */
    public void updateNormalizesEnabledValue() {
        PreferenceUpdateDto dto = new PreferenceUpdateDto();
        dto.setCategoryCode("PAYMENT");
        dto.setChannel("EMAIL");
        dto.setEnabled(5);

        service.updateBatch(1L, Collections.singletonList(dto));

        verify(preferenceManager).save(any(PreferencePo.class));
    }

    /**
     * 测试目标：验证没有持久化配置的渠道默认允许发送。
     *
     * 前置：getOne 返回 null。
     * 预期：isChannelEnabled 返回 true。
     */
    public void isEnabledReturnsTrueWhenNotStored() {
        assertTrue(service.isChannelEnabled(1L, "ORDER", "SITE"));
    }

    /**
     * 测试目标：验证渠道启用判断读取并遵守持久化配置。
     *
     * 前置：ORDER 的 SITE 为 0，SMS 为 1。
     * 预期：SITE 返回 false，SMS 返回 true。
     */
    public void isEnabledRespectsStoredValue() {
        PreferencePo existing = buildPreference(1L, "ORDER", 0, 1, 1);
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        assertFalse(service.isChannelEnabled(1L, "ORDER", "SITE"));
        assertTrue(service.isChannelEnabled(1L, "ORDER", "SMS"));
    }

    /**
     * 测试目标：验证未支持的渠道编码被拒绝。
     *
     * 前置：channel 为 PUSH。
     * 预期：抛出 BizException。
     */
    public void isEnabledThrowsOnInvalidChannel() {
        assertThrows(BizException.class, () -> service.isChannelEnabled(1L, "ORDER", "PUSH"));
    }

    /**
     * 测试目标：验证批量更新逐项处理多个不同类别。
     *
     * 前置：两个待更新项均不存在，getOne 返回 null。
     * 预期：save 被调用两次。
     */
    public void updateBatchHandlesMultipleItems() {
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        PreferenceUpdateDto a = new PreferenceUpdateDto();
        a.setCategoryCode("ORDER");
        a.setChannel("SITE");
        a.setEnabled(0);

        PreferenceUpdateDto b = new PreferenceUpdateDto();
        b.setCategoryCode("PAYMENT");
        b.setChannel("EMAIL");
        b.setEnabled(1);

        service.updateBatch(1L, Arrays.asList(a, b));

        verify(preferenceManager, org.mockito.Mockito.times(2)).save(any(PreferencePo.class));
    }

    /** 构造指定用户、类别和三渠道开关值的偏好实体，供测试数据准备复用. */
    private PreferencePo buildPreference(
            Long userId, String categoryCode, Integer site, Integer sms, Integer email) {
        PreferencePo p = new PreferencePo();
        p.setUserId(userId);
        p.setCategoryCode(categoryCode);
        p.setSiteEnabled(site);
        p.setSmsEnabled(sms);
        p.setEmailEnabled(email);
        return p;
    }
}
