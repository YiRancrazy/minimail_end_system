package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.user.entity.AddressPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressVO.from() 单元测试，验证字段映射逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public class AddressVOTest {

    /**
     * 验证 from 将所有字段正确映射。
     */
    @Test
    public void from_mapsAllFields() {
        AddressPO po = new AddressPO();
        po.setId(1L);
        po.setUserId(100L);
        po.setReceiverName("张三");
        po.setReceiverPhone("13812345678");
        po.setProvince("浙江省");
        po.setCity("杭州市");
        po.setDistrict("西湖区");
        po.setDetailAddress("文三路1号");
        po.setIsDefault(1);
        po.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));

        AddressVO vo = AddressVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getUserId());
        assertEquals("张三", vo.getReceiverName());
        assertEquals("13812345678", vo.getReceiverPhone());
        assertEquals("浙江省", vo.getProvince());
        assertEquals("杭州市", vo.getCity());
        assertEquals("西湖区", vo.getDistrict());
        assertEquals("文三路1号", vo.getDetailAddress());
        assertEquals(1, vo.getIsDefault());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), vo.getCreateTime());
    }

    /**
     * 验证 from 在字段为 null 时正确处理，不抛 NPE。
     */
    @Test
    public void from_nullFields() {
        AddressPO po = new AddressPO();
        po.setId(1L);
        po.setUserId(null);
        po.setReceiverName(null);
        po.setReceiverPhone(null);
        po.setProvince(null);
        po.setCity(null);
        po.setDistrict(null);
        po.setDetailAddress(null);
        po.setIsDefault(null);
        po.setCreateTime(null);

        AddressVO vo = AddressVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertNull(vo.getUserId());
        assertNull(vo.getReceiverName());
        assertNull(vo.getReceiverPhone());
        assertNull(vo.getProvince());
        assertNull(vo.getCity());
        assertNull(vo.getDistrict());
        assertNull(vo.getDetailAddress());
        assertNull(vo.getIsDefault());
        assertNull(vo.getCreateTime());
    }
}
