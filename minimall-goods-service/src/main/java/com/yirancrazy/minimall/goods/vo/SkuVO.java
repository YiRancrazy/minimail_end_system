package com.yirancrazy.minimall.goods.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品SKU VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class SkuVO {

    private Long id;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createTime;

    /**
     * 将 SkuPO 转换为 SkuVO。
     * @param po SKU持久化对象
     * @return SKU VO
     */
    public static SkuVO from(SkuPO po) {
        SkuVO vo = new SkuVO();
        vo.setId(po.getId());
        vo.setSpuId(po.getSpuId());
        vo.setSkuName(po.getSkuName());
        vo.setPrice(po.getPrice());
        vo.setStock(po.getStock());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
