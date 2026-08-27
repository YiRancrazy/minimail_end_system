package com.yirancrazy.minimall.cart.vo;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemVO 单元测试，覆盖 from 转换方法的字段映射、勾选状态转换与快照空值场景。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
public class CartItemVOTest {

    /**
     * 验证 from 方法正确叠加 SKU 与 SPU 快照字段。
     */
    @Test
    public void from_mergesSkuAndSpu() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(2);
        po.setSelected(1);
        SkuSnapshotDTO sku = new SkuSnapshotDTO(100L, 10L, "S 白色", new BigDecimal("9.90"), 99, 7L);
        SpuSnapshotDTO spu = new SpuSnapshotDTO(10L, "商品标题", "http://img/a.jpg");

        CartItemVO vo = CartItemVO.from(po, sku, spu);

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
     * 验证 selected 为 0 时 isSelected 转换为 false。
     */
    @Test
    public void from_selectedZero_mapsToFalse() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(1);
        po.setSelected(0);

        CartItemVO vo = CartItemVO.from(po, null, null);

        assertNotNull(vo);
        assertFalse(vo.getIsSelected());
        assertNull(vo.getTitle());
        assertNull(vo.getPrice());
        assertNull(vo.getStock());
    }

    /**
     * 验证四参版本传入 MinioUtil 时，主图 objectKey 被解析为可访问的预签名 URL。
     */
    @Test
    public void from_withMinioUtil_resolvesMainImage() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(1);
        po.setSelected(1);
        SpuSnapshotDTO spu = new SpuSnapshotDTO(10L, "标题", "a1b2c3.png");
        MinioUtil minioUtil = mock(MinioUtil.class);
        when(minioUtil.resolvePublicUrl("a1b2c3.png")).thenReturn("http://minio/mall-files/a1b2c3.png?token");

        CartItemVO vo = CartItemVO.from(po, null, spu, minioUtil);

        assertEquals("http://minio/mall-files/a1b2c3.png?token", vo.getMainImage());
    }

    /**
     * 验证 from 方法在 PO 字段为 null 时保留 null 值且勾选态为 false。
     */
    @Test
    public void from_nullFields() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);

        CartItemVO vo = CartItemVO.from(po, null, null);

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
