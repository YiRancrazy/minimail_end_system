package com.yirancrazy.minimall.goods.vo;

import com.yirancrazy.minimall.goods.entity.SkuPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 输出商家端 SKU 的规格、价格、库存和创建时间，不暴露持久化审计字段。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SkuVo {

    private Long id;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createTime;

    /**
     * 将 SKU 持久化对象映射为视图对象，不修改源对象。
     *
     * @param po 待转换的持久化对象
     * @return 转换后的 SKU 视图对象
     */
    public static SkuVo from(SkuPo po) {
        SkuVo vo = new SkuVo();
        vo.setId(po.getId());
        vo.setSpuId(po.getSpuId());
        vo.setSkuName(po.getSkuName());
        vo.setPrice(po.getPrice());
        vo.setStock(po.getStock());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
