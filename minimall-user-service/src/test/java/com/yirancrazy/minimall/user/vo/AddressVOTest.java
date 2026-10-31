package com.yirancrazy.minimall.user.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.user.entity.AddressPo;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址视图转换单元测试，覆盖完整字段映射和空值默认标记转换.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class AddressVoTest {

  /**
   * 验证完整地址持久化对象会逐字段映射为视图。
   *
   * <p>前置条件：AddressPo 包含有效用户、收件人、手机号、省市区、详细地址、默认标记和创建时间。
   * 关键步骤：调用 AddressVo.from。预期结果：所有视图字段与输入一致，isDefault=1 转为 true。
   */
  public void fromMapsAllFields() {
    AddressPo po = new AddressPo();
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

    AddressVo vo = AddressVo.from(po);

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
   * 验证地址空值字段会保持为空，空默认标记转换为 false。
   *
   * <p>前置条件：除 ID 外的地址字段均为 null。关键步骤：调用 AddressVo.from。预期结果：ID 保留，
   * 其余字段为 null，isDefault=false。
   */
  public void fromNullFields() {
    AddressPo po = new AddressPo();
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

    AddressVo vo = AddressVo.from(po);

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
