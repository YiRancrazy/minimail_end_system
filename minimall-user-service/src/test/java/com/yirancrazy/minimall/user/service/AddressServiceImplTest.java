package com.yirancrazy.minimall.user.service;

import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.entity.AddressPO;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.service.impl.AddressServiceImpl;

/**
 * AddressServiceImpl 单元测试，覆盖收货地址增删改查与默认地址设置的正常、失败、边界路径。
 */
public class AddressServiceImplTest {

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            AddressPO.class);
    }

    private AddressManager addressManager;
    private AddressServiceImpl service;

    @BeforeEach
    void setUp() {
        addressManager = mock(AddressManager.class);
        lenient().doAnswer(inv -> {
            AddressPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(addressManager).save(any(AddressPO.class));
        lenient().when(addressManager.updateById(any(AddressPO.class))).thenReturn(true);
        lenient().when(addressManager.removeById(anyLong())).thenReturn(true);
        lenient().when(addressManager.update(any(Wrapper.class))).thenReturn(true);
        service = new AddressServiceImpl(addressManager);
    }

    /**
     * 验证 create 在地址数量未超限时正常保存并返回 ID。
     */
    @Test
    public void create_persists_and_returns_id() {
        when(addressManager.count(any(Wrapper.class))).thenReturn(0L);

        AddressCreateDTO dto = buildCreateDTO("张三", "13800000001");
        Long id = service.create(1L, dto);

        assertTrue(id != null);
        verify(addressManager).save(any(AddressPO.class));
    }

    /**
     * 验证 create 在地址数量达到 20 时抛出 BizException。
     */
    @Test
    public void create_throws_when_limit_exceeded() {
        when(addressManager.count(any(Wrapper.class))).thenReturn(20L);

        AddressCreateDTO dto = buildCreateDTO("张三", "13800000001");
        assertThrows(BizException.class, () -> service.create(1L, dto));
        verify(addressManager, never()).save(any(AddressPO.class));
    }

    /**
     * 验证 create 在用户无地址时自动设为默认。
     */
    @Test
    public void create_auto_set_default_when_no_addresses() {
        when(addressManager.count(any(Wrapper.class))).thenReturn(0L);
        doAnswer(inv -> {
            AddressPO saved = inv.getArgument(0);
            assertEquals(1, saved.getIsDefault());
            return true;
        }).when(addressManager).save(any(AddressPO.class));

        AddressCreateDTO dto = buildCreateDTO("张三", "13800000001");
        service.create(1L, dto);

        verify(addressManager).save(any(AddressPO.class));
    }

    /**
     * 验证 listByUserId 返回指定用户的地址列表。
     */
    @Test
    public void listByUserId_returns_addresses() {
        AddressPO addr = new AddressPO();
        addr.setId(1L);
        addr.setUserId(1L);
        addr.setReceiverName("张三");
        when(addressManager.list(any(Wrapper.class))).thenReturn(List.of(addr));

        List<AddressPO> result = service.listByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getReceiverName());
    }

    /**
     * 验证 update 在地址存在且属于当前用户时正常更新。
     */
    @Test
    public void update_succeeds_when_owned() {
        AddressPO existing = buildOwnedAddress(1L, 1L);
        when(addressManager.getById(1L)).thenReturn(existing);

        AddressUpdateDTO dto = buildUpdateDTO("李四", "13900000002");
        boolean ok = service.update(1L, 1L, dto);

        assertTrue(ok);
        verify(addressManager).updateById(any(AddressPO.class));
    }

    /**
     * 验证 update 在地址不属于当前用户时抛出 BizException。
     */
    @Test
    public void update_throws_when_not_owned() {
        AddressPO existing = buildOwnedAddress(1L, 2L);
        when(addressManager.getById(1L)).thenReturn(existing);

        AddressUpdateDTO dto = buildUpdateDTO("李四", "13900000002");
        assertThrows(BizException.class, () -> service.update(1L, 1L, dto));
    }

    /**
     * 验证 update 在地址不存在时抛出 BizException。
     */
    @Test
    public void update_throws_when_not_found() {
        when(addressManager.getById(999L)).thenReturn(null);

        AddressUpdateDTO dto = buildUpdateDTO("李四", "13900000002");
        assertThrows(BizException.class, () -> service.update(1L, 999L, dto));
    }

    /**
     * 验证 delete 在地址存在且属于当前用户时正常删除。
     */
    @Test
    public void delete_succeeds_when_owned() {
        AddressPO existing = buildOwnedAddress(1L, 1L);
        when(addressManager.getById(1L)).thenReturn(existing);

        service.delete(1L, 1L);

        verify(addressManager).removeById(1L);
    }

    /**
     * 验证 delete 在地址不属于当前用户时抛出 BizException。
     */
    @Test
    public void delete_throws_when_not_owned() {
        AddressPO existing = buildOwnedAddress(1L, 2L);
        when(addressManager.getById(1L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(1L, 1L));
        verify(addressManager, never()).removeById(anyLong());
    }

    /**
     * 验证 setDefault 在地址存在且属于当前用户时清除旧默认并设置新默认。
     */
    @Test
    public void setDefault_clears_old_and_sets_new() {
        AddressPO existing = buildOwnedAddress(1L, 1L);
        when(addressManager.getById(1L)).thenReturn(existing);

        service.setDefault(1L, 1L);

        verify(addressManager).update(any(Wrapper.class));
        verify(addressManager).updateById(any(AddressPO.class));
    }

    /**
     * 验证 setDefault 在地址不属于当前用户时抛出 BizException。
     */
    @Test
    public void setDefault_throws_when_not_owned() {
        AddressPO existing = buildOwnedAddress(1L, 2L);
        when(addressManager.getById(1L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.setDefault(1L, 1L));
    }

    private AddressCreateDTO buildCreateDTO(String name, String phone) {
        AddressCreateDTO dto = new AddressCreateDTO();
        dto.setReceiverName(name);
        dto.setReceiverPhone(phone);
        dto.setProvince("江苏省");
        dto.setCity("南京市");
        dto.setDistrict("鼓楼区");
        dto.setDetailAddress("中山路100号");
        return dto;
    }

    private AddressUpdateDTO buildUpdateDTO(String name, String phone) {
        AddressUpdateDTO dto = new AddressUpdateDTO();
        dto.setReceiverName(name);
        dto.setReceiverPhone(phone);
        dto.setProvince("江苏省");
        dto.setCity("南京市");
        dto.setDistrict("玄武区");
        dto.setDetailAddress("珠江路200号");
        return dto;
    }

    private AddressPO buildOwnedAddress(Long id, Long userId) {
        AddressPO po = new AddressPO();
        po.setId(id);
        po.setUserId(userId);
        po.setReceiverName("张三");
        po.setReceiverPhone("13800000001");
        po.setProvince("江苏省");
        po.setCity("南京市");
        po.setDistrict("鼓楼区");
        po.setDetailAddress("中山路100号");
        po.setIsDefault(0);
        return po;
    }
}
