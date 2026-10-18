package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.user.entity.AddressPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressVOTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class AddressVOTest {


    /**
     * 执行 from_mapsAllFields 方法。
     */
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
        assertEquals("张三", vo.getReceiver());
        assertEquals("13812345678", vo.getPhone());
        assertEquals("浙江省", vo.getProvince());
        assertEquals("杭州市", vo.getCity());
        assertEquals("西湖区", vo.getDistrict());
        assertEquals("文三路1号", vo.getDetail());
        assertEquals(true, vo.getIsDefault());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), vo.getCreateTime());
    }


    /**
     * 执行 from_nullFields 方法。
     */
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
        assertNull(vo.getReceiver());
        assertNull(vo.getPhone());
        assertNull(vo.getProvince());
        assertNull(vo.getCity());
        assertNull(vo.getDistrict());
        assertNull(vo.getDetail());
        assertEquals(false, vo.getIsDefault());
        assertNull(vo.getCreateTime());
    }
}
