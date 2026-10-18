package com.yirancrazy.minimall.notify.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDTO;
import com.yirancrazy.minimall.notify.entity.PreferencePO;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.service.impl.PreferenceServiceImpl;
import com.yirancrazy.minimall.notify.vo.PreferenceVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class PreferenceServiceImplTest {

    private PreferenceManager preferenceManager;
    private PreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.PreferenceMapper");
        TableInfoHelper.initTableInfo(assistant, PreferencePO.class);

        preferenceManager = mock(PreferenceManager.class);
        lenient().doAnswer(inv -> {
            PreferencePO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(preferenceManager).save(any(PreferencePO.class));
        lenient().when(preferenceManager.updateById(any(PreferencePO.class))).thenReturn(true);
        lenient().when(preferenceManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);
        service = new PreferenceServiceImpl(preferenceManager);
    }


    /**
     * 查询列表数据。
     */
    public void list_returns_defaults_when_empty() {
        List<PreferenceVO> list = service.listByUser(1L);

        assertEquals(NotifyCategoryEnum.values().length, list.size());
        for (PreferenceVO vo : list) {
            assertEquals(1, vo.getSiteEnabled());
            assertEquals(1, vo.getSmsEnabled());
            assertEquals(1, vo.getEmailEnabled());
        }
    }


    /**
     * 查询列表数据。
     */
    public void list_merges_stored_and_default() {
        PreferencePO orderPref = buildPreference(1L, "ORDER", 1, 0, 1);
        when(preferenceManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.singletonList(orderPref));

        List<PreferenceVO> list = service.listByUser(1L);

        assertEquals(NotifyCategoryEnum.values().length, list.size());
        PreferenceVO orderVo = list.stream().filter(v -> "ORDER".equals(v.getCategoryCode())).findFirst().orElseThrow();
        assertEquals(0, orderVo.getSmsEnabled());
        PreferenceVO payVo = list.stream().filter(v -> "PAYMENT".equals(v.getCategoryCode())).findFirst().orElseThrow();
        assertEquals(1, payVo.getSiteEnabled());
    }


    /**
     * 更新业务数据。
     */
    public void update_creates_new_when_not_exists() {
        PreferenceUpdateDTO dto = new PreferenceUpdateDTO();
        dto.setCategoryCode("ORDER");
        dto.setChannel("SMS");
        dto.setEnabled(0);

        service.updateBatch(1L, Collections.singletonList(dto));

        verify(preferenceManager).save(any(PreferencePO.class));
    }


    /**
     * 更新业务数据。
     */
    public void update_modifies_existing() {
        PreferencePO existing = buildPreference(1L, "ORDER", 1, 1, 1);
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        PreferenceUpdateDTO dto = new PreferenceUpdateDTO();
        dto.setCategoryCode("ORDER");
        dto.setChannel("SITE");
        dto.setEnabled(0);

        service.updateBatch(1L, Collections.singletonList(dto));

        assertEquals(0, existing.getSiteEnabled());
        assertEquals(1, existing.getSmsEnabled());
        verify(preferenceManager).updateById(existing);
    }


    /**
     * 更新业务数据。
     */
    public void update_throws_on_invalid_category() {
        PreferenceUpdateDTO dto = new PreferenceUpdateDTO();
        dto.setCategoryCode("UNKNOWN");
        dto.setChannel("SITE");
        dto.setEnabled(1);

        assertThrows(BizException.class, () -> service.updateBatch(1L, Collections.singletonList(dto)));
        verify(preferenceManager, never()).save(any(PreferencePO.class));
    }


    /**
     * 更新业务数据。
     */
    public void update_normalizes_enabled_value() {
        PreferenceUpdateDTO dto = new PreferenceUpdateDTO();
        dto.setCategoryCode("PAYMENT");
        dto.setChannel("EMAIL");
        dto.setEnabled(5);

        service.updateBatch(1L, Collections.singletonList(dto));

        verify(preferenceManager).save(any(PreferencePO.class));
    }


    /**
     * 执行 is_enabled_returns_true_when_not_stored 方法。
     */
    public void is_enabled_returns_true_when_not_stored() {
        assertTrue(service.isChannelEnabled(1L, "ORDER", "SITE"));
    }


    /**
     * 执行 is_enabled_respects_stored_value 方法。
     */
    public void is_enabled_respects_stored_value() {
        PreferencePO existing = buildPreference(1L, "ORDER", 0, 1, 1);
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        assertFalse(service.isChannelEnabled(1L, "ORDER", "SITE"));
        assertTrue(service.isChannelEnabled(1L, "ORDER", "SMS"));
    }


    /**
     * 执行 is_enabled_throws_on_invalid_channel 方法。
     */
    public void is_enabled_throws_on_invalid_channel() {
        assertThrows(BizException.class, () -> service.isChannelEnabled(1L, "ORDER", "PUSH"));
    }


    /**
     * 更新业务数据。
     */
    public void update_batch_handles_multiple_items() {
        when(preferenceManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        PreferenceUpdateDTO a = new PreferenceUpdateDTO();
        a.setCategoryCode("ORDER");
        a.setChannel("SITE");
        a.setEnabled(0);

        PreferenceUpdateDTO b = new PreferenceUpdateDTO();
        b.setCategoryCode("PAYMENT");
        b.setChannel("EMAIL");
        b.setEnabled(1);

        service.updateBatch(1L, Arrays.asList(a, b));

        verify(preferenceManager, org.mockito.Mockito.times(2))
                .save(any(PreferencePO.class));
    }

    private PreferencePO buildPreference(Long userId, String categoryCode,
                                         Integer site, Integer sms, Integer email) {
        PreferencePO p = new PreferencePO();
        p.setUserId(userId);
        p.setCategoryCode(categoryCode);
        p.setSiteEnabled(site);
        p.setSmsEnabled(sms);
        p.setEmailEnabled(email);
        return p;
    }
}
