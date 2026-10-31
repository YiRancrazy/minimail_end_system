package com.yirancrazy.minimall.cart.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 CartItemVo 的投影场景，覆盖 SKU/SPU 合并、勾选编码转换、MinIO URL 解析和
 *     空快照字段。当前方法未携带 JUnit @Test 注解，因此只记录预期行为，不会被 JUnit 5 发现执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class CartItemVoTest {

  /**
   * 验证 from 会将持久化购物车数据与完整 SKU、SPU 快照合并.
   *
   * <p>前置条件：购物车记录已勾选，两个快照均包含主键、标题、规格、价格、库存和主图。
   * 预期结果：对应视图字段全部复制，勾选状态转换为 true。
   */
  public void fromMergesSkuAndSpu() {
    CartItemPo po = new CartItemPo();
    po.setId(1L);
    po.setSkuId(100L);
    po.setQuantity(2);
    po.setSelected(1);
    SkuSnapshotDto sku = new SkuSnapshotDto(100L, 10L, "S 白色", new BigDecimal("9.90"), 99, 7L);
    SpuSnapshotDto spu = new SpuSnapshotDto(10L, "商品标题", "http://img/a.jpg");

    CartItemVo vo = CartItemVo.from(po, sku, spu);

    assertNotNull(vo);
    assertEquals(1L, vo.getId());
    assertEquals(100L, vo.getSkuId());
    assertEquals(10L, vo.getSpuId());
    assertEquals("商品标题", vo.getTitle());
    assertEquals("S 白色", vo.getSkuSpec());
    assertEquals(new BigDecimal("9.90"), vo.getPrice());
    assertEquals(2, vo.getQuantity());
    assertTrue(vo.getIsSelected());
    assertEquals(99, vo.getStock());
    assertEquals("http://img/a.jpg", vo.getMainImage());
  }

  /**
   * 验证持久化勾选编码 0 会映射为 false，且快照可以缺失.
   *
   * <p>前置条件：购物车记录 selected=0，SKU 与 SPU 参数均为空。预期结果：视图仍会创建，
   * isSelected 为 false，商品快照字段为 null。
   */
  public void fromSelectedZeroMapsToFalse() {
    CartItemPo po = new CartItemPo();
    po.setId(1L);
    po.setSkuId(100L);
    po.setQuantity(1);
    po.setSelected(0);

    CartItemVo vo = CartItemVo.from(po, null, null);

    assertNotNull(vo);
    assertFalse(vo.getIsSelected());
    assertNull(vo.getTitle());
    assertNull(vo.getPrice());
    assertNull(vo.getStock());
  }

  /**
   * 验证四参数 from 会将商品主图交给 MinioUtil 解析.
   *
   * <p>前置条件：SPU 主图是 MinIO 对象键，MinioUtil 返回带签名的公网 URL。预期结果：视图保存
   * 解析后的 URL，而不是原始对象键。
   */
  public void fromWithMinioUtilResolvesMainImage() {
    CartItemPo po = new CartItemPo();
    po.setId(1L);
    po.setSkuId(100L);
    po.setQuantity(1);
    po.setSelected(1);
    SpuSnapshotDto spu = new SpuSnapshotDto(10L, "标题", "a1b2c3.png");
    MinioUtil minioUtil = mock(MinioUtil.class);
    when(minioUtil.resolvePublicUrl("a1b2c3.png"))
        .thenReturn("http://minio/mall-files/a1b2c3.png?token");

    CartItemVo vo = CartItemVo.from(po, null, spu, minioUtil);

    assertEquals("http://minio/mall-files/a1b2c3.png?token", vo.getMainImage());
  }

  /**
   * 验证 from 可以处理缺失的可选购物车字段和商品快照.
   *
   * <p>前置条件：购物车记录只设置主键，两个快照均为空。预期结果：主键被复制，勾选状态默认
   * false，SKU/SPU 相关字段保持 null。
   */
  public void fromNullFields() {
    CartItemPo po = new CartItemPo();
    po.setId(1L);

    CartItemVo vo = CartItemVo.from(po, null, null);

    assertNotNull(vo);
    assertEquals(1L, vo.getId());
    assertNull(vo.getSkuId());
    assertNull(vo.getQuantity());
    assertFalse(vo.getIsSelected());
    assertNull(vo.getSpuId());
    assertNull(vo.getTitle());
    assertNull(vo.getPrice());
  }
}
